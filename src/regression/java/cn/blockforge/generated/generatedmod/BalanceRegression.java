package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.balance.*;
import cn.blockforge.generated.generatedmod.client.HeartForgeLayout;

/** Dependency-free behavioral regression executable; failures produce a nonzero Gradle task. */
public final class BalanceRegression {
    private static int checks;
    private static void require(boolean condition,String message) {
        checks++;if(!condition)throw new AssertionError(message);
    }
    private static void near(double expected,double actual,String message) {
        require(Math.abs(expected-actual)<.001,message+": expected "+expected+", got "+actual);
    }
    public static void main(String[] args) {
        AlchemySimulationRegression.run();
        ScreenFitRegression.run();
        var flow=new cn.blockforge.generated.generatedmod.client.FlightFlowMotion();
        for(int tick=0;tick<20000;tick++){
            double phase=flow.phase(1),intensity=flow.strength(1);
            flow.tick(tick%60<30?.06:1.2,tick%90<45);
            near(phase,flow.phase(0),"Flow speed changes preserve phase across tick boundaries");
            near(intensity,flow.strength(0),"Flow strength is continuous across tick boundaries");
            double step=flow.phase(1)-phase;
            require(step>=.04&&step<=.196,"Acceleration and braking cannot jump the flow position");
            require(Math.abs(flow.strength(1)-intensity)<=.35,"Strength transition is bounded");
        }
        for(int tick=0;tick<30;tick++)flow.tick(tick%2==0?.018:.024,false);
        require(flow.strength(1)>.44,"Threshold jitter does not flicker after movement starts");
        for(int tick=0;tick<30;tick++)flow.tick(0,false);
        require(flow.strength(1)==0,"Stationary hover has no persistent effect");
        flow.reset();require(flow.phase(1)==0&&flow.strength(1)==0,"Disconnect and flight exit clear flow state");
        near(20,XuanlingAnvilBalance.manaPerLevel(),"Anvil prices use Foundation mana");
        near(150,XuanlingAnvilBalance.repairCost(),"Anvil restoration price");
        near(780,XuanlingAnvilBalance.operationCost(39),"Gold Core can pay the largest vanilla survival quote");
        require(XuanlingAnvilBalance.operationCost(Integer.MAX_VALUE)==Integer.MAX_VALUE,"Anvil prices must not overflow");
        require(XuanlingAnvilBalance.operationCost(-1)==0,"Negative prices must not create mana");
        for(float luck:new float[]{0,1,3,5,100,Float.NaN}) {
            int[] grades=new int[4];for(int i=0;i<10000;i++)grades[ElementMaterialDrops.grade(luck,(i+.5)/10000)]++;
            require(grades[0]>5000&&grades[3]<=500,"luck must not force perfect elemental drops");
            require(grades[1]>0&&grades[2]>0&&grades[3]>0,"all elemental grades remain possible");
        }
        TribulationPlanRegression.run();
        RealmFlightRegression.run();
        RealmFlightCameraRegression.run();
        RealmFlightLevelRegression.run();
        RealmFlightSelectionRegression.run();
        SwordTrailPhaseRegression.run();
        HuahongFlightRegression.run();
        RealmMeteorPathRegression.run();
        RealmMeteorJoinRegression.run();
        RealmMeteorTuningRegression.run();
        RealmMeteorFlowRegression.run();
        RealmMeteorVertexRingRegression.run();
        BossCombatRegression.run();
        ShieldFractureGeometryRegression.run();
        DamageDomainRegression.run();
        near(1,DivineArtCosts.soulDistanceMultiplier(0),"Soul upkeep starts at base cost");
        near(2,DivineArtCosts.soulDistanceMultiplier(64),"Soul upkeep doubles at 64 blocks");
        near(3,DivineArtCosts.soulDistanceMultiplier(128),"Soul upkeep triples at 128 blocks");
        require(DivineArtCosts.maintenance("yuan_shen_chu_qiao",128)>DivineArtCosts.maintenance("yuan_shen_chu_qiao",64),
                "Soul upkeep must rise with distance");
        require(DivineArtCosts.maintenance("yuan_shen_chu_qiao",Double.NaN)==Integer.MAX_VALUE,
                "Invalid soul distances must not produce free upkeep");
        near(DivineArtCosts.perSecond("yi_hun_ji_po"),DivineArtCosts.maintenance("yi_hun_ji_po",256),
                "Possession upkeep must keep its original cost");
        near(10000,HeartDemonBalance.phaseOneHealth(1000),"Heart demon first phase mirrors HP tenfold");
        near(20,HeartDemonBalance.phaseOneHealth(Double.NaN),"Invalid mirrored health uses a finite minimum");
        near(100,HeartDemonBalance.mirror(10,0,HeartDemonBalance.ATTACK_MULTIPLIER),"Heart demon independent attack multiplier");
        near(120,HeartMethodBalance.cultivationReward("nine_turns",false,100),"Nine Turns cultivation reward");
        near(.40,HeartMethodBalance.breakthroughFraction("nine_turns"),"Nine Turns breakthrough cost");
        near(100,HeartMethodBalance.cultivationReward("gathering",false,100),"Gathering requires meditation");
        near(115,HeartMethodBalance.cultivationReward("gathering",true,100),"Gathering meditation cultivation");
        double carry=0;long earlyGains=0;
        for(int tick=0;tick<20;tick++) {
            var reward=HeartMethodBalance.splitCultivationReward("gathering",true,1,carry);
            earlyGains+=reward.amount();carry=reward.remainder();
        }
        near(23,earlyGains,"Early meditation must retain Gathering's fractional bonus");
        near(.92,HeartMethodBalance.damageTakenMultiplier("xuanshui"),"Xuanshui damage reduction preserved");
        near(1.1,HeartMethodBalance.manaMultiplier("yuan_hun"),"Yuan Hun maximum mana");
        near(1.0,HeartMethodBalance.manaMultiplier(""),"Inactive method has no mana bonus");
        require(HeartMethodBalance.cultivationReward("nine_turns",false,Long.MAX_VALUE)==Long.MAX_VALUE,
                "Heart-method reward must saturate instead of overflowing");
        near(1600,RealmBalance.health(6),"Fusion health must exceed vanilla 1024 cap");
        near(4000,RealmBalance.health(8),"Earth immortal health");
        near(4800,RealmBalance.health(8)*RealmBalance.stage(3),"Peak-stage health");
        near(40,RealmBalance.health(-1),"Invalid low realm");
        near(4000,RealmBalance.health(Integer.MAX_VALUE),"Invalid high realm");
        near(100000000,RealmBalance.cultivation(8,3),"Peak cultivation cap");
        near(1562500,ManaBalance.maximum(8,25,1.25F),"Aura and physique bonuses compose");
        near(95,ManaBalance.maximum(0,-5,1),"Damaged meridian penalty preserved");
        near(0,ManaBalance.sanitizeMaximum(8,Float.NaN),"NaN maximum rejected");
        near(0,ManaBalance.clamp(Float.POSITIVE_INFINITY,100),"Infinite current mana rejected");
        near(100,ManaBalance.clamp(101,100),"Recovery cannot exceed capacity");
        near(0,ManaBalance.clamp(-1,100),"Current mana cannot be negative");
        near(8000,ManaBalance.regenerationCap(1000000,false),"Standing regeneration ceiling");
        near(40000,ManaBalance.regenerationCap(1000000,true),"Meditation regeneration ceiling");
        near(3200,DivineArtCosts.activation("jin_dou_yun"),"Cloud unlock cost is ten percent");
        near(6400,DivineArtCosts.activation("ri_yue_ting_lun"),"Canonical time-stop ID cost");
        for(int realm=0;realm<8;realm++){
            int count=TribulationBalance.strikes(realm);double total=0;
            for(int strike=1;strike<=count;strike++)total+=TribulationBalance.damage(RealmBalance.health(realm),count,strike);
            near(2.3,total/RealmBalance.health(realm),"Tribulation health budget");
        }
        near(0,HeartSwordTimeline.reveal(0,0),"Summon starts invisible");
        near(1,HeartSwordTimeline.reveal(0,1),"Summon ends complete");
        near(0,HeartSwordTimeline.reveal(1,1),"Disperse ends invisible");
        near(0,HeartSwordTimeline.reveal(0,.1F),"Palm nucleus precedes the solid sword");
        near(0,HeartSwordTimeline.reveal(1,.9F),"Palm nucleus outlasts the dissolving sword");
        near(21,HeartSwordTimeline.duration(2),"Forge timing");
        near(13,HeartSwordTimeline.duration(3),"Unforge timing");
        float last=0;
        for(int i=0;i<=100;i++) {
            float p=i/100F;
            near(HeartSwordTimeline.sweep(2,p),HeartSwordTimeline.sweep(3,1-p),"Unforge reverses forge path");
            float next=HeartSwordTimeline.reveal(0,p);
            require(next>=last,"Summon front never moves backward");last=next;
        }
        near(0,HeartSwordTimeline.poseWeight(0),"Animation enters from vanilla pose");
        near(0,HeartSwordTimeline.poseWeight(1),"Animation returns to vanilla pose");
        var firstGrip=HeartForgeLayout.firstPerson(0,0);
        var thirdGrip=HeartForgeLayout.thirdPerson(0,0);
        require(firstGrip.x()>0,"Holding right hand belongs on the right of the first-person image");
        require(thirdGrip.x()<0,"Holding grip belongs to the player's physical right arm");
        for(boolean slim:new boolean[]{false,true}) {
            float shoulderX=slim?5.5F/16:6F/16,shoulderY=slim?2.5F/16:2F/16;
            var rightShoulder=new HeartForgeLayout.Point(-shoulderX,shoulderY,0);
            var rightElbow=HeartForgeLayout.elbow(rightShoulder,thirdGrip,true);
            near(HeartForgeLayout.ARM_HALF,rightElbow.subtract(rightShoulder).length(),"Holding upper arm keeps its length");
            near(HeartForgeLayout.ARM_HALF,thirdGrip.subtract(rightElbow).length(),"Holding forearm keeps its length");
            near(thirdGrip.y(),rightElbow.y(),"Holding forearm is horizontal");
            for(int i=0;i<=20;i++) {
                float along=(.15F+.73F*i/20)*HeartForgeLayout.THIRD_PERSON_SCALE;
                var leftShoulder=new HeartForgeLayout.Point(shoulderX,shoulderY,0);
                var wrist=HeartForgeLayout.thirdPerson(along,HeartForgeLayout.PALM_CLEARANCE);
                var elbow=HeartForgeLayout.elbow(leftShoulder,wrist,false);
                near(HeartForgeLayout.ARM_HALF,elbow.subtract(leftShoulder).length(),"Stroking upper arm stays joined");
                near(HeartForgeLayout.ARM_HALF,wrist.subtract(elbow).length(),"Stroking wrist reaches the blade without stretching");
                var first=HeartForgeLayout.firstPerson(along,0);
                require(first.x()<firstGrip.x(),"Blade and stroking hand extend left from the right-hand grip");
                near(firstGrip.y(),first.y(),"First-person sword stays horizontal throughout the stroke");
            }
        }
        System.out.println("PASS: "+checks+" balance/animation boundary checks");
    }
}
