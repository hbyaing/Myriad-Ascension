package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.*;
import java.util.LinkedHashMap;
import java.util.Map;

@GameTestHolder("wuxingxiantu")
@PrefixGameTestTemplate(false)
public final class HuahongGameTests {
    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void gatheringAndFullRainbowPassOrdinaryWallWithoutDunxu(GameTestHelper h){
        try(var f=new WallFlight(h)){
            f.wall(10,10,Blocks.STONE.defaultBlockState());
            f.warm(1);
            h.assertTrue(f.root.huahongStage()==1,"A nearby ordinary wall prevented gathering");
            f.place(f.start.add(0,0,8));
            f.drive(true,false,true);
            h.assertTrue(f.root.huahongStage()==1&&f.root.getZ()>f.start.z+11,
                    "Gathering rainbow did not pass the real stone wall");
            f.warm(2);f.place(f.start);
            int impacts=f.root.impact();f.drive(true,false,true);
            h.assertTrue(f.root.huahongStage()==2&&f.root.getZ()>f.start.z+11&&f.root.impact()==impacts,
                    "Full-speed rainbow stopped at an ordinary stone wall");
            h.assertTrue(f.root.advancedPhase()!=YujianPhase.DUNXU&&f.player.getVehicle()==f.root&&!f.root.noPhysics,
                    "Wall passage required Dunxu, lost the rider or leaked the movement bypass flag");
            f.assertOriginalSword();
        }
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void rainbowBrakeInsideWallRestoresNearbyCollisionAndMount(GameTestHelper h){insideWallExit(h,0);}

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void rainbowForwardReleaseInsideWallRestoresNearbyCollisionAndMount(GameTestHelper h){insideWallExit(h,1);}

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void rainbowLowAuraInsideWallRestoresNearbyCollisionAndMount(GameTestHelper h){insideWallExit(h,2);}

    private static void insideWallExit(GameTestHelper h,int reason){
        try(var f=new WallFlight(h)){
            f.wall(10,30,Blocks.STONE.defaultBlockState());f.warm(2);
            h.assertTrue(!FlightCollision.clear(f.player,f.volume(),false),"Fixture did not end inside a real thick wall");
            Vec3 inside=f.root.position();
            if(reason==2)GeneratedMod.data(f.player).wuxingxiantu$setAura(1);
            f.drive(reason!=1,reason==0,true);
            h.assertTrue(f.root.huahongStage()==3&&f.player.getVehicle()==f.root,
                    "Exiting rainbow inside a wall lost the mount or retained wall permission: reason="+reason);
            h.assertTrue(FlightCollision.clear(f.player,f.volume(),false)&&!f.root.noPhysics,
                    "Collision resumed with the rider/sword still embedded: reason="+reason);
            h.assertTrue(f.root.position().distanceTo(inside)<6,"Safe exit jumped back an entire high-speed history tick");
            h.assertTrue(!f.player.getPersistentData().getBoolean("YujianEmergencyReturn"),"Nearby exit incorrectly required emergency recovery");
            f.assertOriginalSword();
        }
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void rainbowRecallInsideWallReturnsSafelyWithoutDroppingSword(GameTestHelper h){
        for(boolean inputExit:new boolean[]{false,true})try(var f=new WallFlight(h)){
            f.wall(10,30,Blocks.STONE.defaultBlockState());f.warm(2);
            h.assertTrue(!FlightCollision.clear(f.player,f.volume(),false),"Recall fixture was not inside the wall");
            Vec3 inside=f.root.position();
            if(inputExit){
                YujianController.handleInput(f.player,false,true,false,false,false,false,false,false,false,false,1,0,0,0);
                h.assertTrue(f.player.level().noCollision(f.player),"Exit input left the rider embedded until a later tick");
                for(int i=0;i<16&&!f.root.isRemoved();i++)YujianController.tickFlightEntity(f.root,f.player);
            }else YujianController.stop(f.player);
            h.assertTrue(!f.player.isPassenger()&&f.root.isRemoved(),"Explicit recall did not finish");
            h.assertTrue(f.player.level().noCollision(f.player)&&f.player.position().distanceTo(inside)<6,
                    "Recall left the rider inside the wall or teleported back to distant history");
            f.assertOriginalSword();
        }
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void rainbowStillStopsAtProtectedBlocksAndBlockEntities(GameTestHelper h){
        for(BlockState obstacle:new BlockState[]{Blocks.OBSIDIAN.defaultBlockState(),Blocks.CHEST.defaultBlockState()}){
            try(var f=new WallFlight(h)){
                f.warm(2);f.place(f.start);f.wall(4,4,obstacle);int impacts=f.root.impact();
                f.drive(true,false,true);
                h.assertTrue(f.root.getZ()<f.start.z+4&&f.root.impact()>impacts&&f.root.huahongStage()==3,
                        "Rainbow crossed a protected block/entity or failed to exit: "+obstacle);
                h.assertTrue(f.player.getVehicle()==f.root&&FlightCollision.clear(f.player,f.volume(),false),
                        "Protected collision lost the mounted rider or left an embedded body");
                f.assertOriginalSword();
            }
        }
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void rainbowWallPermissionPreventsSuffocationOnlyWhileActive(GameTestHelper h){
        try(var f=new WallFlight(h)){
            f.wall(10,30,Blocks.STONE.defaultBlockState());f.warm(2);
            h.assertTrue(!FlightCollision.clear(f.player,f.volume(),false),"Suffocation fixture was not inside the wall");
            var active=new net.minecraftforge.event.entity.living.LivingAttackEvent(f.player,f.player.damageSources().inWall(),5);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(active);
            h.assertTrue(active.isCanceled(),"A legitimate phasing rider can suffocate while waiting inside ordinary terrain");
            float health=f.player.getHealth();f.player.invulnerableTime=0;
            h.assertTrue(!f.player.hurt(f.player.damageSources().inWall(),5)&&f.player.getHealth()==health,
                    "Real in-wall damage bypassed rainbow's narrowly scoped suffocation protection");
            f.drive(false,false,false);
            var exited=new net.minecraftforge.event.entity.living.LivingAttackEvent(f.player,f.player.damageSources().inWall(),5);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(exited);
            h.assertTrue(!exited.isCanceled(),"Suffocation permission leaked after rainbow exited");
            f.assertOriginalSword();
        }
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void rainbowIgnoresStaleVanillaCollisionFlags(GameTestHelper h){
        try(var f=new WallFlight(h)){
            f.warm(2);f.place(f.start);int impacts=f.root.impact();
            f.root.horizontalCollision=true;f.root.verticalCollision=true;
            f.drive(true,false,true);
            h.assertTrue(f.root.huahongStage()==2&&f.root.impact()==impacts,"Old vanilla collision flags cancelled a clear phasing sweep");
            f.assertOriginalSword();
        }
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void embeddedRainbowStopsWithoutRevokingPhasing(GameTestHelper h){
        try(var f=new WallFlight(h)){
            f.warm(2);f.place(f.start);f.wall(-3,12,Blocks.STONE.defaultBlockState());
            f.wall(4,4,Blocks.OBSIDIAN.defaultBlockState());
            h.assertTrue(!FlightCollision.clear(f.player,f.volume(),false),"Fixture must start embedded in ordinary terrain");
            Vec3 before=f.root.position();int impacts=f.root.impact();
            for(int i=0;i<12;i++)f.drive(true,false,true);
            h.assertTrue(f.root.impact()>impacts,"Protected collision did not execute");
            h.assertTrue(f.root.huahongStage()==2&&f.player.getVehicle()==f.root,"Embedded collision revoked rainbow permission");
            h.assertTrue(f.root.getY()<=before.y+.01&&f.root.getZ()<f.start.z+4,"Embedded collision relocated upward or crossed the protected obstacle");
            f.assertOriginalSword();
        }
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void undergroundDunxuDiveStopsAtBedrockWithoutSkyRecovery(GameTestHelper h) throws Exception {
        try(var f=new WallFlight(h)){
            // Isolate Dunxu from rainbow entry; normal input still enters and
            // maintains Dunxu. Only the unrelated cosmetic boost is inhibited.
            var rides=YujianController.class.getDeclaredField("RIDES");rides.setAccessible(true);
            Object ride=((Map<?,?>)rides.get(null)).get(f.player.getUUID());
            var rainbow=ride.getClass().getDeclaredField("huahong");rainbow.setAccessible(true);
            var flight=(HuahongFlight)rainbow.get(ride);
            var cooldown=HuahongFlight.class.getDeclaredField("cooldown");cooldown.setAccessible(true);cooldown.setInt(flight,10000);
            f.player.getPersistentData().remove("YujianCooldown_DUNXU");
            for(int i=0;i<90;i++){f.place(f.start);f.drive(true,false,true);}
            h.assertTrue(f.root.advancedPhase()==YujianPhase.DUNXU&&f.root.dunxuStage()==2&&f.root.huahongStage()==0,"Fixture must use real Dunxu without rainbow");
            Vec3 underground=new Vec3(f.start.x,40,f.start.z);
            int x=(int)Math.floor(underground.x),z=(int)Math.floor(underground.z);
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int y=35;y<=44;y++){
                var at=new BlockPos(x+dx,y,z+dz);f.previous.putIfAbsent(at,h.getLevel().getBlockState(at));
                h.getLevel().setBlockAndUpdate(at,(y<=36?Blocks.BEDROCK:Blocks.STONE).defaultBlockState());
            }
            f.place(underground);f.root.setXRot(90);f.root.setDeltaMovement(new Vec3(0,-8,0));int impacts=f.root.impact();
            for(int i=0;i<14;i++){
                f.player.tickCount++;YujianController.handleInput(f.player,false,false,true,false,false,false,false,false,true,false,1,0,0,90);
                YujianController.tickFlightEntity(f.root,f.player);f.root.positionRider(f.player);
                h.assertTrue(f.root.getY()<=40.01&&f.player.getVehicle()==f.root,"Dunxu dive relocated upward or lost its rider");
                h.assertTrue(f.root.advancedPhase()==YujianPhase.DUNXU&&f.root.huahongStage()==0,"Protected impact revoked Dunxu permission");
            }
            h.assertTrue(f.root.impact()>impacts&&f.root.getY()>36,"Dive must really hit, and must not pass protected bedrock");
        }
        h.succeed();
    }

    private static final class WallFlight implements AutoCloseable {
        final GameTestHelper helper;final GameTestPlayer player;final ControlledSwordEntity root;final Vec3 start;
        final ItemStack original;final String marker=java.util.UUID.randomUUID().toString();
        final Map<BlockPos,BlockState> previous=new LinkedHashMap<>();
        WallFlight(GameTestHelper helper){
            this.helper=helper;player=new GameTestPlayer(helper.getLevel());
            var base=helper.absolutePos(new BlockPos(0,0,0));
            start=new Vec3(base.getX()+8.5,helper.getLevel().getMaxBuildHeight()-16,base.getZ()+8.5);
            // Test setup only: include the full-speed braking/prefetch corridor.
            // Cold streaming is tested separately without these generated chunks.
            for(int x=-2;x<=2;x++)for(int z=-2;z<=10;z++)helper.getLevel().getChunk((base.getX()>>4)+x,(base.getZ()>>4)+z);
            player.setPos(start);player.join();
            var data=GeneratedMod.data(player);data.wuxingxiantu$setRealm(99);data.wuxingxiantu$learnArt(YujianController.FLIGHT);
            data.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);data.wuxingxiantu$setMaxMana(100000);data.wuxingxiantu$setAura(100000);
            original=new ItemStack(WuXingItems.PURE_YANG_SWORD.get());original.getOrCreateTag().putString("RainbowWallMarker",marker);
            player.getInventory().setItem(0,original);player.getPersistentData().putLong("YujianCooldown_DUNXU",Long.MAX_VALUE);
            YujianController.cast(player,YujianController.FLIGHT,100);
            root=helper.getLevel().getEntitiesOfClass(ControlledSwordEntity.class,player.getBoundingBox().inflate(8),
                    e->e.flightRoot()&&player.getUUID().equals(e.ownerId)).get(0);
            for(int i=0;i<14;i++)YujianController.tickFlightEntity(root,player);
            root.positionRider(player);
        }
        void place(Vec3 position){root.setPos(position);root.positionRider(player);}
        void drive(boolean forward,boolean brake,boolean sprint){
            player.tickCount++;YujianController.handleInput(player,false,false,forward,brake,false,false,false,false,sprint,false,1,0,0,0);
            YujianController.tickFlightEntity(root,player);if(player.getVehicle()==root)root.positionRider(player);
        }
        void warm(int stage){
            for(int i=0;i<140;i++){
                place(start);drive(true,false,true);
                helper.assertTrue(root.advancedPhase()!=YujianPhase.DUNXU,"Fixture accidentally acquired Dunxu permission");
                if(stage==1&&root.huahongStage()==1)return;
            }
            helper.assertTrue(root.huahongStage()==stage,"Fixture failed to reach rainbow stage "+stage);
        }
        void wall(int near,int far,BlockState state){
            int x=(int)Math.floor(start.x),y=(int)Math.floor(start.y),z=(int)Math.floor(start.z);
            for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=4;dy++)for(int dz=near;dz<=far;dz++){
                var at=new BlockPos(x+dx,y+dy,z+dz);previous.putIfAbsent(at,helper.getLevel().getBlockState(at));
                helper.getLevel().setBlockAndUpdate(at,state);
            }
        }
        AABB volume(){return FlightFrame.riderBounds(root.position(),root.getYRot(),root.getXRot(),root.roll(),root.riderOffset())
                .minmax(player.getBoundingBox()).minmax(root.getBoundingBox());}
        void assertOriginalSword(){
            helper.assertTrue(cn.blockforge.generated.generatedmod.robe.YujianSlot.activeFlyingSword(player)==original&&original.getCount()==1,
                    "Wall passage/exit lost or copied the equipped sword");
            int count=original.getCount();for(int i=0;i<player.getInventory().getContainerSize();i++){
                var stack=player.getInventory().getItem(i);if(stack.hasTag()&&marker.equals(stack.getTag().getString("RainbowWallMarker")))count+=stack.getCount();
            }
            helper.assertTrue(count==1,"Wall passage/exit duplicated the original sword");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    player.getBoundingBox().inflate(40),e->e.getItem().hasTag()&&marker.equals(e.getItem().getTag().getString("RainbowWallMarker"))).isEmpty(),
                    "Wall passage/exit dropped the original sword");
        }
        @Override public void close(){try{YujianController.stop(player);}finally{previous.forEach(helper.getLevel()::setBlockAndUpdate);player.close();}}
    }

    @GameTest(template="furnace_empty",timeoutTicks=200)
    public static void fullSpeedTurnAndBriefSprintReleaseRecoverWithoutReentry(GameTestHelper h) {
        var player=new GameTestPlayer(h.getLevel());
        try {
            var base=h.absolutePos(new BlockPos(0,0,0));
            var start=new Vec3(base.getX()+8,h.getLevel().getMaxBuildHeight()+10,base.getZ()+8);
            // Fixture preparation only. The observed turn/recovery below does
            // not teleport, lower the speed multiplier or load forward terrain.
            for(int x=-18;x<=2;x++)for(int z=-6;z<=18;z++)h.getLevel().getChunk((base.getX()>>4)+x,(base.getZ()>>4)+z);
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
                        "A real sharp turn/brief sprint release cancelled rainbow or leaked the temporary movement flag");
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
    public static void rainbowUsesRealMovementAndPreservesMountedSword(GameTestHelper h){
        var p=new GameTestPlayer(h.getLevel());
        try{
            var base=h.absolutePos(new BlockPos(0,0,0));
            Vec3 start=new Vec3(base.getX()+8,h.getLevel().getMaxBuildHeight()+10,base.getZ()+8);
            // Pure speed/cost fixture: loaded braking corridor, not a cold-streaming test.
            for(int x=-2;x<=2;x++)for(int z=-2;z<=10;z++)h.getLevel().getChunk((base.getX()>>4)+x,(base.getZ()>>4)+z);
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
            h.assertTrue(root.advancedPhase()==YujianPhase.JIYU&&!root.noPhysics,"Rainbow changed the base phase or leaked its temporary movement flag");
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
