package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("wuxingxiantu")
@PrefixGameTestTemplate(false)
public final class HuahongGameTests {
    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void fullSpeedTurnAndBriefSprintReleaseRecoverWithoutReentry(GameTestHelper h) {
        var player=new GameTestPlayer(h.getLevel());
        try {
            var base=h.absolutePos(new BlockPos(0,0,0));
            var start=new Vec3(base.getX()+8,h.getLevel().getMaxBuildHeight()+10,base.getZ()+8);
            // Fixture preparation only. The observed turn/recovery below does
            // not teleport, lower the speed multiplier or load forward terrain.
            for(int x=-10;x<=2;x++)for(int z=-2;z<=8;z++)h.getLevel().getChunk((base.getX()>>4)+x,(base.getZ()>>4)+z);
            player.setPos(start);player.join();
            var data=GeneratedMod.data(player);data.wuxingxiantu$setRealm(99);data.wuxingxiantu$learnArt(YujianController.FLIGHT);
            data.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);data.wuxingxiantu$setMaxMana(100000);data.wuxingxiantu$setAura(100000);
            var stack=new ItemStack(WuXingItems.PURE_YANG_SWORD.get());player.getInventory().setItem(0,stack);
            player.getPersistentData().putLong("YujianCooldown_DUNXU",Long.MAX_VALUE);
            YujianController.cast(player,YujianController.FLIGHT,100);
            var root=h.getLevel().getEntitiesOfClass(ControlledSwordEntity.class,player.getBoundingBox().inflate(8),e->e.flightRoot()&&player.getUUID().equals(e.ownerId)).get(0);
            for(int i=0;i<14;i++)YujianController.tickFlightEntity(root,player);
            for(int i=0;i<140;i++) {
                root.setPos(start);root.positionRider(player);
                YujianController.handleInput(player,false,false,true,false,false,false,false,false,true,false,1,0,0,0);
                YujianController.tickFlightEntity(root,player);root.positionRider(player);
            }
            var definition=FlyingSwordRegistry.get(stack);
            double full=2*definition.advanced().maxSpeed(YujianPhase.JIYU,definition)*FlyingSwordConfig.CRUISE_FRACTION.get();
            h.assertTrue(root.huahongStage()==2&&Math.abs(root.flightSpeed()/full-1)<.02,"Fixture never reached actual full rainbow speed");
            double minSpeed=root.flightSpeed();var before=root.position();
            for(int i=0;i<6;i++) {
                YujianController.handleInput(player,false,false,true,false,false,false,false,false,false,false,1,0,150,0);
                YujianController.tickFlightEntity(root,player);root.positionRider(player);minSpeed=Math.min(minSpeed,root.flightSpeed());
                h.assertTrue(root.huahongStage()==2&&root.huahongStrength()==1&&player.getVehicle()==root&&!root.noPhysics,
                        "A real sharp turn/brief sprint release cancelled rainbow or changed collision permission");
            }
            h.assertTrue(minSpeed<full*.85,"The turn fixture did not exercise real deceleration");
            for(int i=0;i<8;i++) {
                YujianController.handleInput(player,false,false,true,false,false,false,false,false,true,false,1,0,150,0);
                YujianController.tickFlightEntity(root,player);root.positionRider(player);
                h.assertTrue(root.huahongStage()==2&&root.huahongStrength()==1,"Recovery restarted the gathering stage or exited rainbow");
            }
            h.assertTrue(root.position().distanceTo(before)>full*5&&root.flightSpeed()>full*.95,"Turn did not travel/recover at actual configured speed");
            YujianController.handleInput(player,false,false,false,false,false,false,false,false,false,false,1,0,150,0);
            YujianController.tickFlightEntity(root,player);
            h.assertTrue(root.huahongStage()==3&&player.getVehicle()==root,"Stopping forward input failed to exit rainbow immediately");
        } finally {YujianController.stop(player);player.close();}
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void rainbowUsesRealMovementAndDoesNotGrantCollisionPermission(GameTestHelper h){
        var p=new GameTestPlayer(h.getLevel());
        try{
            var base=h.absolutePos(new BlockPos(0,0,0));
            Vec3 start=new Vec3(base.getX()+8,h.getLevel().getMaxBuildHeight()+10,base.getZ()+8);
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)h.getLevel().getChunk((base.getX()>>4)+x,(base.getZ()>>4)+z);
            p.setPos(start);p.join();
            var data=GeneratedMod.data(p);data.wuxingxiantu$setRealm(99);data.wuxingxiantu$learnArt(YujianController.FLIGHT);
            data.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);data.wuxingxiantu$setMaxMana(100000);data.wuxingxiantu$setAura(100000);
            var stack=new ItemStack(WuXingItems.PURE_YANG_SWORD.get());p.getInventory().setItem(0,stack);
            YujianController.cast(p,YujianController.FLIGHT,100);
            var root=h.getLevel().getEntitiesOfClass(ControlledSwordEntity.class,p.getBoundingBox().inflate(8),e->e.flightRoot()&&p.getUUID().equals(e.ownerId)).get(0);
            for(int i=0;i<14;i++)YujianController.tickFlightEntity(root,p);
            h.assertTrue(p.getVehicle()==root,"Actual sword did not mount");
            for(var skin:HuahongSkin.values()){
                float mana=data.wuxingxiantu$getAura();double speed=root.flightSpeed();
                HuahongSkinPacket.apply(p,skin.ordinal());
                h.assertTrue(root.huahongSkin()==skin&&p.getPersistentData().getInt(HuahongSkin.KEY)==skin.ordinal(),"Skin metadata or saved preference differs");
                h.assertTrue(data.wuxingxiantu$getAura()==mana&&root.flightSpeed()==speed&&!root.noPhysics,"Cosmetic choice changed flight rules");
            }
            HuahongSkinPacket.apply(p,Integer.MAX_VALUE);
            h.assertTrue(root.huahongSkin()==HuahongSkin.METEOR_PRISM,"Invalid skin did not fall back safely");
            double normal=FlyingSwordRegistry.get(stack).advanced().maxSpeed(YujianPhase.JIYU,FlyingSwordRegistry.get(stack))*FlyingSwordConfig.CRUISE_FRACTION.get();
            p.getPersistentData().putLong("YujianCooldown_DUNXU",Long.MAX_VALUE);
            for(int i=0;i<150;i++){
                // Keep the long-distance movement fixture inside its generated region.
                root.setPos(start);root.positionRider(p);
                YujianController.handleInput(p,false,false,true,false,false,false,false,false,true,false,1,0,0,0);
                YujianController.tickFlightEntity(root,p);root.positionRider(p);
            }
            h.assertTrue(root.huahongStage()==2,"Guiyuan failed to enter rainbow using actual speed");
            h.assertTrue(Math.abs(root.flightSpeed()/normal-2)<.04,"Rainbow did not double normal speed: "+root.flightSpeed()/normal);
            h.assertTrue(root.advancedPhase()==YujianPhase.JIYU&&!root.noPhysics,"Rainbow changed collision permission");
            float before=data.wuxingxiantu$getAura();
            root.setPos(start);root.positionRider(p);
            YujianController.tickFlightEntity(root,p);
            // These ticks are driven manually, so also perform vanilla's
            // passenger-position phase before issuing the next input. At the
            // new full speed the carrier moves farther than the 8-block owner
            // safety radius in one tick; leaving the rider behind is not braking.
            root.positionRider(p);
            float expected=(float)(cn.blockforge.generated.generatedmod.items.CultivationLogic.maxAura(data)*HuahongFlight.UPKEEP_PER_SECOND/20);
            h.assertTrue(Math.abs(before-data.wuxingxiantu$getAura()-expected)<.02,"Rainbow double-charged ordinary upkeep");
            YujianController.handleInput(p,false,false,true,true,false,false,false,false,true,false,1,0,0,0);
            YujianController.tickFlightEntity(root,p);
            h.assertTrue(root.huahongStage()==3&&p.getVehicle()==root,"Brake failed to exit safely while retaining the mount");
            root.positionRider(p);
            for(int i=0;i<10;i++){YujianController.tickFlightEntity(root,p);root.positionRider(p);}
            h.assertTrue(root.huahongStage()==0&&root.huahongStrength()==0,"Rider/sword did not restore after exit");
        }finally{YujianController.stop(p);p.close();}
        h.succeed();
    }
}
