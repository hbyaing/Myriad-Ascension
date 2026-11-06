package cn.blockforge.generated.generatedmod.arts;

import cn.blockforge.generated.generatedmod.ArtCatalog;
import cn.blockforge.generated.generatedmod.CultivationData;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.ProgressionGate;
import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import cn.blockforge.generated.generatedmod.items.HumanEmperorBannerItem;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtVisualPacket;
import cn.blockforge.generated.generatedmod.yujian.YujianController;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 来源剑诀的持续施法；所有费用、目标筛选和生命周期都由服务端裁定。 */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class SustainedSwordArts {
    public static final String WANJIAN = "art_wanjian_guizong";
    public static final String JIUXIAO = "art_jiuxiao_sword_rain";
    private static final String COOLDOWNS = "MyriadSustainedSwordCooldowns";
    private static final int ATTACK_TICKS = 300;
    private static final int MAX_TARGETS = 32;
    private static final Map<UUID, Cast> ACTIVE = new HashMap<>();

    private static final class Cast {
        final String art;
        final int slot;
        final ResourceKey<Level> dimension;
        final Vec3 origin, anchor;
        final long started;
        long nextFuel, nextPulse, nextRain, lastTick=Long.MIN_VALUE;
        int wave;
        final List<RainSword> rain=new ArrayList<>();
        Cast(ServerPlayer player, String art, int slot, Vec3 anchor, long now) {
            this.art = art; this.slot = slot; this.anchor = anchor;
            dimension = player.level().dimension(); origin = player.position(); started = now;
            nextFuel = now + 20; nextPulse = now + windupTicks(art); nextRain=nextPulse;
        }
    }

    private static final class RainSword {
        Vec3 position,velocity=new Vec3(0,-.6,0);
        UUID target; int age;
        RainSword(Vec3 position,UUID target){this.position=position;this.target=target;}
    }

    public static boolean supports(String art) { return WANJIAN.equals(art) || JIUXIAO.equals(art); }
    public static boolean active(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }
    public static String activeArt(ServerPlayer player) {
        Cast cast = ACTIVE.get(player.getUUID()); return cast == null ? "" : cast.art;
    }
    public static int windupTicks(String art) { return JIUXIAO.equals(art) ? 80 : 70; }
    public static float pulseDamage(ServerPlayer player, String art) {
        CultivationData data = GeneratedMod.data(player);
        if (data == null || !supports(art)) return 0;
        return ArtCatalog.damageFor(art,player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                data.wuxingxiantu$getArtMastery(art),data)
                * (JIUXIAO.equals(art) ? .14F : .18F);
    }
    public static String description(String art, int mastery) {
        String effect = JIUXIAO.equals(art)
                ? "定点半径10格剑域，每秒造成单次天阶伤害的14%并减速；每0.25秒落下5把追踪剑"
                : "自身周围6格万剑，每秒造成单次天阶伤害的18%";
        return effect + "；结印" + windupTicks(art)/20.0F + "秒后持续15秒；每秒消耗"
                + ArtCatalog.fixedManaCost(art) + "点灵力+最大灵力" + ArtCatalog.percentageCost(art)
                + "%（含结印）；再次释放或潜行收诀，结印时移动会中断；熟练度最高增伤30%";
    }

    /** 普通功法槽的唯一释放入口；拒绝无学习、锁定槽、跨阶与重复叠加。 */
    public static boolean releaseSlot(ServerPlayer player, int slot) {
        if (!player.isAlive() || player.isSpectator() || slot < 0 || slot >= 8) return false;
        CultivationData data = GeneratedMod.data(player);
        if (data == null) return false;
        String art = data.wuxingxiantu$getEquippedArts()[slot];
        if (!supports(art)) return false;
        Cast previous = ACTIVE.get(player.getUUID());
        // 收诀不收取费用，也不受后续境界/灵力变化阻拦。
        if (previous != null && previous.art.equals(art)) {
            stop(player); tell(player,"message.wuxingxiantu.sustained_art_stopped"); return true;
        }
        if (slot >= CultivationLogic.unlockedArtSlots(data.wuxingxiantu$getRealm())
                || !data.wuxingxiantu$hasLearnedArt(art)) return false;
        if (previous != null) { tell(player,"message.wuxingxiantu.sustained_art_busy"); return false; }
        if (!heldEquipmentAllowed(player)) { tell(player,"message.wuxingxiantu.heart_sword_other_owner"); return false; }
        if (!data.wuxingxiantu$getActiveDivineArt().isBlank() || GeneratedMod.isSoulProjection(player)) return false;
        if (!ProgressionGate.canUseArt(player,art)) {
            ProgressionGate.deny(player,ProgressionGate.MSG_ART_USE,ProgressionGate.requiredRealmForArt(art)); return false;
        }
        long now = player.level().getGameTime();
        if (cooldowns(player).getLong(art) > now || data.wuxingxiantu$getArtCooldown(slot) > now) {
            tell(player,"message.wuxingxiantu.art_cooldown"); return false;
        }
        Vec3 anchor = JIUXIAO.equals(art) ? aim(player) : player.position();
        if (!player.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(anchor))
                || !player.serverLevel().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(anchor))) return false;
        if (!pay(player,data,art)) return false;
        int delay = ArtCatalog.currentCooldownTicks(art,data.wuxingxiantu$getArtMastery(art));
        cooldowns(player).putLong(art,now + delay);
        data.wuxingxiantu$setArtCooldown(slot,now + delay);
        ACTIVE.put(player.getUUID(),new Cast(player,art,slot,anchor,now));
        data.wuxingxiantu$addArtMastery(art,"nine_turns".equals(ProgressionGate.effectiveHeartMethod(player,data)) ? 2 : 1);
        emit(player,art,ArtVisualPacket.BOUNDARY,anchor,anchor,radius(art),0);
        tell(player,"message.wuxingxiantu.sustained_art_started");
        GeneratedMod.sync(player);
        return true;
    }

    private static CompoundTag cooldowns(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG,10)) root.put(Player.PERSISTED_NBT_TAG,new CompoundTag());
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(COOLDOWNS,10)) persisted.put(COOLDOWNS,new CompoundTag());
        return persisted.getCompound(COOLDOWNS);
    }
    private static boolean pay(ServerPlayer player, CultivationData data, String art) {
        float cost = ArtCatalog.finalManaCost(art,CultivationLogic.maxAura(data));
        if (!Float.isFinite(data.wuxingxiantu$getAura()) || data.wuxingxiantu$getAura() < cost) {
            tell(player,"message.wuxingxiantu.low_mana"); return false;
        }
        data.wuxingxiantu$setAura(data.wuxingxiantu$getAura() - cost); return true;
    }
    private static Vec3 aim(ServerPlayer player) {
        Vec3 start = player.getEyePosition(), end = start.add(player.getLookAngle().scale(24));
        HitResult hit = player.serverLevel().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : end;
    }
    private static double radius(String art) { return JIUXIAO.equals(art) ? 10 : 6; }

    /** 与真实 PlayerTick 共用，重复调用同一服务端刻不会重复扣费或命中。 */
    public static void tick(ServerPlayer player) {
        Cast cast = ACTIVE.get(player.getUUID());
        if (cast == null) return;
        CultivationData data = GeneratedMod.data(player);
        long now = player.level().getGameTime(), age = now - cast.started;
        if (data == null || !player.isAlive() || player.isRemoved() || player.isSpectator() || player.isShiftKeyDown()
                || player.level().dimension() != cast.dimension || age < 0 || age >= windupTicks(cast.art) + ATTACK_TICKS
                || !cast.art.equals(data.wuxingxiantu$getEquippedArts()[cast.slot])
                || cast.slot >= CultivationLogic.unlockedArtSlots(data.wuxingxiantu$getRealm())
                || !data.wuxingxiantu$hasLearnedArt(cast.art) || !ProgressionGate.canUseArt(player,cast.art)
                || !heldEquipmentAllowed(player)
                || !data.wuxingxiantu$getActiveDivineArt().isBlank() || GeneratedMod.isSoulProjection(player)
                || (age < windupTicks(cast.art) && player.position().distanceToSqr(cast.origin) > 4)) {
            stop(player); return;
        }
        if(cast.lastTick==now)return;cast.lastTick=now;
        // 先支付本秒供灵，再允许任何伤害；落后的刻不补发连击或集中扣费。
        if (now >= cast.nextFuel) {
            if (!pay(player,data,cast.art)) { stop(player); return; }
            cast.nextFuel = now + 20; GeneratedMod.sync(player);
        }
        if(JIUXIAO.equals(cast.art)&&age>=windupTicks(cast.art))rain(player,cast,now);
        if (now < cast.nextPulse) return;
        cast.nextPulse = now + 20;
        Vec3 center = JIUXIAO.equals(cast.art) ? cast.anchor : player.position();
        double range = radius(cast.art);
        emit(player,cast.art,ArtVisualPacket.AREA,center,center,range,(int)age);
        float damage = pulseDamage(player,cast.art);
        var targets = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(center.x-range,center.y-3,center.z-range,center.x+range,center.y+8,center.z+range),
                target -> allowedTarget(player,target) && horizontalDistanceSqr(target.position(),center) <= range*range);
        targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(center)));
        int count = 0;
        for (LivingEntity target : targets) {
            if (count++ >= MAX_TARGETS) break;
            Vec3 impact = target.position().add(0,target.getBbHeight()*.6D,0);
            emit(player,cast.art,ArtVisualPacket.PROJECTILE,
                    JIUXIAO.equals(cast.art) ? impact.add(0,8,0) : center.add(0,2,0),impact,1,(int)age);
            if (target.hurt(player.damageSources().playerAttack(player),damage) && JIUXIAO.equals(cast.art))
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,30,0,false,true,true));
        }
    }
    /** Source cadence, target combat authority: bounded transient swords, no second entity/account system. */
    private static void rain(ServerPlayer player,Cast cast,long now){
        var level=player.serverLevel();double range=radius(JIUXIAO);
        var choices=level.getEntitiesOfClass(LivingEntity.class,new AABB(cast.anchor.add(-range,-3,-range),cast.anchor.add(range,8,range)),
                target->allowedTarget(player,target)&&horizontalDistanceSqr(target.position(),cast.anchor)<=range*range);
        choices.sort(Comparator.comparingDouble(target->target.distanceToSqr(cast.anchor)));
        if(now>=cast.nextRain){
            cast.nextRain=now+5;
            for(int i=0;i<5&&cast.rain.size()<80;i++){
                double angle=(cast.wave*5+i)*2.399963229728653;
                var spawn=cast.anchor.add(Math.cos(angle)*range*.6,8+(i%3)*.5,Math.sin(angle)*range*.6);
                var target=choices.isEmpty()?null:choices.get((cast.wave+i)%choices.size()).getUUID();
                cast.rain.add(new RainSword(spawn,target));
            }cast.wave++;
        }
        var iterator=cast.rain.iterator();
        while(iterator.hasNext()){
            var sword=iterator.next();if(++sword.age>60){iterator.remove();continue;}
            LivingEntity victim=sword.target!=null&&level.getEntity(sword.target) instanceof LivingEntity living?living:null;
            if(victim!=null&&(!allowedTarget(player,victim)||horizontalDistanceSqr(victim.position(),cast.anchor)>range*range
                    ||victim.getY()<cast.anchor.y-3||victim.getY()>cast.anchor.y+8))victim=null;
            if(victim==null&&sword.age%5==0&&!choices.isEmpty()){victim=choices.get((cast.wave+sword.age)%choices.size());sword.target=victim.getUUID();}
            var desired=victim==null?new Vec3(0,-1,0):victim.getBoundingBox().getCenter().subtract(sword.position).normalize();
            sword.velocity=sword.velocity.scale(.3).add(desired.scale(.7)).normalize().scale(.95);
            var end=sword.position.add(sword.velocity);
            var block=level.clip(new ClipContext(sword.position,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            var stop=block.getType()==HitResult.Type.MISS?end:block.getLocation();
            boolean hit=victim!=null&&(victim.getBoundingBox().inflate(.2).contains(sword.position)
                    ||victim.getBoundingBox().inflate(.2).clip(sword.position,stop).isPresent());
            if(sword.age==1||sword.age%5==0||hit)emit(player,JIUXIAO,ArtVisualPacket.PROJECTILE,sword.position,stop,1,sword.age);
            if(hit){victim.hurt(player.damageSources().playerAttack(player),pulseDamage(player,JIUXIAO));iterator.remove();}
            else if(block.getType()!=HitResult.Type.MISS)iterator.remove();else sword.position=end;
        }
    }
    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double x=a.x-b.x,z=a.z-b.z; return x*x+z*z;
    }
    private static boolean heldEquipmentAllowed(ServerPlayer player) {
        return cn.blockforge.generated.generatedmod.crafting.EquipmentBond.canUse(player,player.getMainHandItem())
                && cn.blockforge.generated.generatedmod.crafting.EquipmentBond.canUse(player,player.getOffhandItem());
    }
    /** 共用五行御剑的 LOS/友军过滤，再保护所有已认主生物与人皇幡随从。 */
    public static boolean allowedTarget(ServerPlayer player, LivingEntity target) {
        if (!YujianController.canTarget(player,target) || target.isSpectator() || HumanEmperorBannerItem.isServant(target)
                || (target instanceof OwnableEntity owned && owned.getOwnerUUID() != null)) return false;
        if (target instanceof Player other)
            return !other.isCreative() && player.getServer() != null && player.getServer().isPvpAllowed();
        return target instanceof Enemy || (target instanceof Mob mob && mob.getTarget() == player);
    }
    private static void emit(ServerPlayer player,String art,int mode,Vec3 from,Vec3 to,double size,int phase) {
        // 复用现有剑形轨迹和阵域绘制入口，不增加客户端着色器或独立粒子实体。
        ArtParticleLibrary.emit(player.serverLevel(),JIUXIAO.equals(art) ? "art_sword_array" : "art_myriad_swords",
                mode,from,to,size,phase);
    }
    private static void tell(ServerPlayer player,String key) { player.displayClientMessage(Component.translatable(key),true); }
    public static void stop(ServerPlayer player) { ACTIVE.remove(player.getUUID()); }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) tick(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (event.getEntity() instanceof ServerPlayer player && old.contains(COOLDOWNS,10)) {
            CompoundTag root = player.getPersistentData();
            if (!root.contains(Player.PERSISTED_NBT_TAG,10)) root.put(Player.PERSISTED_NBT_TAG,new CompoundTag());
            root.getCompound(Player.PERSISTED_NBT_TAG).put(COOLDOWNS,old.getCompound(COOLDOWNS).copy());
        }
    }
    @SubscribeEvent public static void serverStopped(ServerStoppedEvent event) { ACTIVE.clear(); }
    private SustainedSwordArts() {}
}
