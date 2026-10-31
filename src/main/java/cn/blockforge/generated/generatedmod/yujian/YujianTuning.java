package cn.blockforge.generated.generatedmod.yujian;

import java.util.Set;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

/** Shared movement profiles and collision work budgets, without a fixed flight-speed cap. */
public final class YujianTuning {
    /** Raw per-sword config defaults; installed values remain meaningful. */
    public static final double BASE_FLIGHT_SPEED=0.58D*3.0D;
    public static final double SPRINT_FLIGHT_SPEED=BASE_FLIGHT_SPEED*1.5D;
    /** Raise actual ordinary cruise/sprint by 50%, including existing worlds' configs. */
    public static final double NORMAL_FLIGHT_GAIN=1.5D;
    public static final double SWEEP_STEP=0.2;
    // At config maxima, Tianque Dunxu plus rainbow reaches 120 blocks/tick:
    // base 2 * Dunxu 4 * tier 2.5 * global 3 * rainbow 2. Keep at least
    // 600 exact 0.2-block steps available to prediction/reference consumers.
    // The actual section sweep independently retains its 0.2-block fine checks.
    public static final int MAX_SWEEP_STEPS=768, MAX_COLLISION_BLOCKS=512, SAFE_HISTORY=48;
    public static final int DUNXU_TICKS=30, DUNXU_PRE_TICKS=2, DUNXU_EXIT_TICKS=4;
    public static final int DUNXU_COOLDOWN=40;
    public static final double DUNXU_ENTRY_SPEED=0.70;
    public static final int DUNXU_PREDICT_TICKS=4;
    public static final double DUNXU_ENTRY_AURA_FRACTION=0.01;
    public static final double DUNXU_TICK_AURA_FRACTION=0.015/20.0;
    public static final double HOVER_HORIZONTAL=0.12, HOVER_VERTICAL=0.07, HOVER_DAMPING=0.65;
    public static final double DECELERATION=0.12;
    public static final int INPUT_HEARTBEAT=4, DIAGNOSTIC_INTERVAL=5;
    public static final float MAX_FOV_BOOST=15;
    public record Profile(Set<YujianPhase> capabilities, boolean jinghong, double cruiseMultiplier, double jiyuMultiplier,
                          double dunxuMultiplier, double efficiency,
                          double jiyuAcceleration, double burstAcceleration, double fastHandling) {
        public Profile { capabilities=Set.copyOf(capabilities); }
        public boolean supports(YujianPhase phase) { return capabilities.contains(phase); }
        public int lingkongLevel(FlyingSwordDefinition sword) {
            String path=sword.itemId().getPath();
            if(path.endsWith("_sword"))path=path.substring(0,path.length()-6);
            // Registry names were renamed while SwordProfile's material IDs
            // stayed stable. Looking up the new name silently erased every tier.
            String material=switch(path) {
                case "mingjue" -> "azure_steel";
                case "qingming" -> "tiangang";
                case "jixiao" -> "big_dipper";
                case "wuxiang" -> "demon_slayer";
                case "guiyuan" -> "pure_yang";
                case "tianque" -> "xuanyuan";
                default -> path;
            };
            return SwordProfile.of(material).lingkongLevel();
        }
        public double maxSpeed(YujianPhase phase, FlyingSwordDefinition sword) {
            double lingkong=1.0D+0.25D*lingkongLevel(sword);
            double speed=switch(phase) {
                case JIYU -> sword.sprintSpeed()*NORMAL_FLIGHT_GAIN*jiyuMultiplier*lingkong;
                case DUNXU -> sword.baseSpeed()*dunxuMultiplier*lingkong;
                case LINGKONG -> sword.baseSpeed()*NORMAL_FLIGHT_GAIN*lingkong;
                default -> sword.baseSpeed()*NORMAL_FLIGHT_GAIN*cruiseMultiplier*lingkong;
            };
            return Math.max(0.01, speed*FlyingSwordConfig.SPEED_MULTIPLIER.get());
        }
        public double cruiseAcceleration(FlyingSwordDefinition sword) {
            return sword.acceleration()*NORMAL_FLIGHT_GAIN*(1+.25*lingkongLevel(sword))*FlyingSwordConfig.SPEED_MULTIPLIER.get();
        }
        public float costPerTick(YujianPhase phase) {
            double perSecond=switch(phase) {
                case JIYU -> 7; case LINGKONG -> 3; case DUNXU -> 0; default -> 2;
            };
            return (float)(perSecond*efficiency/20.0);
        }
        /** Dunxu entry and upkeep are percentage based and charged by the controller. */
        public float startup(YujianPhase phase) { return 0.0F; }
        public float jinghongCost() { return (float)(16*efficiency); }
    }
    private static Profile profile(double efficiency, boolean jinghong, YujianPhase... phases) {
        return new Profile(Set.of(phases),jinghong,1,1,4,efficiency,0.14,0.26,0.65);
    }
    public static final Profile NONE=profile(1,false);
    private static final Map<String,Profile> PROFILES=Map.of(
            "dark_iron_sword",profile(1,false,YujianPhase.BASIC,YujianPhase.ACCELERATING,YujianPhase.JIYU),
            "mingjue_sword",profile(1,false,YujianPhase.BASIC,YujianPhase.ACCELERATING,YujianPhase.JIYU,YujianPhase.LINGKONG),
            "qingming_sword",profile(1,true,YujianPhase.BASIC,YujianPhase.ACCELERATING,YujianPhase.JIYU,YujianPhase.LINGKONG),
            "jixiao_sword",profile(0.95,true,YujianPhase.BASIC,YujianPhase.ACCELERATING,YujianPhase.JIYU,YujianPhase.LINGKONG),
            "wuxiang_sword",profile(0.95,true,YujianPhase.values()),
            "guiyuan_sword",profile(0.90,true,YujianPhase.values()),
            "tianque_sword",profile(0.85,true,YujianPhase.values()));
    public static Profile forSword(String id) { return PROFILES.getOrDefault(id,NONE); }
    public static Vec3 finiteVelocity(Vec3 velocity) {
        if(!Double.isFinite(velocity.x)||!Double.isFinite(velocity.y)||!Double.isFinite(velocity.z))return Vec3.ZERO;
        return velocity;
    }
    private YujianTuning() {}
}
