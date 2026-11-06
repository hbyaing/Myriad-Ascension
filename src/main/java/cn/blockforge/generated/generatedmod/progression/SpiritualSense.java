package cn.blockforge.generated.generatedmod.progression;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog.Stat;
import cn.blockforge.generated.generatedmod.network.packet.ProgressionStatePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID)
public final class SpiritualSense {
    private static final Map<UUID,Session> ACTIVE=new HashMap<>();
    private static final class Session {long actionAt,scanAt,syncAt,scanUntil;int channel,remaining;UUID target;Vec3 origin,look;int[] scanned=new int[0];CompoundTag lastState,visual=new CompoundTag();long visualSerial;String dimension="",notice="";}
    public static int capacity(int realm){return realm<2?0:new int[]{120,260,500,900,1500,2400,3600}[Math.min(6,realm-2)];}
    private static int capacity(ServerPlayer player, int realm) {
        int base = capacity(realm);
        var data = GeneratedMod.data(player);
        return base == 0 || data == null ? base : base + (int) Math.round(data.acupoints().bonuses().get(Stat.SENSE_CAPACITY));
    }
    public static int radius(int realm){return realm<2?0:Math.min(64,16+8*(realm-2));}
    private static int radius(ServerPlayer player, int realm) {
        int base = radius(realm);
        var data = GeneratedMod.data(player);
        return base == 0 || data == null ? base : base + (int) Math.round(data.acupoints().bonuses().get(Stat.SENSE_SCAN_RADIUS));
    }
    private static int realm(ServerPlayer p){var d=GeneratedMod.data(p);return d==null?0:d.wuxingxiantu$getRealm();}
    private static CompoundTag account(ServerPlayer p){return PlayerCrafts.get(p.server).account(p.getUUID());}
    public static boolean channeling(ServerPlayer p){var s=ACTIVE.get(p.getUUID());return s!=null&&s.channel!=0;}
    public static boolean visible(ServerPlayer p,LivingEntity e,int distance){return e!=p&&e.isAlive()&&!e.isSpectator()&&p.distanceToSqr(e)<=distance*distance&&p.hasLineOfSight(e);}
    private static LivingEntity aimed(ServerPlayer p){
        Vec3 eye=p.getEyePosition(),look=p.getLookAngle(),end=eye.add(look.scale(24));
        return p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().expandTowards(look.scale(24)).inflate(1),e->visible(p,e,24))
            .stream().filter(e->e.getBoundingBox().inflate(.25).clip(eye,end).isPresent()).min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }
    private static void tell(ServerPlayer p,Session s,String text){s.notice=text;p.displayClientMessage(net.minecraft.network.chat.Component.literal(text),true);}
    /** Transient visual receipt: only successful server actions can advance its serial. */
    private static void visual(ServerPlayer p,Session s,String kind,int duration,Vec3 position,int target,float radius){
        var tag=new CompoundTag();tag.putLong("Serial",++s.visualSerial);tag.putLong("Start",p.level().getGameTime());tag.putString("Kind",kind);
        tag.putInt("Duration",duration);tag.putDouble("X",position.x);tag.putDouble("Y",position.y);tag.putDouble("Z",position.z);tag.putInt("Target",target);tag.putFloat("Radius",radius);s.visual=tag;
    }
    public static void action(ServerPlayer p,String action){
        if(!p.isAlive()||p.isSpectator())return;
        if(!Set.of("refresh","open","scan","identify","lock","survey","craft:alchemy","craft:refining","craft:talisman").contains(action))return;
        var s=ACTIVE.computeIfAbsent(p.getUUID(),id->new Session());long now=p.server.overworld().getGameTime();
        if(action.equals("refresh")){if(now>=s.syncAt){s.syncAt=now+20;sync(p);}return;}
        if(now<s.actionAt)return;s.actionAt=now+5;
        var n=account(p);int r=realm(p);
        if(action.startsWith("craft:")){String id=action.substring(6);tell(p,s,PlayerCrafts.get(p.server).learnOrPromote(p,id));sync(p);return;}
        if(action.equals("survey")&&cn.blockforge.generated.generatedmod.divinesurvey.DivineSurvey.active(p)){
            cn.blockforge.generated.generatedmod.divinesurvey.DivineSurvey.toggle(p);sync(p);return;
        }
        if(s.channel!=0){tell(p,s,"正在凝神，请先完成当前动作。");sync(p);return;}
        if(action.equals("survey")){cn.blockforge.generated.generatedmod.divinesurvey.DivineSurvey.toggle(p);sync(p);return;}
        if(action.equals("open")){
            if(r<2){tell(p,s,"金丹境方可开辟识海。");sync(p);return;}
            if(!n.getBoolean("SenseOpen")){s.channel=1;s.remaining=100;s.origin=p.position();s.dimension=p.level().dimension().location().toString();visual(p,s,"opening",100,p.position(),-1,1.4f);tell(p,s,"凝神开辟识海：保持静止五息。");}
            sync(p);return;
        }
        if(!n.getBoolean("SenseOpen")||r<2){tell(p,s,"请先在识海页开辟识海。");sync(p);return;}
        float power=Float.isFinite(n.getFloat("Sense"))?Math.max(0,Math.min(capacity(p,r),n.getFloat("Sense"))):0;
        if(action.equals("scan")){
            if(now<s.scanAt||power<8){tell(p,s,"探查冷却中或神识不足（需8点）。");sync(p);return;}
            n.putFloat("Sense",power-8);s.scanAt=now+80;
            int range=radius(p,r),seen=0,hidden=0;double x=0,z=0;var scanned=new ArrayList<Integer>();
            for(var e:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range),e->e!=p&&e.isAlive()&&!e.isSpectator())){
                if(p.distanceToSqr(e)>range*range)continue;
                if(p.hasLineOfSight(e)){seen++;if(scanned.size()<16)scanned.add(e.getId());}else{hidden++;x+=e.getX()-p.getX();z+=e.getZ()-p.getZ();}
                if(seen+hidden>=64)break;
            }
            String direction=Math.abs(x)>Math.abs(z)?(x>0?"东":"西"):(z>0?"南":"北");
            s.scanned=scanned.stream().mapToInt(Integer::intValue).toArray();s.scanUntil=now+60;visual(p,s,"scan",60,p.position().add(0,1,0),-1,range);
            tell(p,s,"神识探查：可见生灵"+seen+"；"+(hidden==0?"未感到隐蔽气息。":direction+"侧有"+(hidden>8?"密集":"微弱")+"气息（方位近似）。"));
        }else if(action.equals("identify")){
            if(power<6){tell(p,s,"凝神辨识需要6点神识。");sync(p);return;}
            s.channel=2;s.remaining=40;s.origin=p.position();s.look=p.getLookAngle();s.dimension=p.level().dimension().location().toString();var target=aimed(p);s.target=target==null?null:target.getUUID();
            n.putFloat("Sense",power-6);visual(p,s,"identify",40,target==null?p.pick(12,0,false).getLocation():target.getBoundingBox().getCenter(),target==null?-1:target.getId(),1);tell(p,s,"凝神辨识：保持视线两息。");
        }else if(action.equals("lock")){
            if(s.target!=null&&s.channel==0){s.target=null;visual(p,s,"unlock",12,p.position(),-1,1);tell(p,s,"已解除神识锁定。");}
            else {var target=aimed(p);if(target==null||power<4){tell(p,s,"请瞄准24格内可见生灵，锁定需4点神识。");sync(p);return;}s.channel=0;s.target=target.getUUID();n.putFloat("Sense",power-4);visual(p,s,"lock",18,target.getBoundingBox().getCenter(),target.getId(),1);s.dimension=p.level().dimension().location().toString();tell(p,s,"锁定："+target.getName().getString()+"（每秒消耗1点）");}
        }
        PlayerCrafts.get(p.server).setDirty();sync(p);
    }
    public static CompoundTag snapshot(ServerPlayer p){
        var n=account(p).copy();var s=ACTIVE.computeIfAbsent(p.getUUID(),id->new Session());n.putInt("Capacity",capacity(p,realm(p)));n.putInt("Radius",radius(p,realm(p)));n.putInt("Realm",realm(p));
        for(String id:PlayerCrafts.IDS){var row=n.getCompound(id);row.putInt("WorksCount",row.getCompound("Works").size());row.remove("Works");n.put(id,row);}
        if(s!=null){n.putString("Notice",s.notice);n.putInt("Channel",s.channel);n.putInt("Remaining",s.remaining);n.putInt("ScanCooldown",(int)Math.max(0,s.scanAt-p.server.overworld().getGameTime()));
            if(s.scanUntil>p.server.overworld().getGameTime())n.putIntArray("ScanTargets",s.scanned);
            if(s.target!=null&&s.channel==0){var e=p.serverLevel().getEntity(s.target);if(e!=null){n.putString("Lock",e.getName().getString());n.putInt("LockEntity",e.getId());}}}
        n.put("SenseVisual",s.visual.copy());return n;
    }
    public static void sync(ServerPlayer p){
        var n=snapshot(p);var s=ACTIVE.get(p.getUUID());if(n.equals(s.lastState))return;s.lastState=n.copy();
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new ProgressionStatePacket(n));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        var s=ACTIVE.computeIfAbsent(p.getUUID(),id->new Session());var n=account(p);int r=realm(p);
        if(!p.isAlive()||p.isSpectator()||r<2){s.channel=0;s.target=null;}
        if(s.channel!=0){
            boolean interrupted=s.origin==null||p.position().distanceToSqr(s.origin)>.09||!s.dimension.equals(p.level().dimension().location().toString());
            if(s.channel==2&&(s.look==null||p.getLookAngle().dot(s.look)<.985))interrupted=true;
            if(s.channel==2&&s.target!=null){var target=aimed(p);interrupted|=target==null||!target.getUUID().equals(s.target);}
            if(interrupted){visual(p,s,"interrupt",10,p.position(),-1,1);s.channel=0;s.target=null;tell(p,s,"凝神中断。");sync(p);}
            else if(--s.remaining<=0){int channel=s.channel;s.channel=0;
                if(channel==1){visual(p,s,"opened",30,p.position().add(0,1,0),-1,2);n.putBoolean("SenseOpen",true);n.putFloat("Sense",capacity(p,r));tell(p,s,"识海已开，可探查、辨识与锁定。");}
                else {var target=s.target==null?null:p.serverLevel().getEntity(s.target);visual(p,s,"identified",20,target==null?new Vec3(s.visual.getDouble("X"),s.visual.getDouble("Y"),s.visual.getDouble("Z")):target.getBoundingBox().getCenter(),target==null?-1:target.getId(),1);if(target instanceof LivingEntity living)tell(p,s,target.getName().getString()+" · 气血 "+Math.round(living.getHealth())+" / "+Math.round(living.getMaxHealth()));
                    else {var eye=p.getEyePosition();var end=eye.add(p.getLookAngle().scale(12));
                        var item=p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().expandTowards(p.getLookAngle().scale(12)).inflate(1),it->p.hasLineOfSight(it)&&it.getBoundingBox().inflate(.3).clip(eye,end).isPresent()).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
                        if(item!=null){var stack=item.getItem();var pill=cn.blockforge.generated.generatedmod.alchemy.PillData.read(stack);tell(p,s,"辨识："+stack.getHoverName().getString()+" ×"+stack.getCount()+(pill==null?stack.isDamageableItem()?" · 耐久 "+(stack.getMaxDamage()-stack.getDamageValue()):"":" · 丹药品阶 "+(pill.grade().ordinal()+1)));}
                        else{var hit=p.pick(12,0,false);var pos=net.minecraft.core.BlockPos.containing(hit.getLocation().subtract(p.getLookAngle().scale(.02)));tell(p,s,"辨识："+p.level().getBlockState(pos).getBlock().getName().getString());}}s.target=null;}
                PlayerCrafts.get(p.server).setDirty();sync(p);
            }
        }
        if(p.tickCount%20!=0)return;
        float power=Float.isFinite(n.getFloat("Sense"))?n.getFloat("Sense"):0;
        if(s.target!=null&&s.channel==0){var target=p.serverLevel().getEntity(s.target);
            if(!(target instanceof LivingEntity living)||!visible(p,living,24)||!s.dimension.equals(p.level().dimension().location().toString())||power<1){visual(p,s,"unlock",12,p.position(),-1,1);s.target=null;tell(p,s,"目标丢失，神识锁定解除。");}else power-=1;
        }else if(s.channel==0&&n.getBoolean("SenseOpen")){
            var d=GeneratedMod.data(p);float regen=r<2?0:new float[]{1.2f,2,3,4.5f,6,8,10}[Math.min(6,r-2)];power+=regen*(d!=null&&d.wuxingxiantu$isMeditating()?2:1)*(d==null?1:d.acupoints().bonuses().multiplier(Stat.SENSE_REGEN_PCT));
        }
        power=Math.max(0,Math.min(capacity(p,r),power));
        if(Float.compare(n.getFloat("Sense"),power)!=0){n.putFloat("Sense",power);PlayerCrafts.get(p.server).setDirty();}sync(p);
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=ACTIVE.get(p.getUUID());if(s!=null&&s.channel!=0){visual(p,s,"interrupt",10,p.position(),-1,1);s.channel=0;s.target=null;tell(p,s,"受击，凝神中断。");sync(p);}}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){ACTIVE.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){ACTIVE.remove(e.getEntity().getUUID());if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){ACTIVE.remove(e.getEntity().getUUID());if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){ACTIVE.remove(e.getEntity().getUUID());}
}
