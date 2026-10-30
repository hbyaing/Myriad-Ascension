package cn.blockforge.generated.generatedmod.yujian;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import cn.blockforge.generated.generatedmod.arts.ArtParticleLibrary;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtVisualPacket;
import cn.blockforge.generated.generatedmod.robe.YujianSlot;
import cn.blockforge.generated.generatedmod.robe.RobeSlots;
import cn.blockforge.generated.generatedmod.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.MoverType;
import net.minecraft.util.Mth;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Adapted YujianCraft formation/flight/technique behavior for the cultivation mana system.
 * Original: Copyright (c) 2026 forThousands, MIT; see META-INF/licenses/YujianCraft-MIT.txt. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID)
public final class YujianController {
    public static final String FLIGHT="art_sword_flight";
    private static final String BIND="WuxingYujianBinding";
    private static final Map<UUID,Ride> RIDES=new HashMap<>();
    private static final class Ride {
        final UUID entity;
        UUID sword;
        final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        FlyingSwordState state = FlyingSwordState.SUMMONING;
        int stateTicks;
        boolean summonHeld;
        boolean forward, brake, up, down, rollLeft, rollRight, sprint, recenter;
        float rollSensitivity = 1.0F;
        ManualRoll.ReleaseMode releaseMode = ManualRoll.ReleaseMode.HOLD_ANGLE;
        float desiredYaw, desiredPitch;
        final ManualRoll manualRoll=new ManualRoll();
        float bank;
        Vec3 mountStart;
        int collisionTicks;
        double nurtureDistance;
        int gameMode;
        long lastInputTick;
        final Map<UUID, Integer> hitCooldown = new HashMap<>();
        YujianPhase phase=YujianPhase.BASIC;
        int phaseTicks,dunxuStageTicks,extremeTicks,dunxuHoldTicks;
        int dunxuStage; // 0 none, 1 pre-phase, 2 phasing, 3 exiting
        boolean extremeEmitted;
        final JinghongBurst jinghong=new JinghongBurst();
        final HuahongFlight huahong=new HuahongFlight();
        double cruiseTarget;
        int rollPulseTicks, rollPulseCooldown;
        boolean wasRolling;
        final Deque<Vec3> safePositions=new ArrayDeque<>();
        Ride(UUID entity, UUID sword, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
            this.entity = entity; this.sword = sword; this.dimension = dimension;
        }
    }
    private YujianController(){}
    public static UUID binding(ItemStack stack){var tag=stack.getOrCreateTag();if(!tag.hasUUID(BIND))tag.putUUID(BIND,UUID.randomUUID());return tag.getUUID(BIND);}
    public static ItemStack find(ServerPlayer player,UUID id){
        if(id==null)return ItemStack.EMPTY;
        var equipped=YujianSlot.activeFlyingSword(player);if(matchesBinding(equipped,id))return equipped;
        for(int i=0;i<player.getInventory().getContainerSize();i++){var s=player.getInventory().getItem(i);if(matchesBinding(s,id))return s;}
        return ItemStack.EMPTY;
    }
    private static boolean matchesBinding(ItemStack stack,UUID id){return !stack.isEmpty()&&stack.hasTag()&&stack.getTag().hasUUID(BIND)&&id.equals(stack.getTag().getUUID(BIND));}
    public static boolean riding(ServerPlayer player){return RIDES.containsKey(player.getUUID());}
    public static List<ItemStack> implementsFor(ServerPlayer p,String art){
        if(FLIGHT.equals(art)){var candidate=YujianSlot.candidate(p);return YujianSlot.accepts(candidate)?List.of(candidate):List.of();}
        var result=new ArrayList<ItemStack>();
        // 御剑与剑诀只认本模组灵剑；普通铁剑、钻石剑等原版 SwordItem 一律不能触发御剑。
        for(int i=-1;i<p.getInventory().getContainerSize()&&result.size()<6;i++){
            ItemStack stack=i==-1?p.getMainHandItem():p.getInventory().getItem(i);
            boolean valid=ProgressionGate.isSpiritSword(stack);
            if(valid&&!result.contains(stack))result.add(stack);
        }return result;
    }
    public static boolean canCast(ServerPlayer p,String art){
        if(art.equals(FLIGHT)&&riding(p))return true;
        if(implementsFor(p,art).isEmpty()||p.isSpectator()||!p.isAlive())return false;
        // 御剑必须真正学习《御剑凌霄诀》、持合格灵剑并满足境界门槛，不再绕过秘籍学习条件。
        if(art.equals(FLIGHT)){
            if(p.isPassenger()||p.isSleeping())return false;
            var data=GeneratedMod.data(p);
            if(data==null||!data.wuxingxiantu$hasLearnedArt(FLIGHT))return false;
            if(!data.wuxingxiantu$getActiveDivineArt().isBlank())return false;
            if(data.wuxingxiantu$getAura()<ArtCatalog.finalManaCost(FLIGHT,CultivationLogic.maxAura(data))
                    ||p.getPersistentData().getLong("SwordFlightCooldown")>p.level().getGameTime())return false;
            var items=implementsFor(p,art);
            if(items.isEmpty())return false;
            return ProgressionGate.canYujian(p,data,items.get(0));
        }
        // 主动剑诀也要能驾驭当前灵剑并满足功法境界门槛。
        var data=GeneratedMod.data(p);
        if(data==null)return false;
        if(!ProgressionGate.canUseSwordTechnique(p,art,SwordNurture.current(p)))return false;
        return SwordTechniqueManager.isSwordTechnique(art);
    }
    /** 御剑失败时给玩家的具体原因（供服务端提示）。返回本地化 key 表示被拒；null 表示允许。 */
    public static String flightDenialReason(ServerPlayer p){
        var data=GeneratedMod.data(p);
        if(data==null||!data.wuxingxiantu$hasLearnedArt(FLIGHT))return ProgressionGate.MSG_YUJIAN_ART;
        var items=implementsFor(p,FLIGHT);
        if(items.isEmpty())return "message.wuxingxiantu.yujian_ineligible_sword";
        if(!ProgressionGate.canYujian(p,data,items.get(0)))return ProgressionGate.MSG_YUJIAN_REALM;
        if(data.wuxingxiantu$getAura()<ArtCatalog.finalManaCost(FLIGHT,CultivationLogic.maxAura(data)))return "message.wuxingxiantu.low_mana";
        if(p.getPersistentData().getLong("SwordFlightCooldown")>p.level().getGameTime())return "message.wuxingxiantu.art_cooldown";
        return null;
    }
    public static BlockHitResult aim(ServerPlayer p,boolean water){return p.serverLevel().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(24)),ClipContext.Block.COLLIDER,water?ClipContext.Fluid.SOURCE_ONLY:ClipContext.Fluid.NONE,p));}
    public static void cast(ServerPlayer p,String art,int mastery){
        if(SwordTechniqueRegistry.isTechnique(art)) {
            SwordNurture.cast(p,SwordNurture.current(p),art);
            return;
        }
        if(!canCast(p,art))return;
        if(art.equals(FLIGHT)){
            if(riding(p)) stop(p);
            else { beginSummon(p); Ride ride=RIDES.get(p.getUUID()); if(ride!=null) ride.summonHeld=false; }
            return;
        }
    }
    static boolean executeTechnique(ServerPlayer p,String art,int mastery){
        if(!SwordNurture.canCast(p,SwordNurture.current(p),art))return false;
        List<ItemStack> items;
        int shadowLimit = 6;
        if (SwordTechniqueRegistry.isTechnique(art)) {
            ItemStack selected = SwordNurture.current(p);
            if (!(selected.getItem() instanceof cn.blockforge.generated.generatedmod.items.SpiritSwordItem spirit)) return false;
            var nurture = cn.blockforge.generated.generatedmod.items.SpiritSwordItem.nurture(selected);
            var technique = SwordTechniqueRegistry.get(art);
            if (!nurture.equippedTechniques.contains(art)) return false;
            shadowLimit = Math.min(technique.maxShadows(), Math.max(1, spirit.profile().tier() + nurture.nurtureStage / 2));
            items = List.of(selected);
        } else items = implementsFor(p,art);
        var existing=p.serverLevel().getEntitiesOfClass(ControlledSwordEntity.class,p.getBoundingBox().inflate(80),s->p.getUUID().equals(s.ownerId)&&!s.support());
        var hit=aim(p,false);Vec3 point=hit.getLocation();
        LivingEntity victim=p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(p.getEyePosition(),point).inflate(1),e->canTarget(p,e))
                .stream().filter(e->e.getBoundingBox().inflate(0.35).clip(p.getEyePosition(),hit.getLocation()).isPresent())
                .min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if(victim!=null)point=victim.position().add(0,victim.getBbHeight()*0.5,0);
        boolean spawned=false;
        for(int i=0;i<Math.min(shadowLimit,items.size()*shadowLimit);i++){
            ItemStack source=items.get(i % items.size());
            var sword=ModEntities.CONTROLLED_SWORD.get().create(p.level());if(sword==null)continue;
            sword.bind(p,source,i,false);sword.setPos(FormationGeometry.dockPosition(p,i,FormationMode.FAN_ALIGNED,0));
            sword.order(art,point,victim==null?null:victim.getUUID(),hit.getBlockPos(),mastery);
            spawned|=p.serverLevel().addFreshEntity(sword);
        }
        if(spawned)existing.forEach(Entity::discard);
        return spawned;
    }
    public static boolean canTarget(ServerPlayer p,LivingEntity e){return e!=p&&e.isAlive()&&!e.isAlliedTo(p)&&!(e instanceof net.minecraft.world.entity.TamableAnimal pet&&p.getUUID().equals(pet.getOwnerUUID()))&&(!(e instanceof Player other)||p.canHarmPlayer(other))&&p.hasLineOfSight(e);}
    private static void beginSummon(ServerPlayer p){
        if(!canCast(p,FLIGHT))return;
        var sword=ModEntities.CONTROLLED_SWORD.get().create(p.level());if(sword==null)return;
        ItemStack source=YujianSlot.equipFromMainHand(p);if(source.isEmpty())return;
        sword.bind(p,source,0,true);sword.flightRoot(true);sword.setPos(p.position().add(0,1.1,0));
        if(!p.serverLevel().addFreshEntity(sword))return;
        ArtParticleLibrary.emit(p.serverLevel(), FLIGHT, ArtVisualPacket.AURA,
                p.position().add(0, 1.0, 0), p.position(), 1.6D, 0);
        var data=GeneratedMod.data(p);
        // All summon inputs share the Art startup cost; phase upkeep is charged in tickAdvanced.
        data.wuxingxiantu$setAura(data.wuxingxiantu$getAura()-ArtCatalog.finalManaCost(FLIGHT,CultivationLogic.maxAura(data)));
        p.getPersistentData().putLong("SwordFlightCooldown",p.level().getGameTime()
                +ArtCatalog.currentCooldownTicks(FLIGHT,data.wuxingxiantu$getArtMastery(FLIGHT)));
        data.wuxingxiantu$addArtMastery(FLIGHT,"nine_turns".equals(ProgressionGate.effectiveHeartMethod(p,data))?2:1);
        GeneratedMod.sync(p);
        Ride ride=new Ride(sword.getUUID(),binding(source),p.level().dimension());
        ride.state=FlyingSwordState.SUMMONING; ride.summonHeld=true;
        ride.desiredYaw=p.getYRot(); ride.desiredPitch=p.getXRot();
        ride.lastInputTick=p.tickCount;
        ride.gameMode=p.gameMode.getGameModeForPlayer().getId();
        RIDES.put(p.getUUID(),ride);
        RobeSlots.sync(p);
        FlightFeedbackEvent.emit(sword,FlightFeedbackEvent.Kind.SUMMON);
    }
    public static void stop(ServerPlayer p){
        Ride ride=RIDES.remove(p.getUUID());
        if(ride!=null){
            var level=p.server.getLevel(ride.dimension);
            if(level!=null){var entity=level.getEntity(ride.entity);
                // A mounted root may already have crossed into an inaccessible chunk.
                // Its validated vehicle reference still has to be detached and removed.
                if(!(entity instanceof ControlledSwordEntity)&&p.getVehicle() instanceof ControlledSwordEntity vehicle
                        &&ride.entity.equals(vehicle.getUUID()))entity=vehicle;
                if(entity instanceof ControlledSwordEntity sword){
                if(p.isAlive() && p.level()==level && ride.phase==YujianPhase.DUNXU)restoreSafePosition(sword,p,ride);
                sword.huahong(0,0);ride.huahong.reset();
                sword.noPhysics=false;sword.advancedPhase(YujianPhase.BASIC);sword.dunxuStage(0);
                if(p.getVehicle()==sword)p.stopRiding();
                p.setDeltaMovement(Vec3.ZERO);
                if(p.isAlive()&&!p.onGround())p.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.SLOW_FALLING,100,0,false,false));
                FlightFeedbackEvent.emit(sword,FlightFeedbackEvent.Kind.RECALL);sword.discard();}}
            Network.sendSwordFlightState(p,false);
            RobeSlots.sync(p);
        }
        emergencyReturn(p);
        p.fallDistance=0;
        // One-time migration of a world saved while the legacy creative-flight wrapper was active.
        if(p.getPersistentData().contains("WuxingYujianFlight")){
            var saved=p.getPersistentData().getCompound("WuxingYujianFlight");
            if(saved.getInt("mode")==p.gameMode.getGameModeForPlayer().getId()){
                var abilities=p.getAbilities();abilities.mayfly=saved.getBoolean("mayfly");
                abilities.flying=saved.getBoolean("flying")&&abilities.mayfly;
                abilities.setFlyingSpeed(Mth.clamp(saved.getFloat("speed"),0.01F,0.2F));
                p.onUpdateAbilities();
            }
            p.getPersistentData().remove("WuxingYujianFlight");
        }
    }
    private static void emergencyReturn(ServerPlayer player){
        if(!player.isAlive()||!player.getPersistentData().getBoolean("YujianEmergencyReturn"))return;
        var level=player.server.overworld();var spawn=level.getSharedSpawnPos();
        for(int y=level.getMaxBuildHeight()-4;y>level.getMinBuildHeight();y--){
            Vec3 point=new Vec3(spawn.getX()+.5,y,spawn.getZ()+.5);
            AABB box=new AABB(point.x-.4,point.y,point.z-.4,point.x+.4,point.y+2,point.z+.4);
            if(level.hasChunk(spawn.getX()>>4,spawn.getZ()>>4)&&level.getWorldBorder().isWithinBounds(box)
                    &&level.noCollision(player,box)&&!level.containsAnyLiquid(box)){
                player.stopRiding();player.teleportTo(level,point.x,point.y,point.z,player.getYRot(),0);
                player.getPersistentData().remove("YujianEmergencyReturn");return;
            }
        }
        player.connection.disconnect(net.minecraft.network.chat.Component.translatable("message.wuxingxiantu.yujian_no_safe_exit"));
    }
    public static void handleInput(ServerPlayer player, boolean summonHeld, boolean togglePressed,
                                   boolean forward, boolean brake, boolean up, boolean down,
                                   boolean rollLeft, boolean rollRight, boolean sprint, boolean recenter,
                                   float rollSensitivity, int releaseMode,
                                   float yaw, float pitch) {
        Ride ride=RIDES.get(player.getUUID());
        if (ride==null) {
            if (summonHeld && togglePressed) {
                if(canCast(player,FLIGHT))beginSummon(player);
                else {
                    // 统一失败反馈：区分“未学御剑凌霄诀”“无合格灵剑”“境界不足”。
                    String reason=flightDenialReason(player);
                    if(reason!=null){
                        if(reason.equals(ProgressionGate.MSG_YUJIAN_REALM)){
                            var items=implementsFor(player,FLIGHT);
                            ProgressionGate.deny(player,reason,ProgressionGate.yujianRequiredRealm(
                                    GeneratedMod.data(player),items.isEmpty()?net.minecraft.world.item.ItemStack.EMPTY:items.get(0)));
                        } else {
                            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(reason),true);
                        }
                    }
                }
            }
            return;
        }
        ride.lastInputTick=player.tickCount;
        ride.summonHeld=summonHeld;
        ride.forward=forward; ride.brake=brake; ride.up=up; ride.down=down;
        ride.rollLeft=rollLeft; ride.rollRight=rollRight; ride.sprint=sprint; ride.recenter|=recenter;
        ride.rollSensitivity=Mth.clamp(Float.isFinite(rollSensitivity) ? rollSensitivity : 1.0F, 0.35F, 2.0F);
        ride.releaseMode=releaseMode == 1 ? ManualRoll.ReleaseMode.RECENTER : ManualRoll.ReleaseMode.HOLD_ANGLE;
        if (Float.isFinite(yaw)) ride.desiredYaw=Mth.wrapDegrees(yaw);
        if (Float.isFinite(pitch)) ride.desiredPitch=Mth.clamp(pitch,-75.0F,75.0F);
        if (togglePressed && (ride.state==FlyingSwordState.FLYING || ride.state==FlyingSwordState.MOUNT_TRANSITION)) {
            ride.state=FlyingSwordState.DISMOUNT_TRANSITION; ride.stateTicks=0;
        }
        if (!summonHeld && (ride.state==FlyingSwordState.SUMMONING || ride.state==FlyingSwordState.ORBITING)) {
            ride.state=FlyingSwordState.MOUNT_TRANSITION; ride.stateTicks=0;
        }
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        Ride ride=RIDES.get(p.getUUID());if(ride==null)return;
        var stack=YujianSlot.activeFlyingSword(p);
        if(!validEquippedSword(p,ride)){stop(p);return;}
        if(!p.isAlive()||p.isSpectator()||p.isSleeping()||p.gameMode.getGameModeForPlayer().getId()!=ride.gameMode||p.level().dimension()!=ride.dimension||stack.isEmpty()||!ProgressionGate.isSpiritSword(stack)||p.serverLevel().getEntity(ride.entity)==null){stop(p);return;}
        // 驾驭资格每秒复核：创造切生存、境界变化等导致不再满足御剑条件时立即收剑。
        if(!ProgressionGate.canYujian(p,GeneratedMod.data(p),stack)){stop(p);return;}
        // Keep input alive across small packet gaps without letting a stale client fly forever.
        if (p.tickCount - ride.lastInputTick > FlyingSwordConfig.INPUT_TIMEOUT_TICKS) {
            ride.forward=false; ride.brake=true; ride.up=false; ride.down=false;
            ride.rollLeft=false; ride.rollRight=false; ride.sprint=false;
            ride.dunxuStage=0;ride.dunxuStageTicks=0;
            if(ride.state!=FlyingSwordState.FLYING){stop(p);return;}
        }
        if(p.getVehicle() instanceof ControlledSwordEntity)p.fallDistance=0;
        if(p.tickCount%20==0)RobeSlots.syncIfChanged(p);
    }
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public static void serverStopped(net.minecraftforge.event.server.ServerStoppedEvent e){RIDES.clear();}

    /** Server-authoritative movement for the single flight-root entity. */
    public static void tickFlightEntity(ControlledSwordEntity sword, ServerPlayer player) {
        Ride ride=RIDES.get(player.getUUID());
        if (ride==null || !ride.entity.equals(sword.getUUID())) { sword.discard(); return; }
        if(!validEquippedSword(player,ride)){stop(player);return;}
        ItemStack source=YujianSlot.activeFlyingSword(player);
        // Replacing the valid sword in the equipment slot keeps the ride alive and
        // swaps the visual/flight definition on the next server tick.  An empty or
        // invalid slot is still rejected by validEquippedSword and stops flight.
        if(!ItemStack.matches(sword.display(),source))sword.syncFlightDisplay(source);
        if (!ProgressionGate.canYujian(player,GeneratedMod.data(player),source)) { stop(player); return; }
        FlyingSwordDefinition definition=FlyingSwordRegistry.get(source);
        if(definition==null){stop(player);return;}
        ride.stateTicks++;
        sword.flightState(ride.state);
        sword.dunxuStage(ride.phase==YujianPhase.DUNXU?ride.dunxuStage:0);
        if(ride.state!=FlyingSwordState.FLYING){ride.jinghong.cancelEffect();sword.jinghongStrength(0);ride.huahong.reset();sword.huahong(0,0);}
        if(ride.state==FlyingSwordState.FLYING)sword.visualScale(definition.flightScale());
        if(ride.state!=FlyingSwordState.FLYING && ride.phase!=YujianPhase.BASIC)changePhase(sword,player,ride,YujianPhase.BASIC);
        switch (ride.state) {
            case SUMMONING, ORBITING -> tickOrbit(sword, player, ride, definition);
            case MOUNT_TRANSITION -> tickMountTransition(sword, player, ride, definition);
            case FLYING -> tickFlight(sword, player, ride, definition, source);
            case DISMOUNT_TRANSITION -> tickDismount(sword, player, ride);
            case INACTIVE -> stop(player);
        }
    }

    private static void tickOrbit(ControlledSwordEntity sword, ServerPlayer player, Ride ride, FlyingSwordDefinition definition) {
        if (!ride.summonHeld) { ride.state=FlyingSwordState.MOUNT_TRANSITION; ride.stateTicks=0; return; }
        if (ride.state==FlyingSwordState.SUMMONING && ride.stateTicks>6) {ride.state=FlyingSwordState.ORBITING;FlightFeedbackEvent.emit(sword,FlightFeedbackEvent.Kind.ORBIT);}
        double t=(player.tickCount+ride.stateTicks*0.15D)*0.24D;
        double progress=Math.min(1.0D,ride.stateTicks/(double)FlyingSwordConfig.MOUNT_TRANSITION_TICKS);
        double radius=(1.7D+0.45D*Math.sin(t*0.7D))*definition.orbitScale()*progress;
        Vec3 center=player.position().add(0,1.0D,0);
        Vec3 desired=center.add(Math.cos(t)*radius,0.35D*Math.sin(t*1.9D),Math.sin(t)*radius);
        sword.toward(desired,0.35D+0.16D*progress);
        sword.visualScale((float)(0.65D+0.35D*progress));
        sword.flightSpeed(0.0F);
    }

    private static void tickMountTransition(ControlledSwordEntity sword, ServerPlayer player, Ride ride, FlyingSwordDefinition definition) {
        Vec3 target=player.position().add(0,definition.riderOffset(),0);
        if(ride.mountStart==null)ride.mountStart=sword.position();
        double progress=Mth.clamp(ride.stateTicks/(double)FlyingSwordConfig.MOUNT_TRANSITION_TICKS,0,1);
        double eased=progress*progress*(3-2*progress);
        Vec3 curve=ride.mountStart.lerp(target,eased).add(0,Math.sin(progress*Math.PI)*0.45,0);
        sword.toward(curve,1.5);
        sword.visualScale((float)Math.min(1.0D,0.65D+ride.stateTicks/(double)FlyingSwordConfig.MOUNT_TRANSITION_TICKS*0.35D));
        if (ride.stateTicks>=FlyingSwordConfig.MOUNT_TRANSITION_TICKS) {
            if(!player.level().noCollision(player,player.getBoundingBox().move(target.add(0,definition.riderOffset(),0).subtract(player.position())))){
                sword.art="mount_obstructed";
                com.mojang.logging.LogUtils.getLogger().debug("Sword mount obstructed at {} with bounds {}",target,player.getBoundingBox());
                stop(player);return;
            }
            sword.setPos(target); sword.visualScale(definition.flightScale());
            // The flight packet owns descent; a pre-mount sneak flag would immediately dismount in Player.rideTick.
            player.setShiftKeyDown(false);
            if (player.startRiding(sword,true)) {
                ride.state=FlyingSwordState.FLYING; ride.stateTicks=0;
                sword.setYRot(player.getYRot()); sword.setXRot(0); sword.roll(0);
                Network.sendSwordFlightState(player,true);
                FlightFeedbackEvent.emit(sword,FlightFeedbackEvent.Kind.MOUNT);
            } else { sword.art="mount_rejected";stop(player); }
        }
    }

    private static void tickFlight(ControlledSwordEntity sword, ServerPlayer player, Ride ride,
                                   FlyingSwordDefinition definition, ItemStack source) {
        if (!player.isPassenger() || player.getVehicle()!=sword) {
            ride.state=FlyingSwordState.DISMOUNT_TRANSITION; ride.stateTicks=0; return;
        }
        player.setShiftKeyDown(false);
        if(sword.distanceToSqr(player)>64){stop(player);return;}
        if(!tickAdvanced(sword,player,ride,definition,source))return;
        tickHuahong(sword,player,ride,definition,source);
        boolean hovering=ride.phase==YujianPhase.LINGKONG;
        float currentYaw=sword.getYRot();
        float currentPitch=sword.getXRot();
        float currentRoll=sword.roll();
        float yawDelta=Mth.wrapDegrees(ride.desiredYaw-currentYaw);
        double steer=definition.turnRate()/(1.0D+Math.abs(ride.manualRoll.velocity)*1.8D);
        if(ride.phase==YujianPhase.JIYU||ride.phase.burst())steer*=definition.advanced().fastHandling();
        if(ride.huahong.boosting())steer*=.65;
        if(hovering)steer=Math.min(0.35,steer*1.4);
        float nextYaw=currentYaw+(float)(yawDelta*Mth.clamp(steer,0.03D,0.35D));
        float targetPitch=(float)Mth.clamp(ride.desiredPitch*definition.pitchResponse(),-70.0D,70.0D);
        float nextPitch=sword.getXRot()+(targetPitch-sword.getXRot())*0.14F;
        sword.setYRot(nextYaw); sword.setXRot(nextPitch);

        double speedRatio=sword.flightSpeed()/definition.advanced().maxSpeed(YujianPhase.JIYU,definition);
        double low=Math.toRadians(FlyingSwordConfig.ROLL_LOW_MAX.get())/20;
        double normal=Math.toRadians(FlyingSwordConfig.ROLL_NORMAL_MAX.get())/20;
        double rollLimit=Mth.lerp(Mth.clamp(speedRatio,0,1),low,normal);
        if(speedRatio>1)rollLimit=Mth.lerp(Mth.clamp((speedRatio-1)/Math.max(0.01,definition.spiralSpeedMultiplier()),0,1),normal,definition.maxRollSpeed());
        int rollInput=hovering?0:ride.rollLeft==ride.rollRight?0:ride.rollLeft?-1:1;
        ride.manualRoll.tick(rollInput,rollLimit,definition.rollAcceleration(),Math.toRadians(FlyingSwordConfig.ROLL_START.get())/20,
                FlyingSwordConfig.ROLL_DAMPING.get(),ride.rollSensitivity,ride.releaseMode,ride.recenter);
        ride.recenter=false;
        double bankWeight=1-Mth.clamp(Math.abs(ride.manualRoll.velocity)/normal,0,1);
        float autoBank=(float)(Mth.clamp(-yawDelta*definition.bankingStrength(),-30.0D,30.0D)*bankWeight*bankWeight);
        ride.bank+=(autoBank-ride.bank)*0.15F;
        if(hovering){ride.bank*=0.65F;ride.manualRoll.settleUpright();}
        sword.roll((float)Math.toDegrees(ride.manualRoll.angle)+ride.bank);
        sword.bank(ride.bank);
        sword.angularSpeed((float)ride.manualRoll.velocity);
        sword.sprinting(ride.phase==YujianPhase.JIYU||ride.phase.burst());
        if(ride.stateTicks%20==0)FlightFeedbackEvent.emit(sword,Math.abs(ride.manualRoll.velocity)>0.2?FlightFeedbackEvent.Kind.ROLL:ride.sprint?FlightFeedbackEvent.Kind.SPRINT:FlightFeedbackEvent.Kind.WIND);

        Vec3 forward=direction(nextYaw,nextPitch);
        double ceiling=definition.advanced().maxSpeed(ride.phase,definition);
        boolean rolling=rollInput!=0 && ride.forward && !ride.brake && !hovering;
        var data=GeneratedMod.data(player);
        float jinghongCost=definition.advanced().jinghongCost();
        long now=player.level().getGameTime();
        boolean jinghongEligible=ride.huahong.stage()==0 && rolling && ride.phase==YujianPhase.JIYU && ride.sprint
                && ride.collisionTicks==0 && ProgressionGate.canJinghong(player,source)
                && sword.flightSpeed()>=definition.advanced().maxSpeed(YujianPhase.JIYU,definition)*JinghongBurst.ENTRY_SPEED
                && player.getPersistentData().getLong("YujianJinghongCooldown")<=now;
        boolean jinghongTriggered=ride.jinghong.tick(now,ride.manualRoll.holdTicks(),jinghongEligible,
                data!=null && data.wuxingxiantu$getAura()>=jinghongCost);
        if(ride.huahong.stage()!=0)ride.jinghong.cancelEffect();
        if(jinghongTriggered){
            data.wuxingxiantu$setAura(data.wuxingxiantu$getAura()-jinghongCost);
            player.getPersistentData().putLong("YujianJinghongCooldown",now+JinghongBurst.COOLDOWN_TICKS);
            FlightFeedbackEvent.emit(sword,FlightFeedbackEvent.Kind.ROLL);
        }
        if(ride.brake||!ride.forward||!ProgressionGate.canJinghong(player,source))
            ride.jinghong.cancelEffect();
        sword.jinghongStrength(ride.jinghong.strength());
        if(ride.rollPulseCooldown>0)ride.rollPulseCooldown--;
        if(rolling&&!ride.wasRolling&&ride.rollPulseCooldown==0){
            ride.rollPulseTicks=FlyingSwordConfig.ROLL_PULSE_TICKS.get();
            ride.rollPulseCooldown=FlyingSwordConfig.ROLL_PULSE_COOLDOWN.get();
            FlightFeedbackEvent.emit(sword,FlightFeedbackEvent.Kind.ROLL);
        }
        ride.wasRolling=rolling;
        if(!rolling)ride.rollPulseTicks=0;
        double pulse=ride.rollPulseTicks>0?Math.sin(Math.PI*(1.0D-ride.rollPulseTicks/(double)FlyingSwordConfig.ROLL_PULSE_TICKS.get())):0;
        if(ride.rollPulseTicks>0)ride.rollPulseTicks--;
        double cruiseCeiling=ceiling*FlyingSwordConfig.CRUISE_FRACTION.get();
        if(ride.forward&&!ride.brake)ride.cruiseTarget=Math.min(cruiseCeiling,ride.cruiseTarget+definition.acceleration());
        else ride.cruiseTarget=Math.max(0,ride.cruiseTarget-YujianTuning.DECELERATION);
        double bonus=Math.min(FlyingSwordConfig.SPIRAL_BONUS.get(),FlyingSwordConfig.ROLL_PULSE_BONUS.get());
        double targetSpeed=Math.min(ceiling,ride.cruiseTarget*(1.0D+bonus*pulse));
        // Scale the actual normal target once, including its cruise fraction and roll bonus.
        // Do not accumulate a rainbow multiplier into cruiseTarget.
        double rainbowMultiplier=ride.huahong.multiplier();
        targetSpeed*=rainbowMultiplier;
        ceiling*=rainbowMultiplier;
        if (ride.brake||hovering) targetSpeed=0.0D;
        Vec3 velocity=sword.getDeltaMovement();
        double previousSpeed=velocity.length();
        double acceleration=ride.phase.burst()?definition.advanced().burstAcceleration():ride.phase==YujianPhase.JIYU
                ?definition.advanced().jiyuAcceleration():Math.max(YujianTuning.DECELERATION,definition.acceleration());
        acceleration=Math.min(1,acceleration*(1+(FlyingSwordConfig.ROLL_PULSE_ACCELERATION.get()-1)*pulse));
        if(ride.huahong.boosting())acceleration=Math.max(acceleration,.55);
        if(ride.brake)acceleration=Math.max(acceleration,.18);
        Vec3 target=forward.scale(targetSpeed);
        velocity=new Vec3(Mth.lerp(acceleration,velocity.x,target.x),Mth.lerp(acceleration,velocity.y,target.y),Mth.lerp(acceleration,velocity.z,target.z));
        if (!ride.brake&&!ride.forward) velocity=velocity.scale(definition.drag());
        if (ride.up) velocity=velocity.add(0,0.055D,0);
        if (ride.down) velocity=velocity.add(0,-0.055D,0);
        // One impulse only. The normal cruise target stays unchanged and naturally absorbs the momentum.
        if(jinghongTriggered)velocity=velocity.add(forward.scale(JinghongBurst.IMPULSE));
        if(hovering){
            int lateral=ride.rollLeft==ride.rollRight?0:ride.rollLeft?-1:1;
            Vec3 sideways=direction(nextYaw+90,0).scale(lateral*YujianTuning.HOVER_HORIZONTAL);
            Vec3 hoverTarget=sideways.add(0,(ride.up?1:0)*YujianTuning.HOVER_VERTICAL-(ride.down?1:0)*YujianTuning.HOVER_VERTICAL,0);
            velocity=velocity.scale(YujianTuning.HOVER_DAMPING).add(hoverTarget.scale(1-YujianTuning.HOVER_DAMPING));
        }
        // A lower phase changes the target immediately, but sheds existing momentum over several ticks.
        double allowed=Math.max(ceiling,previousSpeed-YujianTuning.DECELERATION);
        if(jinghongTriggered)allowed=Math.max(allowed,velocity.length());
        if(velocity.length()>allowed)velocity=velocity.normalize().scale(allowed);
        velocity=YujianTuning.finiteVelocity(velocity);
        Vec3 before=sword.position();
        Vec3 oldTip=before.add(direction(currentYaw,currentPitch).scale(1.2D*definition.flightScale()));
        AABB body=FlightFrame.riderBounds(before,nextYaw,nextPitch,sword.roll(),definition.riderOffset());
        if(!FlightCollision.clear(player,body,ride.phase==YujianPhase.DUNXU)){
            sword.setYRot(currentYaw);sword.setXRot(currentPitch);sword.roll(currentRoll);
            ride.manualRoll.halt(Math.toRadians(currentRoll-ride.bank));
            body=FlightFrame.riderBounds(before,currentYaw,currentPitch,currentRoll,definition.riderOffset());
        }
        AABB volume=body.minmax(player.getBoundingBox()).minmax(sword.getBoundingBox());
        var sweep=FlightCollision.sweep(player,volume,velocity,ride.phase==YujianPhase.DUNXU);
        boolean blocked=sweep.blocked();velocity=sweep.movement();
        if(ride.collisionTicks>0){ride.collisionTicks--;velocity=velocity.scale(0.7);}
        sword.setDeltaMovement(velocity);
        // Only the sword's already-validated displacement bypasses vanilla solid collision during DUNXU.
        sword.noPhysics=ride.phase==YujianPhase.DUNXU;
        try{sword.move(MoverType.SELF,velocity);}finally{sword.noPhysics=false;}
        double moved=sword.position().distanceTo(before);
        sword.flightSpeed((float)moved); player.fallDistance=0;
        if(FlightCollision.clear(player,volume.move(sword.position().subtract(before)),false))rememberSafe(ride,sword.position());
        if(player.tickCount%YujianTuning.DIAGNOSTIC_INTERVAL==0)
            sword.diagnostics(targetSpeed,acceleration,sweep.loaded(),blocked,player.getPersistentData().getBoolean("YujianDebug"));
        if(ride.phase==YujianPhase.DUNXU && moved>=FlyingSwordConfig.EXTREME_SPEED.get()){
            if(++ride.extremeTicks>=FlyingSwordConfig.EXTREME_TICKS.get()&&!ride.extremeEmitted){
                ride.extremeEmitted=true;net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                        new SustainedExtremeYujianEvent(player,source,moved,ride.extremeTicks));
            }
        }else{ride.extremeTicks=0;ride.extremeEmitted=false;}
        if (moved > 0.02D && !blocked) {
            ride.nurtureDistance += moved;
            if (ride.nurtureDistance >= 1.0D && source.getItem() instanceof cn.blockforge.generated.generatedmod.items.SpiritSwordItem) {
                long whole=(long)ride.nurtureDistance;
                ride.nurtureDistance -= whole;
                var profile=source.getItem() instanceof cn.blockforge.generated.generatedmod.items.SpiritSwordItem spirit ? spirit.profile() : null;
                var nurture=cn.blockforge.generated.generatedmod.items.SpiritSwordItem.nurture(source);
                nurture.addExp(whole,profile);
                cn.blockforge.generated.generatedmod.items.SpiritSwordItem.saveNurture(source,nurture);
            }
        }
        // Waiting for terrain is not a wall impact: retain the cruise target
        // and phase, while still moving only through the validated loaded path.
        if(blocked){ride.huahong.exit();sword.huahong(ride.huahong.stage(),ride.huahong.strength());}
        if ((blocked&&sweep.loaded())||sword.horizontalCollision||sword.verticalCollision) {
            changePhase(sword,player,ride,YujianPhase.BASIC);
            ride.cruiseTarget=Math.min(ride.cruiseTarget,definition.baseSpeed());
            if(ride.collisionTicks==0){sword.impactNow();FlightFeedbackEvent.emit(sword,FlightFeedbackEvent.Kind.BLOCK_IMPACT);}
            ride.collisionTicks=6;
            // The sweep returns only the validated prefix of the requested displacement.
            sword.setDeltaMovement(velocity.scale(0.70D)); ride.manualRoll.velocity*=0.3D;
        }
        Vec3 tip=direction(sword.getYRot(),sword.getXRot()).scale(1.2D*definition.flightScale());
        sword.angularSpeed((float)ride.manualRoll.velocity);
        var tipWall=FlightCollision.clipLoaded(player,sword,sword.position(),sword.position().add(tip));
        if(ride.huahong.stage()==0 && ride.collisionTicks==0 && tipWall.isPresent() && tipWall.get().getType()==HitResult.Type.MISS)
            applyChargeDamage(sword,player,ride,source,oldTip,tipWall.get().getLocation(),definition);
        for (Iterator<Map.Entry<UUID,Integer>> it=ride.hitCooldown.entrySet().iterator();it.hasNext();) if(it.next().getValue()<=player.tickCount)it.remove();
    }

    /** Rainbow only changes speed/appearance. Collision always reads the original Dunxu phase. */
    private static void tickHuahong(ControlledSwordEntity sword,ServerPlayer player,Ride ride,
                                    FlyingSwordDefinition definition,ItemStack source){
        var data=GeneratedMod.data(player);
        double normal=definition.advanced().maxSpeed(ride.phase,definition)*FlyingSwordConfig.CRUISE_FRACTION.get();
        boolean eligible=HuahongFlight.supported(SwordProfile.of(source).tier())
                && ProgressionGate.canYujianPhase(player,source,YujianPhase.DUNXU)
                && ride.forward&&ride.sprint&&!ride.brake&&ride.collisionTicks==0
                && Math.abs(Mth.wrapDegrees(ride.desiredYaw-sword.getYRot()))<55
                && Math.abs(ride.desiredPitch-sword.getXRot())<45;
        if(eligible){
            AABB volume=FlightFrame.riderBounds(sword.position(),sword.getYRot(),sword.getXRot(),sword.roll(),sword.riderOffset())
                    .minmax(player.getBoundingBox()).minmax(sword.getBoundingBox());
            // Look ahead along momentum AND the steered direction; never loads terrain.
            double look=Math.max(normal*2,sword.flightSpeed())*2;
            boolean phasing=ride.phase==YujianPhase.DUNXU;
            var check=FlightCollision.sweep(player,volume,direction(sword.getYRot(),sword.getXRot()).scale(look),phasing);
            eligible=!check.blocked();
            if(eligible&&sword.getDeltaMovement().lengthSqr()>.01)
                eligible=!FlightCollision.sweep(player,volume,sword.getDeltaMovement().normalize().scale(look),phasing).blocked();
        }
        float max=CultivationLogic.maxAura(data);
        float entry=Math.max(1,(float)(max*HuahongFlight.ENTRY_COST));
        float upkeep=Math.max(.1F,(float)(max*HuahongFlight.UPKEEP_PER_SECOND/20));
        upkeep=cn.blockforge.generated.generatedmod.crafting.EquipmentCrafting.upkeep(source,upkeep);
        boolean funded=data.wuxingxiantu$getAura()>=(ride.huahong.stage()==0?entry+upkeep:upkeep);
        int before=ride.huahong.stage();
        if(ride.huahong.tick(eligible,sword.flightSpeed()>=normal*HuahongFlight.ENTRY_RATIO,funded))
            data.wuxingxiantu$setAura(data.wuxingxiantu$getAura()-entry);
        if(ride.huahong.stage()==1||ride.huahong.stage()==2){
            // tickAdvanced already charged ordinary upkeep. Refund it, except Dunxu's independent cost.
            float refund=ride.phase==YujianPhase.DUNXU?0:
                    cn.blockforge.generated.generatedmod.crafting.EquipmentCrafting.upkeep(source,definition.advanced().costPerTick(ride.phase));
            data.wuxingxiantu$setAura(Math.max(0,data.wuxingxiantu$getAura()+refund-upkeep));
        }
        int after=ride.huahong.stage();sword.huahong(after,ride.huahong.strength());
        if(after!=before&&after!=0){
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("yujian.wuxingxiantu.huahong.stage."+after),true);
            if(after==2)player.serverLevel().playSound(null,sword.blockPosition(),net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_1,
                    net.minecraft.sounds.SoundSource.PLAYERS,.45F,1.65F);
        }
    }

    private static void tickDismount(ControlledSwordEntity sword, ServerPlayer player, Ride ride) {
        if (player.getVehicle()==sword) {player.stopRiding();Network.sendSwordFlightState(player,false);FlightFeedbackEvent.emit(sword,FlightFeedbackEvent.Kind.DISMOUNT);}
        Vec3 previous=sword.position();
        Vec3 target=player.position().add(direction(sword.getYRot(),0).scale(1.5D)).add(0,0.45D,0);
        sword.toward(target,0.4D);
        sword.setDeltaMovement(direction(sword.getYRot(),sword.getXRot()).scale(0.15D));
        if (ride.stateTicks>12 || sword.distanceToSqr(target)<0.18D) stop(player);
    }

    private static void applyChargeDamage(ControlledSwordEntity sword, ServerPlayer player, Ride ride, ItemStack source,
                                          Vec3 from, Vec3 to, FlyingSwordDefinition definition) {
        double speed=sword.flightSpeed();
        if (speed<FlyingSwordConfig.CHARGE_SPEED.get() || Math.abs(ride.manualRoll.velocity)<FlyingSwordConfig.CHARGE_ROLL.get()) return;
        var wall=player.serverLevel().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,sword));
        to=wall.getLocation();
        AABB sweep=new AABB(from,to).inflate(0.7D);
        double multiplier=1.0D+speed*3.0D+Math.abs(ride.manualRoll.velocity)*5.0D;
        for (LivingEntity target:player.serverLevel().getEntitiesOfClass(LivingEntity.class,sweep,e->canTarget(player,e))) {
            if (ride.hitCooldown.containsKey(target.getUUID())) continue;
            if (!target.getBoundingBox().inflate(0.35D).contains(from) && target.getBoundingBox().inflate(0.35D).clip(from,to).isEmpty()) continue;
            ride.hitCooldown.put(target.getUUID(),player.tickCount+FlyingSwordConfig.HIT_COOLDOWN_TICKS);
            if(cn.blockforge.generated.generatedmod.combat.HeartSwordCombatProfile.swordHit(player,source,target,
                    definition.impactDamage(),0,multiplier,FlyingSwordConfig.MAX_CHARGE_DAMAGE.get())) {
                sword.impactNow();
                FlightFeedbackEvent.emit(sword,FlightFeedbackEvent.Kind.ENTITY_IMPACT);
                Vec3 push=direction(sword.getYRot(),0).scale(Math.min(1.8D,0.3D+speed*0.9D));
                target.push(push.x,0.18D,push.z); target.hurtMarked=true;
            }
        }
    }

    private static Vec3 direction(float yaw,float pitch) {
        double y=Math.toRadians(yaw), p=Math.toRadians(pitch), cp=Math.cos(p);
        return new Vec3(-Math.sin(y)*cp,-Math.sin(p),Math.cos(y)*cp).normalize();
    }

    private static boolean validEquippedSword(ServerPlayer player, Ride ride) {
        ItemStack source=YujianSlot.activeFlyingSword(player);
        if(!YujianSlot.accepts(source)||!ProgressionGate.canYujian(player,GeneratedMod.data(player),source))return false;
        // The slot itself is the authoritative binding while riding.  If the
        // player replaces one valid spirit sword with another, adopt its binding
        // instead of treating the replacement as an invalid removal.
        if(!matchesBinding(source,ride.sword))ride.sword=binding(source);
        return true;
    }

    private static void rememberSafe(Ride ride,Vec3 position){
        if(ride.safePositions.isEmpty()||ride.safePositions.peekLast().distanceToSqr(position)>0.04){
            ride.safePositions.addLast(position);while(ride.safePositions.size()>YujianTuning.SAFE_HISTORY)ride.safePositions.removeFirst();
        }
    }
    private static boolean restoreSafePosition(ControlledSwordEntity sword,ServerPlayer player,Ride ride){
        AABB volume=FlightFrame.riderBounds(sword.position(),sword.getYRot(),sword.getXRot(),sword.roll(),sword.riderOffset())
                .minmax(player.getBoundingBox()).minmax(sword.getBoundingBox());
        if(FlightCollision.clear(player,volume,false))return true;
        for(var iterator=ride.safePositions.descendingIterator();iterator.hasNext();){
            Vec3 candidate=iterator.next();
            if(FlightCollision.clear(player,volume.move(candidate.subtract(sword.position())),false)){
                sword.setPos(candidate);player.setPos(candidate.add(0,sword.riderOffset(),0));sword.setDeltaMovement(Vec3.ZERO);return true;
            }
        }
        // A nearby block edit may invalidate the history. Search only bounded, already-loaded space.
        for(int y=1;y<=12;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            Vec3 offset=new Vec3(x,y,z);
            if(FlightCollision.clear(player,volume.move(offset),false)){
                Vec3 point=sword.position().add(offset);sword.setPos(point);player.setPos(point.add(0,sword.riderOffset(),0));
                sword.setDeltaMovement(Vec3.ZERO);return true;
            }
        }
        // Last resort for terrain edits: a loaded, collision-free column above the flight route.
        for(int y=player.level().getMaxBuildHeight()-4;y>player.level().getMinBuildHeight();y--){
            Vec3 point=new Vec3(sword.getX(),y,sword.getZ());
            if(FlightCollision.clear(player,volume.move(point.subtract(sword.position())),false)){
                sword.setPos(point);player.setPos(point.add(0,sword.riderOffset(),0));sword.setDeltaMovement(Vec3.ZERO);return true;
            }
        }
        // Persist a normal respawn recovery instead of leaving a player embedded with phasing enabled.
        player.getPersistentData().putBoolean("YujianEmergencyReturn",true);
        return false;
    }
    private static void changePhase(ControlledSwordEntity sword,ServerPlayer player,Ride ride,YujianPhase phase){
        if(ride.phase==phase)return;
        if(ride.phase==YujianPhase.DUNXU)restoreSafePosition(sword,player,ride);
        ride.phase=phase;ride.phaseTicks=0;ride.extremeTicks=0;ride.extremeEmitted=false;
        ride.rollPulseTicks=0;
        if(phase!=YujianPhase.DUNXU){ride.dunxuStage=0;ride.dunxuStageTicks=0;ride.dunxuHoldTicks=0;}
        sword.advancedPhase(phase);
        sword.dunxuStage(phase==YujianPhase.DUNXU?ride.dunxuStage:0);
        if(phase==YujianPhase.DUNXU)
            player.serverLevel().playSound(null,sword.blockPosition(),net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_1,
                    net.minecraft.sounds.SoundSource.PLAYERS,0.45F,0.6F);
    }
    private static YujianPhase lowerPhase(YujianPhase phase){
        return phase==YujianPhase.DUNXU?YujianPhase.JIYU:YujianPhase.BASIC;
    }
    private static boolean tickAdvanced(ControlledSwordEntity sword,ServerPlayer player,Ride ride,FlyingSwordDefinition definition,ItemStack source){
        var data=GeneratedMod.data(player);var profile=definition.advanced();
        if(data==null||!data.wuxingxiantu$getActiveDivineArt().isBlank()){stop(player);return false;}
        rememberSafeIfClear(sword,player,ride);
        ride.phaseTicks++;
        if (ride.sprint && ride.forward && !ride.brake) ride.dunxuHoldTicks++;
        else ride.dunxuHoldTicks = 0;
        YujianPhase wanted=ride.phase;
        long now=player.level().getGameTime();
        if(ride.phase==YujianPhase.DUNXU){
            ride.dunxuStageTicks++;
            if(ride.dunxuStage==1 && ride.dunxuStageTicks>=YujianTuning.DUNXU_PRE_TICKS)ride.dunxuStage=2;
            if(ride.dunxuStage==2 && (!ride.sprint||!ride.forward||ride.brake
                    ||data.wuxingxiantu$getAura()<Math.max(0.1F,CultivationAuraCost(data)))){
                ride.dunxuStage=3;ride.dunxuStageTicks=0;
            }
            if(ride.dunxuStage==3 && ride.dunxuStageTicks>=YujianTuning.DUNXU_EXIT_TICKS)wanted=YujianPhase.JIYU;
            else wanted=YujianPhase.DUNXU;
        } else {
            wanted=ride.brake&&!ride.forward&&ProgressionGate.canYujianPhase(player,source,YujianPhase.LINGKONG)?YujianPhase.LINGKONG
                    :ride.sprint&&ride.forward?YujianPhase.JIYU:ride.forward?YujianPhase.ACCELERATING:YujianPhase.BASIC;
            int entryCost=Math.max(1,Math.round(cn.blockforge.generated.generatedmod.items.CultivationLogic.maxAura(data)
                    *(float)YujianTuning.DUNXU_ENTRY_AURA_FRACTION));
            boolean trigger=ride.sprint&&ride.forward&&!ride.brake&&ride.dunxuHoldTicks>=60
                    &&ride.collisionTicks==0
                    &&ProgressionGate.canYujianPhase(player,source,YujianPhase.DUNXU)
                    &&player.getPersistentData().getLong("YujianCooldown_DUNXU")<=now
                    &&data.wuxingxiantu$getAura()>=entryCost;
            if(trigger){
                wanted=YujianPhase.DUNXU;ride.dunxuStage=1;ride.dunxuStageTicks=0;
                data.wuxingxiantu$setAura(data.wuxingxiantu$getAura()-entryCost);
                player.getPersistentData().putLong("YujianCooldown_DUNXU",now+YujianTuning.DUNXU_COOLDOWN);
            }
        }
        if(ride.collisionTicks>0&&wanted!=YujianPhase.DUNXU)wanted=YujianPhase.BASIC;
        float upkeep=cn.blockforge.generated.generatedmod.crafting.EquipmentCrafting.upkeep(source,profile.costPerTick(YujianPhase.BASIC));
        while(wanted!=YujianPhase.BASIC){
            float candidateCost=wanted==YujianPhase.DUNXU
                    ?(ride.dunxuStage==2?CultivationAuraCost(data):0.0F):profile.costPerTick(wanted);
            candidateCost=cn.blockforge.generated.generatedmod.crafting.EquipmentCrafting.upkeep(source,candidateCost);
            if(ProgressionGate.canYujianPhase(player,source,wanted)&&data.wuxingxiantu$getAura()>=candidateCost){
                upkeep=candidateCost;break;
            }
            wanted=lowerPhase(wanted);
        }
        if(data.wuxingxiantu$getAura()<cn.blockforge.generated.generatedmod.crafting.EquipmentCrafting.upkeep(source,profile.costPerTick(YujianPhase.BASIC))){
            changePhase(sword,player,ride,YujianPhase.BASIC);stop(player);return false;
        }
        changePhase(sword,player,ride,wanted);
        if(player.getPersistentData().getBoolean("YujianEmergencyReturn")){stop(player);return false;}
        if(upkeep>0)data.wuxingxiantu$setAura(data.wuxingxiantu$getAura()-upkeep);
        sword.dunxuStage(ride.phase==YujianPhase.DUNXU?ride.dunxuStage:0);
        return true;
    }
    private static float CultivationAuraCost(CultivationData data){
        return Math.max(0.1F,cn.blockforge.generated.generatedmod.items.CultivationLogic.maxAura(data)
                *(float)YujianTuning.DUNXU_TICK_AURA_FRACTION);
    }
    private static void rememberSafeIfClear(ControlledSwordEntity sword,ServerPlayer player,Ride ride){
        AABB box=FlightFrame.riderBounds(sword.position(),sword.getYRot(),sword.getXRot(),sword.roll(),sword.riderOffset())
                .minmax(player.getBoundingBox()).minmax(sword.getBoundingBox());
        if(FlightCollision.clear(player,box,false))rememberSafe(ride,sword.position());
    }
    private static boolean burstPathClear(ControlledSwordEntity sword,ServerPlayer player,FlyingSwordDefinition definition,YujianPhase phase){
        AABB box=FlightFrame.riderBounds(sword.position(),sword.getYRot(),sword.getXRot(),sword.roll(),sword.riderOffset())
                .minmax(player.getBoundingBox()).minmax(sword.getBoundingBox());
        return !FlightCollision.sweep(player,box,direction(sword.getYRot(),sword.getXRot())
                .scale(definition.advanced().maxSpeed(phase,definition)),phase==YujianPhase.DUNXU).blocked();
    }

    public static void tickSword(ControlledSwordEntity s,ServerPlayer p){
        if(s.support()){if(!riding(p)){s.discard();return;}s.setPos(p.position().add(0,-0.18,0));s.setYRot(p.getYRot());s.setXRot(0);return;}
        if(find(p,s.binding).isEmpty()){s.discard();return;}
        var data=GeneratedMod.data(p);if(data==null||!data.wuxingxiantu$getActiveDivineArt().isBlank()||s.distanceToSqr(p)>96*96){s.discard();return;}
        Vec3 previous=s.position();int t=s.age;
        if(t>300){s.discard();return;}
        switch(s.art){
            case "art_sword_pierce" -> {
                if(t<5){s.toward(FormationGeometry.launchClearPoint(p,s.slot,t),0.6);return;}
                if(t<12){s.toward(FormationGeometry.risePoint(FormationGeometry.launchClearPoint(p,s.slot,t),p.getLookAngle()),0.8);return;}
                var target=s.target==null?null:p.serverLevel().getEntity(s.target);
                if(target instanceof LivingEntity living&&canTarget(p,living))s.anchor=living.position().add(0,living.getBbHeight()*0.5,0);
                // A short star-like feint gives the strike a readable wind-up before the final line.
                Vec3 aim = s.anchor == null ? p.getEyePosition().add(p.getLookAngle().scale(8)) : s.anchor;
                if (t < 22) {
                    Vec3 line = aim.subtract(p.getEyePosition());
                    if (line.lengthSqr() < 1.0E-4D) line = p.getLookAngle();
                    line = line.normalize();
                    Vec3 side = line.cross(new Vec3(0, 1, 0));
                    if (side.lengthSqr() < 1.0E-4D) side = new Vec3(1, 0, 0);
                    side = side.normalize();
                    double phase = (t - 12) / 10.0D;
                    Vec3 feint = aim.subtract(line.scale(3.2D - phase * 1.8D))
                            .add(side.scale(Math.sin(phase * Math.PI * 1.5D) * 0.9D))
                            .add(0, Math.sin(phase * Math.PI) * 0.55D, 0);
                    s.toward(feint, 1.2);
                } else s.toward(aim,1.35);
                hitAlong(s,p,previous,s.position(),1);
                if(s.position().distanceToSqr(aim)<0.5||t>65)s.art="return";
            }
            case "art_sword_sweep" -> {
                double travel=Math.max(0,t-4);
                double angle=2*Math.PI*s.slot/6.0+travel/42.0*Math.PI*6;
                double radius=3.15+0.35*Math.sin(travel*0.22+s.slot*1.2);
                Vec3 radial=new Vec3(Math.cos(angle),0,Math.sin(angle));
                Vec3 desired=p.position().add(0,1.05+Math.sin(angle*0.5)*0.48+(s.slot%2)*0.12,0)
                        .add(radial.scale(radius));
                s.toward(desired,t<4?1:3);if(t>=4){hitAlong(s,p,previous,s.position(),0.65);hitAlong(s,p,p.position().add(0,1,0),s.position(),0.65);}
                if(t>=44)s.art="return";
            }
            case "art_sword_guard" -> {s.toward(FormationGeometry.guardPosition(p,s.slot,t),0.8);if(t>=160)s.art="return";}
            case "art_sword_array", "art_myriad_swords" -> {
                boolean myriad=s.art.equals("art_myriad_swords");
                if(t>=88&&s.slot==0){
                    s.visualScale(myriad?2.8F:4.0F);s.setXRot(90);s.setPos(s.anchor.add(0,Math.max(0,(96-t)*(myriad?0.72:0.9)),0));
                    if(t==96)for(var victim:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(s.anchor,s.anchor).inflate(4),e->canTarget(p,e)))damage(s,p,victim,1.8);
                    if(t>=100){s.visualScale(1.0F);s.art="return";}return;
                }
                int ringSlot=myriad?s.slot%6:s.slot;
                int ringLayer=myriad?s.slot/6:0;
                double angle=2*Math.PI*ringSlot/6+t*(myriad?0.022:0.016)+ringLayer*0.24;
                double ringRadius=myriad?(ringLayer==0?3.15:4.65):3.15;
                double ringHeight=myriad?(ringLayer==0?5.2:6.2):5.0;
                Vec3 ring=s.anchor.add(Math.cos(angle)*ringRadius,ringHeight,Math.sin(angle)*ringRadius);
                if(t<24)s.toward(ring,1);
                else {
                    int cycle=(t-24+s.slot*3)%36;
                    double drop=cycle<16?cycle/16.0D:1.0D;
                    double radiusNow=ringRadius*(1.0D-drop*0.52D);
                    Vec3 point=s.anchor.add(Math.cos(angle)*radiusNow,
                            ringHeight-drop*4.35D+ringLayer*0.18D,Math.sin(angle)*radiusNow);
                    s.toward(point,1.35);if(cycle==0)s.hits.clear();hitAlong(s,p,previous,s.position(),0.5);
                }
                if(t>=100)s.art="return";
            }            default -> {
                Vec3 rally=FormationGeometry.returnRallyPoint(p,s.slot);
                Vec3 dock=FormationGeometry.dockPosition(p,s.slot,FormationMode.FAN_ALIGNED,t);
                if(s.art.equals("return")&&s.position().distanceToSqr(rally)>0.6)s.toward(rally,1.2);
                else{s.art="dock";s.toward(dock,Math.max(0.6,p.getDeltaMovement().length()+0.4));s.setYRot(p.getYRot());s.setXRot(-70);}
            }
        }
    }
    private static void hitAlong(ControlledSwordEntity s,ServerPlayer p,Vec3 from,Vec3 to,double scale){
        var wall=p.serverLevel().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s));
        Vec3 end=wall.getLocation();
        for(var target:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(0.6),e->canTarget(p,e)))
            if(!s.hits.contains(target.getUUID())&&(target.getBoundingBox().inflate(0.5).contains(from)||target.getBoundingBox().inflate(0.5).clip(from,end).isPresent())){s.hits.add(target.getUUID());damage(s,p,target,scale);}
    }
    private static void damage(ControlledSwordEntity s,ServerPlayer p,LivingEntity target,double scale){
        var stack=find(p,s.binding);if(!(stack.getItem() instanceof SwordItem sword))return;
        cn.blockforge.generated.generatedmod.combat.HeartSwordCombatProfile.swordHit(p,stack,target,
                sword.getDamage(),net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(stack,target.getMobType()),
                scale*(1+s.mastery/200.0),Double.MAX_VALUE);
    }
    @SubscribeEvent public static void guard(net.minecraftforge.event.entity.living.LivingHurtEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p)||event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                ||cn.blockforge.generated.generatedmod.combat.HeartSwordCombatProfile.isHeartTrue(event.getSource()))return;
        var guards=p.serverLevel().getEntitiesOfClass(ControlledSwordEntity.class,p.getBoundingBox().inflate(4),s->p.getUUID().equals(s.ownerId)&&s.art.equals("art_sword_guard"));
        if(guards.isEmpty()||event.getSource().getDirectEntity()==null)return;
        var sword=guards.get(0);var stack=find(p,sword.binding);if(stack.isEmpty())return;
        float blocked=event.getAmount()*Math.min(0.7F,0.2F+guards.size()*0.07F);
        event.setAmount(event.getAmount()-blocked);
        if(event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile){projectile.setOwner(p);projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.8));projectile.hasImpulse=true;}
    }
}

