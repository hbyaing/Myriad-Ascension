package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.Network;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client input/camera bridge. It never decides the authoritative flight result. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID, value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class YujianClientController {
    private static boolean active;
    private static boolean previousDown;
    private static boolean previousRecenter;
    private static CameraType previousCamera=CameraType.FIRST_PERSON;
    private static float cameraRoll;
    private static float fovBoost;
    private static int lastImpact;
    private static float shake;
    private static double distance=6, collisionDistance=6;
    private static float cameraTargetRoll;
    private static boolean inputLeft, inputRight;
    private static int rollHoldTime;
    private static long lastFrame;
    private static ControlledSwordEntity lastPhasingCarrier;
    private static cn.blockforge.generated.generatedmod.network.packet.yujian.SwordFlightInputPacket lastInput;
    private static int lastInputTick;
    private static final java.lang.reflect.Method CAMERA_POSITION=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(
            net.minecraft.client.Camera.class,"m_90581_",net.minecraft.world.phys.Vec3.class);

    private YujianClientController() {}

    /** The authoritative phase may end before its buffered pose leaves a wall.
     * Keep only the mounted presentation clear until that pose is outside. */
    private static boolean phasingPresentation(ControlledSwordEntity sword){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.player.getVehicle()!=sword||!sword.flightRoot()){
            lastPhasingCarrier=null;return false;
        }
        if(sword.bufferedPhasing()){lastPhasingCarrier=sword;return true;}
        if(lastPhasingCarrier==sword&&mc.level.getBlockCollisions(mc.player,mc.player.getBoundingBox()).iterator().hasNext())return true;
        lastPhasingCarrier=null;return false;
    }

    @SubscribeEvent public static void phasingOverlay(net.minecraftforge.client.event.RenderBlockScreenEffectEvent event){
        var player=Minecraft.getInstance().player;
        if(event.getOverlayType()==net.minecraftforge.client.event.RenderBlockScreenEffectEvent.OverlayType.BLOCK
                &&player!=null&&player.getVehicle() instanceof ControlledSwordEntity sword&&phasingPresentation(sword))
            event.setCanceled(true);
    }

    public static void setActive(boolean value) {
        Minecraft client=Minecraft.getInstance();
        boolean mounted=client.player!=null && client.player.getVehicle() instanceof ControlledSwordEntity sword && sword.flightRoot();
        // Passenger tracking can arrive a tick after the state packet. Change camera only on the actual mount edge.
        if(value!=mounted && client.player!=null)return;
        if(!value)lastPhasingCarrier=null;
        if (value==active) return;
        if (value) {
            previousCamera=client.options.getCameraType();
            client.options.setCameraType(FlightCameraPreferences.type(cn.blockforge.generated.generatedmod.HudConfig.SWORD_FLIGHT_DEFAULT_CAMERA.get()));
            client.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(),false);
        } else {
            client.options.setCameraType(previousCamera);
            cameraRoll=0;fovBoost=0;shake=0;lastFrame=0;
            distance=FlyingSwordConfig.CAMERA_NORMAL.get();collisionDistance=distance;
        }
        active=value;
    }

    public static boolean active() { return active; }
    public static float cameraRoll() { return cameraRoll; }
    public static double cameraDistance(){return collisionDistance;}
    public static float speedFovBoost(){return fovBoost;}
    public static float cameraTargetRoll(){return cameraTargetRoll;}
    public static boolean rollInputLeft(){return inputLeft;}
    public static boolean rollInputRight(){return inputRight;}
    public static int rollHoldTime(){return rollHoldTime;}

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        Minecraft client=Minecraft.getInstance(); LocalPlayer player=client.player;
        if (player==null) {setActive(false);previousDown=false;previousRecenter=false;lastInput=null;return;}
        boolean input=client.screen==null && client.isWindowActive();
        boolean down=input && WuXingKeybinds.SWORD_FLIGHT.isDown();
        boolean edge=down&&!previousDown;
        boolean mounted=player.getVehicle() instanceof ControlledSwordEntity sword && sword.flightRoot();
        if (mounted && !active) setActive(true);
        if(!mounted && active)setActive(false);
        boolean forward=input && held(client,client.options.keyUp);
        boolean brake=!input || held(client,client.options.keyDown);
        boolean up=input && held(client,client.options.keyJump);
        boolean descend=input && held(client,client.options.keyShift);
        boolean left=input && held(client,client.options.keyLeft);
        boolean right=input && held(client,client.options.keyRight);
        inputLeft=left; inputRight=right;
        if (left || right) rollHoldTime++; else rollHoldTime=0;
        boolean sprint=input && held(client,client.options.keySprint) && forward;
        boolean recenter=input && WuXingKeybinds.RECENTER_FLYING_SWORD.isDown();
        boolean recenterEdge=recenter&&!previousRecenter;
        float yaw=player.getYRot(); float pitch=player.getXRot();
        if (down || mounted || edge || previousDown || recenterEdge) {
            var packet=new cn.blockforge.generated.generatedmod.network.packet.yujian.SwordFlightInputPacket(
                    down,edge,forward,brake,up,descend,left,right,sprint,recenterEdge,
                    FlyingSwordConfig.ROLL_SENSITIVITY.get().floatValue(),FlyingSwordConfig.ROLL_RELEASE_MODE.get().ordinal(),yaw,pitch);
            if(!packet.equals(lastInput)||player.tickCount-lastInputTick>=cn.blockforge.generated.generatedmod.yujian.YujianTuning.INPUT_HEARTBEAT){
                Network.CHANNEL.sendToServer(packet);lastInput=packet;lastInputTick=player.tickCount;
            }
        }
        previousDown=down;
        previousRecenter=recenter;
        if(mounted && player.getVehicle() instanceof ControlledSwordEntity sword){
            if(lastImpact!=sword.impact()){lastImpact=sword.impact();shake=1;}
        }else lastImpact=0;
        shake*=0.65F;
    }

    /** Sample the current action binding, including rebound modifier keys and mouse buttons.
     * Flight uses held actions rather than vanilla toggle/stale cached key states. */
    private static boolean held(Minecraft client,net.minecraft.client.KeyMapping binding) {
        if(binding.isUnbound()||!binding.getKeyConflictContext().isActive())return false;
        // NONE must not reject a movement binding merely because Shift/Ctrl is held.
        // Either modifier can itself be the player's rebound sprint/sneak key.
        if(binding.getKeyModifier()!=net.minecraftforge.client.settings.KeyModifier.NONE
                &&!binding.getKeyModifier().isActive(binding.getKeyConflictContext()))return false;
        var key=binding.getKey();long window=client.getWindow().getWindow();
        return switch(key.getType()) {
            case KEYSYM -> com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,key.getValue());
            case MOUSE -> org.lwjgl.glfw.GLFW.glfwGetMouseButton(window,key.getValue())==org.lwjgl.glfw.GLFW.GLFW_PRESS;
            default -> binding.isDown();
        };
    }

    @SubscribeEvent
    public static void movement(net.minecraftforge.client.event.MovementInputUpdateEvent event){
        if(event.getEntity().getVehicle() instanceof ControlledSwordEntity sword && sword.flightRoot()){
            var input=event.getInput();
            input.shiftKeyDown=false;input.jumping=false;input.forwardImpulse=0;input.leftImpulse=0;
            input.up=false;input.down=false;input.left=false;input.right=false;
        }
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active) return;
        var client=Minecraft.getInstance();
        if(client.player==null||client.level==null||!(client.player.getVehicle() instanceof ControlledSwordEntity sword))return;
        var frame=InterpolatedFlyingSwordTransform.get(sword,(float)event.getPartialTick());
        long now=System.nanoTime();
        double dt=lastFrame==0?1.0/60:Math.min(.1,(now-lastFrame)/1e9);lastFrame=now;
        double targetDistance=net.minecraft.util.Mth.lerp(frame.spiralFactor(),
                net.minecraft.util.Mth.lerp(frame.sprintFactor(),FlyingSwordConfig.CAMERA_NORMAL.get(),FlyingSwordConfig.CAMERA_SPRINT.get()),FlyingSwordConfig.CAMERA_SPIRAL.get());
        distance=smooth(distance,targetDistance,dt,targetDistance>distance?.4:.65);
        double wantedFov=net.minecraft.util.Mth.lerp(frame.spiralFactor(),frame.sprintFactor()*FlyingSwordConfig.FOV_BOOST.get(),FlyingSwordConfig.SPIRAL_FOV.get());
        wantedFov=Math.min(cn.blockforge.generated.generatedmod.yujian.YujianTuning.MAX_FOV_BOOST,
                wantedFov+(sword.advancedPhase().burst()?sword.advancedPhase().trailIntensity*3:0));
        fovBoost=(float)smooth(fovBoost,wantedFov,dt,wantedFov>fovBoost?.4:.65);
        double tilt=net.minecraft.util.Mth.clamp(frame.angular()*20*net.minecraft.util.Mth.RAD_TO_DEG/330,-1,1)*FlyingSwordConfig.CAMERA_TILT.get();
        var mode=FlyingSwordConfig.CAMERA_MODE.get();
        // 手动角度保持连续，可超过 360°；不归一化，也不另行向水平回正。
        cameraTargetRoll=switch(mode){
            case STABLE -> (float)tilt;
            case FOLLOW_ROLL -> frame.manualRoll();
            case FULL_LOCKED -> frame.roll();
        };
        cameraRoll=switch(mode){
            case STABLE -> (float)smooth(cameraRoll,tilt,dt,.25);
            case FOLLOW_ROLL -> frame.manualRoll();
            case FULL_LOCKED -> frame.roll();
        };
        if(mode==FlyingSwordConfig.CameraMode.FULL_LOCKED){
            event.setYaw(frame.yaw());
            event.setPitch(frame.pitch());
        }
        event.setRoll(cameraRoll);
        var camera=event.getCamera();
        var start=frame.position().add(0,sword.riderOffset()+client.player.getEyeHeight()+.3*frame.spiralFactor(),0);
        boolean first=client.options.getCameraType().isFirstPerson();
        boolean phasing=phasingPresentation(sword);
        if(first){
            // Eyes belong to the standing deck frame, not the world vertical axis.
            // No third-person distance/spiral lift or filtered bank in this branch.
            start=frame.local(0,sword.riderOffset()+client.player.getEyeHeight(),0);
            cameraRoll=frame.roll();event.setRoll(cameraRoll);
        }
        if(!first)start=start.add(frame.forward().scale(FlyingSwordConfig.CAMERA_AHEAD.get()*(.35*frame.sprintFactor()+.65*frame.spiralFactor())));
        var direction=net.minecraft.world.phys.Vec3.directionFromRotation(event.getPitch(),event.getYaw()).scale(-1);
        double allowed=first?0:distance;
        for(int i=0;!first&&!phasing&&i<8;i++){
            var corner=start.add((i&1)==0?-0.15:0.15,(i&2)==0?-0.15:0.15,(i&4)==0?-0.15:0.15);
            var hit=client.level.clip(new net.minecraft.world.level.ClipContext(corner,corner.add(direction.scale(allowed)),
                    net.minecraft.world.level.ClipContext.Block.VISUAL,net.minecraft.world.level.ClipContext.Fluid.NONE,client.player));
            if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)allowed=Math.min(allowed,Math.max(0,hit.getLocation().distanceTo(corner)-0.05));
        }
        collisionDistance=!phasing&&allowed<collisionDistance?allowed:smooth(collisionDistance,allowed,dt,phasing?.25:.55);
        try{CAMERA_POSITION.invoke(camera,start.add(direction.scale(first?0:collisionDistance)));}
        catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot position sword flight camera",ex);}
    }

    private static double smooth(double current,double target,double dt,double seconds){return current+(target-current)*(1-Math.exp(-dt*3/seconds));}

    public static boolean isBoundSword(net.minecraft.world.item.ItemStack stack,ControlledSwordEntity sword){
        var a=stack.getTag();var b=sword.display().getTag();
        return a!=null&&b!=null&&a.hasUUID("WuxingYujianBinding")&&b.hasUUID("WuxingYujianBinding")
                &&a.getUUID("WuxingYujianBinding").equals(b.getUUID("WuxingYujianBinding"));
    }
    @SubscribeEvent public static void hand(net.minecraftforge.client.event.RenderHandEvent event){
        var player=Minecraft.getInstance().player;
        if(FlyingSwordConfig.HIDE_HELD.get()&&player!=null&&player.getVehicle() instanceof ControlledSwordEntity sword
                &&sword.flightRoot()&&isBoundSword(event.getItemStack(),sword))event.setCanceled(true);
    }

    @SubscribeEvent
    public static void cameraFov(ViewportEvent.ComputeFov event) {
        Minecraft client=Minecraft.getInstance();
        if (!active || client.player==null || !(client.player.getVehicle() instanceof ControlledSwordEntity sword)) return;
        event.setFOV(event.getFOV()+fovBoost*client.options.fovEffectScale().get());
    }
}
