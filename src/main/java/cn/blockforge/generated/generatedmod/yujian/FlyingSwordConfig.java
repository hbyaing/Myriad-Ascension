package cn.blockforge.generated.generatedmod.yujian;

import net.minecraftforge.common.ForgeConfigSpec;
import cn.blockforge.generated.generatedmod.GeneratedMod;

/** Server config is synced by Forge; client presentation has its own local config. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class FlyingSwordConfig {
    public static final int MOUNT_TRANSITION_TICKS = 9;
    public static final int INPUT_TIMEOUT_TICKS = 12;
    public static final int HIT_COOLDOWN_TICKS = 12;
    private static final ForgeConfigSpec.Builder SERVER=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.DoubleValue SPEED_MULTIPLIER=SERVER.defineInRange("speedMultiplier",1.0,0.1,3.0);
    public static final ForgeConfigSpec.DoubleValue ROLL_DAMPING=SERVER.defineInRange("rollReleaseDampingV2",0.87,0.1,0.98);
    public static final ForgeConfigSpec.DoubleValue ROLL_START=SERVER.defineInRange("rollStartDegreesPerSecond",90.0,10.0,180.0);
    public static final ForgeConfigSpec.DoubleValue ROLL_LOW_MAX=SERVER.defineInRange("rollLowSpeedMaxDegreesPerSecond",150.0,90.0,300.0);
    public static final ForgeConfigSpec.DoubleValue ROLL_NORMAL_MAX=SERVER.defineInRange("rollNormalMaxDegreesPerSecond",330.0,180.0,400.0);
    public static final ForgeConfigSpec.DoubleValue SPIRAL_BONUS=SERVER.defineInRange("spiralBonus",0.65,0.0,1.0);
    public static final ForgeConfigSpec.DoubleValue ROLL_PULSE_BONUS=SERVER.defineInRange("rollPulseBonus",0.25,0.0,0.65);
    public static final ForgeConfigSpec.DoubleValue CRUISE_FRACTION=SERVER.defineInRange("rollCruiseFraction",0.85,0.75,1.0);
    public static final ForgeConfigSpec.IntValue ROLL_PULSE_TICKS=SERVER.defineInRange("rollPulseTicks",16,8,30);
    public static final ForgeConfigSpec.IntValue ROLL_PULSE_COOLDOWN=SERVER.defineInRange("rollPulseCooldown",40,30,200);
    public static final ForgeConfigSpec.DoubleValue ROLL_PULSE_ACCELERATION=SERVER.defineInRange("rollPulseAcceleration",1.5,1.0,2.0);
    public static final ForgeConfigSpec.DoubleValue CHARGE_SPEED=SERVER.defineInRange("chargeMinimumSpeed",0.65,0.1,3.0);
    public static final ForgeConfigSpec.DoubleValue CHARGE_ROLL=SERVER.defineInRange("chargeMinimumRollRadians",0.24,0.05,1.0);
    public static final ForgeConfigSpec.DoubleValue MAX_CHARGE_DAMAGE=SERVER.defineInRange("maximumChargeDamage",28.0,0.0,100.0);
    public static final ForgeConfigSpec.DoubleValue EXTREME_SPEED=SERVER.defineInRange("extremeFlightEventSpeed",2.0,0.1,Double.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue EXTREME_TICKS=SERVER.defineInRange("extremeFlightEventTicks",10,1,1200);
    private static final java.util.List<Profile> PROFILES=FlyingSwordRegistry.definitions().stream().map(Profile::new).toList();
    public static final ForgeConfigSpec SERVER_SPEC=SERVER.build();
    private static final ForgeConfigSpec.Builder CLIENT=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue DEBUG_OVERLAY=CLIENT.define("debugOverlay",false);
    public static final ForgeConfigSpec.EnumValue<HuahongSkin> HUAHONG_SKIN=CLIENT.defineEnum("huahongSkin",HuahongSkin.METEOR_PRISM);
    public static final ForgeConfigSpec.BooleanValue TRAILS=CLIENT.define("trails",true);
    public static final ForgeConfigSpec.DoubleValue ROLL_VISUAL_FULL=CLIENT.defineInRange("rollVisualFullDegreesPerSecond",150.0,90.0,330.0);
    public static final ForgeConfigSpec.DoubleValue WIND_VOLUME=CLIENT.defineInRange("flightWindVolume",0.3,0.0,1.0);
    public enum CameraMode { STABLE, FOLLOW_ROLL, FULL_LOCKED }
    public enum TrailPath { AUTO, NORMAL_TRAIL_RENDER_PATH, SHADER_COMPAT_TRAIL_RENDER_PATH, POST_SHADER_TRAIL_RENDER_PATH }
    public static final ForgeConfigSpec.EnumValue<ManualRoll.ReleaseMode> ROLL_RELEASE_MODE=CLIENT.defineEnum("rollReleaseMode",ManualRoll.ReleaseMode.HOLD_ANGLE);
    public static final ForgeConfigSpec.DoubleValue ROLL_SENSITIVITY=CLIENT.defineInRange("rollSensitivity",1.0,0.35,2.0);
    public static final ForgeConfigSpec.EnumValue<CameraMode> CAMERA_MODE=CLIENT.defineEnum("cameraModeV2",CameraMode.FOLLOW_ROLL);
    public static final ForgeConfigSpec.EnumValue<TrailPath> TRAIL_PATH=CLIENT.defineEnum("trailRenderPath",TrailPath.AUTO);
    public static final ForgeConfigSpec.DoubleValue CAMERA_TILT=CLIENT.defineInRange("stableCameraTiltDegrees",17.0,0.0,20.0);
    public static final ForgeConfigSpec.DoubleValue CAMERA_NORMAL=CLIENT.defineInRange("normalCameraDistance",6.0,2.0,14.0);
    public static final ForgeConfigSpec.DoubleValue CAMERA_SPRINT=CLIENT.defineInRange("sprintCameraDistance",7.5,2,14);
    public static final ForgeConfigSpec.DoubleValue CAMERA_SPIRAL=CLIENT.defineInRange("spiralCameraDistance",9.0,2.0,16.0);
    public static final ForgeConfigSpec.DoubleValue CAMERA_AHEAD=CLIENT.defineInRange("cameraLookAhead",1.5,0,3);
    public static final ForgeConfigSpec.DoubleValue FOV_BOOST=CLIENT.defineInRange("speedFovDegrees",5.0,0.0,10.0);
    public static final ForgeConfigSpec.DoubleValue SPIRAL_FOV=CLIENT.defineInRange("spiralFovDegrees",9.0,0.0,15.0);
    public static final ForgeConfigSpec.BooleanValue HIDE_HELD=CLIENT.define("hideHeldFlyingSwordInFirstPerson",true);
    public static final ForgeConfigSpec CLIENT_SPEC=CLIENT.build();

    private FlyingSwordConfig() {}

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void loaded(net.minecraftforge.fml.event.config.ModConfigEvent event){
        if(event.getConfig().getSpec()==SERVER_SPEC)for(var profile:PROFILES)FlyingSwordRegistry.register(profile.read());
    }

    private static final class Profile {
        final FlyingSwordDefinition base;
        final java.util.Map<String,ForgeConfigSpec.DoubleValue> values=new java.util.HashMap<>();
        Profile(FlyingSwordDefinition d){
            base=d;SERVER.push(d.itemId().getPath());
            add("handScale",d.handScale(),0.1,4);add("orbitScale",d.orbitScale(),0.1,4);
            add("flightScale",d.flightScale(),0.1,4);add("riderOffset",d.riderOffset(),0,1);
            SERVER.comment("Raw base speed before sword-tier scaling and the shared 1.5x ordinary flight gain; used by actual movement.");
            add("baseSpeed",d.baseSpeed(),0.05,2);
            SERVER.comment("Raw sprint speed before sword-tier scaling and the shared 1.5x ordinary flight gain; used by actual movement.");
            add("sprintSpeed",d.sprintSpeed(),0.05,3);
            add("acceleration",d.acceleration(),0.01,0.5);add("drag",d.drag(),0.5,0.999);
            add("turnRate",d.turnRate(),0.02,0.35);add("pitchResponse",d.pitchResponse(),0,1);
            add("bankingStrength",d.bankingStrength(),0,2);add("rollAccelerationDegreesPerSecondSquared",160,20,300);
            add("spiralMaxRollDegreesPerSecond",450,330,480);add("spiralSpeedMultiplier",d.spiralSpeedMultiplier(),0,1);
            add("impactDamage",d.impactDamage(),0,30);SERVER.pop();
        }
        void add(String name,double value,double min,double max){values.put(name,SERVER.defineInRange(name,value,min,max));}
        double value(String name){return values.get(name).get();}
        FlyingSwordDefinition read(){return new FlyingSwordDefinition(base.itemId(),
                (float)value("handScale"),(float)value("orbitScale"),(float)value("flightScale"),value("riderOffset"),
                value("baseSpeed"),value("sprintSpeed"),value("acceleration"),value("drag"),value("turnRate"),value("pitchResponse"),
                value("bankingStrength"),Math.toRadians(value("rollAccelerationDegreesPerSecondSquared"))/400,Math.toRadians(value("spiralMaxRollDegreesPerSecond"))/20,value("spiralSpeedMultiplier"),value("impactDamage"),
                base.modelYaw(),base.modelPitch(),base.modelRoll(),base.baked3d(),base.advanced());}
    }
}
