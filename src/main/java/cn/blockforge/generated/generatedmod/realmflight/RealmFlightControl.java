package cn.blockforge.generated.generatedmod.realmflight;

import cn.blockforge.generated.generatedmod.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Shared bounded movement simulation; inputs convey intent, never position or speed. */
public final class RealmFlightControl {
    // An integrated server and its client tick on different threads. Keep
    // each logical side's weak states on that side's own game thread.
    private static final Map<Player,State> CLIENT_STATES=new WeakHashMap<>();
    private static final Map<Player,State> SERVER_STATES=new WeakHashMap<>();
    private static Map<Player,State> states(Player p){return p.level().isClientSide()?CLIENT_STATES:SERVER_STATES;}
    private static final class State {Vec3 input=Vec3.ZERO,velocity=Vec3.ZERO; boolean boost; long received; double debt,boostWeight; net.minecraft.world.level.Level world;}
    private static State state(Player p){
        var map=states(p);var s=map.computeIfAbsent(p,k->new State());
        if(s.world!=p.level()||p.level().getGameTime()<s.received){
            s=new State();s.world=p.level();s.received=p.level().getGameTime();map.put(p,s);
        }
        return s;
    }
    public static boolean eligible(Player p){
        var d=GeneratedMod.data(p); if(ParalysisControl.blocked(p)||d==null||d.wuxingxiantu$getRealm()<3||!p.isAlive()||p.isCreative()||p.isSpectator()||p.isPassenger()||p.isFallFlying()||p.isInWater())return false;
        String art=d.wuxingxiantu$getActiveDivineArt();
        return !(p instanceof ServerPlayer server&&cn.blockforge.generated.generatedmod.yujian.YujianController.riding(server))
            &&!Set.of("jin_dou_yun","yuan_shen_chu_qiao","yi_hun_ji_po","qi_shi_er_bian").contains(art);
    }
    public static void input(Player p,Vec3 direction,boolean boost){
        if(!eligible(p)||!p.getAbilities().flying)return;
        if(!Double.isFinite(direction.x)||!Double.isFinite(direction.y)||!Double.isFinite(direction.z))return;
        State s=state(p);
        s.input=direction.lengthSqr()>1?direction.normalize():direction;
        s.boost=boost;s.received=p.level().getGameTime();
    }
    public static boolean boosting(Player p){State s=states(p).get(p);var d=GeneratedMod.data(p);long age=s==null?11:p.level().getGameTime()-s.received;return s!=null&&s.world==p.level()&&age>=0&&age<=10&&s.boost&&s.input.lengthSqr()>.001&&d!=null&&d.wuxingxiantu$getAura()>=RealmFlightBalance.cost(d.wuxingxiantu$getRealm(),true,d.getMaxMana())/20;}
    private static void recoverFlyingSpeed(Player p){
        float speed=p.getAbilities().getFlyingSpeed();
        if(!Float.isFinite(speed)||speed<=0)p.getAbilities().setFlyingSpeed(.05F);
    }
    public static void release(Player p){
        states(p).remove(p);
        // A zero-speed abilities packet can outlive its controller state.
        // Recovery must work after entry/exit, a data-sync gap or a new player.
        recoverFlyingSpeed(p);
    }
    /** End return may respawn rather than fire a normal changed-dimension event. */
    public static void transition(ServerPlayer p){
        release(p);var d=GeneratedMod.data(p);
        if(!eligible(p)||d==null)return;
        p.getAbilities().flying=false;p.getAbilities().mayfly=d.wuxingxiantu$getRealm()>=5||d.wuxingxiantu$getAura()>=1.25F;
        p.getAbilities().setFlyingSpeed(.05F);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.hurtMarked=true;p.onUpdateAbilities();
    }
    public static void tick(Player p){
        recoverFlyingSpeed(p);
        if(!eligible(p)||!p.getAbilities().flying){release(p);return;}
        State s=state(p);var d=GeneratedMod.data(p);
        Vec3 input=p.level().getGameTime()-s.received>10?Vec3.ZERO:s.input;
        boolean boost=boosting(p);int realm=d.wuxingxiantu$getRealm(),stage=d.wuxingxiantu$getCultivationStage();
        s.boostWeight=RealmFlightBalance.approach(s.boostWeight,boost?1:0,1.0/12);
        double cruise=RealmFlightBalance.speed(realm,stage,false)/20;
        double speed=cruise+(RealmFlightBalance.speed(realm,stage,true)/20-cruise)*s.boostWeight,vertical=RealmFlightBalance.vertical(realm,stage)/20;
        Vec3 target=new Vec3(input.x*speed,input.y*vertical,input.z*speed);
        if(p instanceof ServerPlayer server)cn.blockforge.generated.generatedmod.flight.FlightTerrainStreaming.prefetch(server,p.position(),target);
        var prediction=p.getBoundingBox().deflate(.02).expandTowards(target.scale(3)).inflate(1);
        if(!cn.blockforge.generated.generatedmod.flight.FlightTerrainStreaming.available(p,prediction))target=target.scale(.35);
        else if(!p.level().noCollision(p,p.getBoundingBox().deflate(.02).expandTowards(target.scale(3))))target=target.scale(.25);
        double step=speed/(input.lengthSqr()<.001?8:boost?12:16);
        var velocity=RealmFlightBalance.advance(s.velocity.x,s.velocity.y,s.velocity.z,target.x,target.y,target.z,step);
        s.velocity=new Vec3(velocity.x(),velocity.y(),velocity.z());
        s.velocity=new Vec3(s.velocity.x,RealmFlightBalance.launchVertical(p.onGround(),input.y,s.velocity.y),s.velocity.z);
        s.velocity=cn.blockforge.generated.generatedmod.flight.FlightTerrainStreaming.limit(p,p.getBoundingBox(),s.velocity);
        // Abilities are synchronized and saved by other flight modes. Never
        // publish a zero acceleration here; the client suppresses the extra
        // vanilla force only around a physics tick it actually controls.
        p.setDeltaMovement(s.velocity);p.fallDistance=0;
        if(p instanceof ServerPlayer server){
            s.debt+=RealmFlightBalance.cost(realm,boost,d.getMaxMana())/20;
            if(s.debt>=.01){double paid=Math.min(d.wuxingxiantu$getAura(),s.debt);d.wuxingxiantu$setAura((float)Math.max(0,d.wuxingxiantu$getAura()-paid));s.debt-=paid;}
            if(realm<5&&d.wuxingxiantu$getAura()<1.25F){p.getAbilities().flying=false;p.getAbilities().mayfly=false;release(p);server.onUpdateAbilities();server.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.wuxingxiantu.flight_stopped"),true);}
            if(p.tickCount%20==0)GeneratedMod.sync(server);
        }
    }
    private RealmFlightControl(){}
}

