package cn.blockforge.generated.generatedmod.client.realmflight;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.HudConfig;
import cn.blockforge.generated.generatedmod.client.HeartSwordClientEffects;
import cn.blockforge.generated.generatedmod.client.JindouCloudRenderState;
import cn.blockforge.generated.generatedmod.realmflight.*;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicit final pose integration; no global HumanoidModel Mixin or copied reference code. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class RealmFlightVisuals {
    private static final Map<UUID,FlightVisualDynamics> STATES=new HashMap<>();
    private static final Set<UUID> ACTIVE=new HashSet<>();
    private static final Map<UUID,Double> CRUISE_SPEEDS=new HashMap<>();
    private static final Map<UUID,Integer> RENDERING=new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static Player modePlayer;
    private static boolean lastMode;
    private static int lastVisualRealm, lastActualRealm;
    public static void sendMode() {
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getConnection()==null)return;
        lastMode=HudConfig.REALM_FLIGHT_ANIMATION.get();modePlayer=mc.player;
        lastVisualRealm=HudConfig.REALM_FLIGHT_VISUAL_REALM.get();
        var data=GeneratedMod.data(mc.player);lastActualRealm=data==null?0:data.wuxingxiantu$getRealm();
        // Mirror only the cosmetic preference locally; flight authorization is untouched.
        mc.player.getPersistentData().putBoolean(RealmFlightModePacket.VANILLA,!lastMode);
        mc.player.getPersistentData().putInt(RealmFlightModePacket.VISUAL_REALM,RealmFlightSelection.sanitizePreference(lastActualRealm,lastVisualRealm));
        cn.blockforge.generated.generatedmod.Network.CHANNEL.sendToServer(new RealmFlightModePacket(lastMode,lastVisualRealm));
        if(!lastMode)STATES.remove(mc.player.getUUID());
    }
    private static boolean vanillaChoice(Player p){return p==Minecraft.getInstance().player&&!HudConfig.REALM_FLIGHT_ANIMATION.get();}
    private static void checkLevel() {
        var current=Minecraft.getInstance().level;
        if(level!=current){STATES.clear();ACTIVE.clear();CRUISE_SPEEDS.clear();RENDERING.clear();level=current;modePlayer=null;}
    }
    public static void receive(RealmFlightVisualPacket p) {
        checkLevel();RealmFlightEffects.accept(p);
        if(p.active()){
            ACTIVE.add(p.player());
            CRUISE_SPEEDS.put(p.player(),RealmFlightBalance.speed(p.realm(),p.stage(),false)/20*FlightVisualDynamics.VELOCITY_RETENTION);
        }else{ACTIVE.remove(p.player());CRUISE_SPEEDS.remove(p.player());}
    }
    @SubscribeEvent public static void leave(net.minecraftforge.event.entity.EntityLeaveLevelEvent e) {
        if(e.getLevel().isClientSide()&&e.getEntity() instanceof Player p){ACTIVE.remove(p.getUUID());CRUISE_SPEEDS.remove(p.getUUID());STATES.remove(p.getUUID());}
    }
    private static boolean excluded(Player p) {
        return !p.isAlive()||p.isPassenger()||p.isSpectator()||p.isCreative()||p.isFallFlying()||p.isInWater()
                ||JindouCloudRenderState.isActive(p)||HeartSwordClientEffects.action(p)>=2;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        checkLevel();
        if(mc.player==null)modePlayer=null;
        else {
            var data=GeneratedMod.data(mc.player);int actual=data==null?0:data.wuxingxiantu$getRealm();
            if(modePlayer!=mc.player||lastMode!=HudConfig.REALM_FLIGHT_ANIMATION.get()
                    ||lastVisualRealm!=HudConfig.REALM_FLIGHT_VISUAL_REALM.get()||lastActualRealm!=actual)sendMode();
        }
        if(level==null||mc.isPaused())return;
        Set<UUID> present=new HashSet<>();
        for(var p:level.players()) {
            present.add(p.getUUID());
            if(excluded(p)||vanillaChoice(p)){STATES.remove(p.getUUID());continue;}
            boolean active=ACTIVE.contains(p.getUUID()) || p==mc.player&&p.getAbilities().flying
                    &&p.hasEffect(cn.blockforge.generated.generatedmod.ModEffects.REALM_FLIGHT.get())&&!vanillaChoice(p);
            var s=STATES.get(p.getUUID());
            if(!active&&s==null)continue;
            if(s==null){s=new FlightVisualDynamics();STATES.put(p.getUUID(),s);}
            var motion=p==mc.player?p.getDeltaMovement():new net.minecraft.world.phys.Vec3(p.getX()-p.xOld,p.getY()-p.yOld,p.getZ()-p.zOld);
            double cruise=CRUISE_SPEEDS.getOrDefault(p.getUUID(),FlightVisualDynamics.DEFAULT_CRUISE_SPEED);
            if(p==mc.player){
                var data=GeneratedMod.data(p);
                if(data!=null)cruise=RealmFlightBalance.speed(data.wuxingxiantu$getRealm(),data.wuxingxiantu$getCultivationStage(),false)/20*FlightVisualDynamics.VELOCITY_RETENTION;
            }
            s.tick(active,motion.x,motion.y,motion.z,p.yBodyRot,cruise);
            if(!active&&s.amount==0&&s.previousAmount==0)STATES.remove(p.getUUID());
        }
        STATES.keySet().retainAll(present);
    }
    public static float forwardShellWeight(Player player,float partial) {
        if(excluded(player)||vanillaChoice(player))return 0;
        var state=STATES.get(player.getUUID());if(state==null)return 0;
        var pose=state.pose(partial,HudConfig.REALM_FLIGHT_HORIZONTAL.get());
        return (float)cn.blockforge.generated.generatedmod.realmflight.RealmMeteorTuning.smooth((pose.leanPitch()-25)/40);
    }
    /** True only while this player's RenderPlayer root has the realm-flight transform. */
    public static boolean bodyTransformActive(Player player) {
        return player != null && RENDERING.containsKey(player.getUUID());
    }
    /** Local cape pitch contribution for the same interpolated flight frame as the body. */
    public static float capeLift(Player player,float partial) {
        var state=player==null?null:STATES.get(player.getUUID());
        if(state==null)return 6F;
        var flight=state.pose(partial,HudConfig.REALM_FLIGHT_HORIZONTAL.get());
        return Mth.clamp(6F+flight.travel()*28F-flight.trajectoryPitch()*.20F,-6F,72F);
    }
    /** Local cape bank contribution for turns in the realm-flight frame. */
    public static float capeBank(Player player,float partial) {
        var state=player==null?null:STATES.get(player.getUUID());
        if(state==null)return 0F;
        return Mth.clamp(state.pose(partial,HudConfig.REALM_FLIGHT_HORIZONTAL.get()).roll()*.35F,-24F,24F);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void before(RenderPlayerEvent.Pre e) {
        if(cn.blockforge.generated.generatedmod.ParalysisControl.blocked(e.getEntity())||e.isCanceled()||excluded(e.getEntity())||vanillaChoice(e.getEntity()))return;
        var s=STATES.get(e.getEntity().getUUID());if(s==null)return;
        RENDERING.merge(e.getEntity().getUUID(),1,Integer::sum);
        var pose=e.getPoseStack();pose.pushPose();
        applyBodyTransform(e.getEntity(),e.getPartialTick(),pose);
    }
    /** Same final flight root for attachments when the first-person body is not rendered. */
    public static void applyBodyTransform(Player player,float partial,com.mojang.blaze3d.vertex.PoseStack pose){
        if(cn.blockforge.generated.generatedmod.ParalysisControl.blocked(player)||excluded(player)||vanillaChoice(player))return;
        var s=STATES.get(player.getUUID());if(s==null)return;
        var flight=s.pose(partial,HudConfig.REALM_FLIGHT_HORIZONTAL.get());
        float bodyYaw=Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot);
        float yaw=bodyYaw+s.yawOffset(partial)*flight.travel();
        // Pre runs before LivingEntityRenderer's 180-bodyYaw rotation. Its bodyYaw must
        // be compensated on the right, while the velocity azimuth goes on the left.
        // Rotate around the pelvis in world units so the player stays inside its wake.
        pose.translate(0,FlightVisualDynamics.PELVIS_HEIGHT,0);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        // Roll about the travel axis after its elevation rather than tipping the torso
        // forward/backward around a world vertical-plane axis during turns.
        pose.mulPose(Axis.XP.rotationDegrees(flight.trajectoryPitch()));
        pose.mulPose(Axis.ZP.rotationDegrees(flight.roll()));
        pose.mulPose(Axis.XP.rotationDegrees(flight.leanPitch()));
        pose.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        pose.translate(0,-FlightVisualDynamics.PELVIS_HEIGHT+Math.sin((player.tickCount+partial)*.09)*.025*flight.amount()*(1-flight.travel()),0);
    }
    @SubscribeEvent public static void after(RenderPlayerEvent.Post e) {
        var id=e.getEntity().getUUID();var count=RENDERING.get(id);if(count==null)return;
        if(count==1)RENDERING.remove(id);else RENDERING.put(id,count-1);
        e.getPoseStack().popPose();
    }
    public static void pose(PlayerModel<AbstractClientPlayer> model,AbstractClientPlayer p,float partial) {
        if(!RENDERING.containsKey(p.getUUID())||excluded(p)||vanillaChoice(p))return;
        var s=STATES.get(p.getUUID());if(s==null)return;
        var flight=s.pose(partial,HudConfig.REALM_FLIGHT_HORIZONTAL.get());float weight=flight.amount();
        // Arms retain vanilla combat/use poses; the root still follows the flight trajectory.
        if(!p.isUsingItem()&&!p.swinging&&model.attackTime<=0&&HeartSwordClientEffects.action(p)<0) {
            model.rightArm.xRot=Mth.lerp(weight,model.rightArm.xRot,flight.armSweep());
            model.leftArm.xRot=Mth.lerp(weight,model.leftArm.xRot,flight.armSweep());
            model.rightArm.yRot=Mth.lerp(weight,model.rightArm.yRot,0);
            model.leftArm.yRot=Mth.lerp(weight,model.leftArm.yRot,0);
            model.rightArm.zRot=Mth.lerp(weight,model.rightArm.zRot,flight.rightArmSpread());
            model.leftArm.zRot=Mth.lerp(weight,model.leftArm.zRot,-flight.leftArmSpread());
            model.body.xRot=Mth.lerp(weight,model.body.xRot,0);
            model.body.yRot=Mth.lerp(weight,model.body.yRot,0);
            model.body.zRot=Mth.lerp(weight,model.body.zRot,0);
        }
        // Vanilla leg pivots already meet at the centre: remove walking and splay,
        // keeping both legs straight rather than bending the whole pair backward.
        model.rightLeg.xRot=Mth.lerp(weight,model.rightLeg.xRot,0);
        model.leftLeg.xRot=Mth.lerp(weight,model.leftLeg.xRot,0);
        model.rightLeg.yRot=Mth.lerp(weight,model.rightLeg.yRot,0);
        model.leftLeg.yRot=Mth.lerp(weight,model.leftLeg.yRot,0);
        model.rightLeg.zRot=Mth.lerp(weight,model.rightLeg.zRot,0);
        model.leftLeg.zRot=Mth.lerp(weight,model.leftLeg.zRot,0);
        model.head.xRot=blendAngle(flight.travel(),model.head.xRot,flight.headPitch());
        model.head.yRot=blendAngle(flight.travel(),model.head.yRot,flight.headYaw());
        model.head.zRot=blendAngle(flight.travel(),model.head.zRot,flight.headRoll());
        model.rightSleeve.copyFrom(model.rightArm);model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);model.leftPants.copyFrom(model.leftLeg);
        model.hat.copyFrom(model.head);model.jacket.copyFrom(model.body);
    }
    private static float blendAngle(float weight,float from,float to){
        float delta=(float)Math.atan2(Math.sin(to-from),Math.cos(to-from));
        return from+weight*delta;
    }
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles e) {
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||e.getCamera().getEntity()!=p||excluded(p)||!HudConfig.REALM_FLIGHT_ANIMATION.get()
                ||!HudConfig.REALM_FLIGHT_CAMERA.get())return;
        var s=STATES.get(p.getUUID());if(s==null)return;
        float partial=(float)e.getPartialTick();
        e.setRoll(e.getRoll()+s.bank(partial)*s.amount(partial)*HudConfig.REALM_FLIGHT_CAMERA_SCALE.get().floatValue());
    }
    private RealmFlightVisuals(){}
}
