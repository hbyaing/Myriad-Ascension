package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.yujian.*;
import cn.blockforge.generated.generatedmod.robe.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("wuxingxiantu")
@PrefixGameTestTemplate(false)
public final class FlyingSwordGameTests {
    private static FakePlayer player(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(java.util.UUID.randomUUID(),"FlightTest"));
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(3,300,3));
        p.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);
        p.getInventory().setItem(0,new ItemStack(WuXingItems.DARK_IRON_SWORD.get()));
        var data=GeneratedMod.data(p);
        if(data!=null){data.wuxingxiantu$setRealm(5);data.wuxingxiantu$learnArt(YujianController.FLIGHT);
            data.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);
            data.wuxingxiantu$setAura(cn.blockforge.generated.generatedmod.items.CultivationLogic.maxAura(data));}
        h.getLevel().addNewPlayer(p);return p;
    }
    private static void input(FakePlayer p,boolean held,boolean edge,boolean forward,boolean brake,boolean roll){
        p.tickCount++;
        YujianController.handleInput(p,held,edge,forward,brake,false,false,roll,false,forward,false,1,1,0,0);
    }
    private static ControlledSwordEntity root(GameTestHelper h,FakePlayer p){
        return h.getLevel().getEntitiesOfClass(ControlledSwordEntity.class,p.getBoundingBox().inflate(8),s->s.flightRoot()&&p.getUUID().equals(s.ownerId)).get(0);
    }
    private static void ticks(ControlledSwordEntity sword,FakePlayer p,int count){
        for(int i=0;i<count;i++){
            YujianController.tickFlightEntity(sword,p);
            if(p.getVehicle()==sword)sword.positionRider(p);
        }
    }

    @GameTest(template="furnace_empty",timeoutTicks=100)
    public static void descentDoesNotEnterSprintPhase(GameTestHelper h){
        var p=player(h);
        try{
            YujianController.cast(p,YujianController.FLIGHT,0);var sword=root(h,p);ticks(sword,p,12);
            h.assertTrue(p.getVehicle()==sword,"Initial mount failed");
            p.tickCount++;
            YujianController.handleInput(p,false,false,true,false,false,true,false,false,false,false,1,1,0,0);
            ticks(sword,p,1);
            h.assertTrue(sword.advancedPhase()==cn.blockforge.generated.generatedmod.yujian.YujianPhase.ACCELERATING,
                    "Descent activated sprint");
            h.assertTrue(sword.getDeltaMovement().y<0,"Sneak action did not descend");
            p.tickCount++;
            YujianController.handleInput(p,false,false,true,false,false,false,false,false,true,false,1,1,0,0);
            ticks(sword,p,1);
            h.assertTrue(sword.advancedPhase()==cn.blockforge.generated.generatedmod.yujian.YujianPhase.JIYU,
                    "Sprint action did not activate accelerated flight");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(WuXingItems.AZURE_STEEL_SWORD.get()));
            h.assertTrue(cn.blockforge.generated.generatedmod.yujian.SwordNurture.current(p)
                            ==YujianSlot.nurtureStack(p,YujianSlot.NURTURE_SLOT),
                    "Flight wheel would release techniques from the held sword rather than the mounted sword");
        }finally{YujianController.stop(p);p.discard();}
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=100)
    public static void holdReleaseMountAndExitEdge(GameTestHelper h){
        var p=player(h);
        try{
            input(p,true,true,false,false,false);
            var sword=root(h,p);ticks(sword,p,8);
            h.assertTrue(!p.isPassenger()&&YujianController.riding(p),"Hold should orbit without mounting");
            input(p,false,false,false,false,false);ticks(sword,p,10);
            h.assertTrue(p.getVehicle()==sword,"Release did not mount: "+sword.art+" removed="+sword.isRemoved()+" at "+p.position());
            h.assertTrue(!p.getAbilities().mayfly,"Flight granted mayfly");
            input(p,true,true,false,false,false);ticks(sword,p,15);
            h.assertTrue(!YujianController.riding(p)&&!p.isPassenger(),"Exit failed");
            input(p,true,false,false,false,false);
            h.assertTrue(!YujianController.riding(p),"Held exit key resummoned without an edge");
            h.assertTrue(p.getInventory().getItem(0).isEmpty()&&YujianSlot.activeFlyingSword(p).getCount()==1,"Persistent flight slot lost or duplicated its sword");
        }finally{YujianController.stop(p);p.discard();}
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=100)
    public static void inertiaBrakeRollAndSourceRemoval(GameTestHelper h){
        // Inertia needs open, loaded sky; GameTest batch placement changes the nearest chunk edge.
        // Unloaded-boundary behavior is covered separately by FlightStallRegressionTests.
        var p=player(h);
        try{
            YujianController.cast(p,YujianController.FLIGHT,0);var sword=root(h,p);loadedSkyTicks(sword,p,12);
            h.assertTrue(p.getVehicle()==sword,"Initial mount failed: "+sword.art+" at "+p.position());
            input(p,false,false,true,false,false);loadedSkyTicks(sword,p,18);double normal=sword.flightSpeed();
            input(p,false,false,true,false,true);loadedSkyTicks(sword,p,40);
            h.assertTrue(Math.abs(sword.angularSpeed())>Math.toRadians(180)/20,"Roll did not accelerate: angular="+sword.angularSpeed()+" speed="+sword.flightSpeed()+" impact="+sword.impact()+" state="+sword.flightState());
            h.assertTrue(sword.flightSpeed()>normal,"Roll did not increase forward speed");
            input(p,false,false,false,false,false);loadedSkyTicks(sword,p,1);double glide=sword.flightSpeed();
            h.assertTrue(glide>0.1,"Release stopped instead of gliding: speed="+glide+" pos="+sword.position()+" impact="+sword.impact()+" state="+sword.flightState());
            input(p,false,false,false,true,false);loadedSkyTicks(sword,p,8);
            h.assertTrue(sword.flightSpeed()<glide*0.25,"Brake did not shed momentum");
            loadedSkyTicks(sword,p,120);
            h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(sword.roll()))<4,"Roll did not recover upright");
            GeneratedMod.data(p).wuxingxiantu$setYujianStack(ItemStack.EMPTY);loadedSkyTicks(sword,p,1);
            h.assertTrue(sword.isRemoved()&&!p.isPassenger(),"Removed source left a rideable clone");
        }finally{YujianController.stop(p);p.discard();}
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=100)
    public static void slotExtractionStopsFlightAndKeepsOriginal(GameTestHelper h){
        var p=player(h);
        try{
            ItemStack original=p.getMainHandItem();original.getOrCreateTag().putString("BatchBMarker","original");
            original.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);
            YujianController.cast(p,YujianController.FLIGHT,0);var sword=root(h,p);ticks(sword,p,12);
            h.assertTrue(YujianSlot.activeFlyingSword(p)==original&&p.getMainHandItem().isEmpty(),"Startup copied the sword instead of moving it");
            p.getInventory().selected=1;p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.STONE));
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.APPLE));ticks(sword,p,3);
            h.assertTrue(YujianController.riding(p)&&p.getVehicle()==sword,"Hand switch ended flight");
            var container=new RobeContainer(p);var menu=new RobeMenu(77,p.getInventory(),container);
            var saved=GeneratedMod.data(p).serializeNBT();var restored=new CultivationDataImpl();restored.deserializeNBT(saved);
            h.assertTrue(ItemStack.matches(original,restored.wuxingxiantu$getYujianStack()),"Slot save/reload lost stack data");
            // Taking the equipped sword now intentionally ends flight atomically.
            menu.clicked(1,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,p);
            h.assertTrue(!YujianController.riding(p)&&!p.isPassenger()&&sword.isRemoved(),"Extraction left a rideable clone");
            h.assertTrue(YujianSlot.activeFlyingSword(p).isEmpty(),"Shift extraction did not empty the slot");
            int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++){
                var stack=p.getInventory().getItem(i);if(stack.hasTag()&&"original".equals(stack.getTag().getString("BatchBMarker")))count+=stack.getCount();
            }
            h.assertTrue(count==1,"Sword count after extraction was "+count);
        }finally{YujianController.stop(p);p.discard();}
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=100)
    public static void equippedStartupAndDeathDropAreSingleOwner(GameTestHelper h){
        var p=player(h);var rule=h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
        boolean previous=rule.get();
        try{
            ItemStack original=YujianSlot.equipFromMainHand(p);
            h.assertTrue(!original.isEmpty()&&p.getMainHandItem().isEmpty(),"Could not pre-equip");
            h.assertTrue(!YujianSlot.accepts(new ItemStack(Items.DIAMOND_SWORD))&&!YujianSlot.accepts(new ItemStack(Items.STONE)),"Illegal item accepted");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));
            h.assertTrue(YujianController.canCast(p,YujianController.FLIGHT),"Equipped sword still requires main hand");
            var drops=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
            var event=new net.minecraftforge.event.entity.living.LivingDropsEvent(p,p.damageSources().generic(),drops,0,false);
            rule.set(true,h.getLevel().getServer());RobeSlots.drops(event);
            h.assertTrue(drops.isEmpty()&&YujianSlot.activeFlyingSword(p)==original,"keepInventory dropped the equipped sword");
            rule.set(false,h.getLevel().getServer());RobeSlots.drops(event);RobeSlots.drops(event);
            h.assertTrue(drops.size()==1&&drops.get(0).getItem()==original&&YujianSlot.activeFlyingSword(p).isEmpty(),"Death duplicated or lost the equipped sword");
        }finally{rule.set(previous,h.getLevel().getServer());YujianController.stop(p);p.discard();}
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=100)
    public static void chargeRequiresRollAndRespectsCooldown(GameTestHelper h){
        var p=player(h);
        try{
            YujianController.cast(p,YujianController.FLIGHT,0);var sword=root(h,p);ticks(sword,p,12);
            input(p,false,false,true,false,false);loadedSkyTicks(sword,p,25);
            returnToFixtureAir(h,p,sword);
            var straightTarget=ironGolem(h,sword.position().add(0,0,2.0));
            assertVisible(h,straightTarget);
            float health=straightTarget.getHealth();loadedSkyTicks(sword,p,1);
            h.assertTrue(straightTarget.getHealth()==health,"Straight flight caused charge damage without rolling");
            straightTarget.discard();
            input(p,false,false,true,false,true);loadedSkyTicks(sword,p,35);
            h.assertTrue(sword.flightSpeed()>=cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig.CHARGE_SPEED.get()
                            &&Math.abs(sword.angularSpeed())>=cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig.CHARGE_ROLL.get(),
                    "Charge fixture never reached speed/roll threshold: speed="+sword.flightSpeed()+" roll="+sword.angularSpeed());
            returnToFixtureAir(h,p,sword);
            var chargeTarget=ironGolem(h,sword.position().add(0,0,2.0));
            assertVisible(h,chargeTarget);
            float chargeHealth=chargeTarget.getHealth();
            try{
                h.assertTrue(chargeTarget.isAddedToWorld()&&!chargeTarget.isRemoved(),"Charge fixture target was removed before collision: "+chargeTarget.position());
                var beforeCharge=sword.position();
                loadedSkyTicks(sword,p,1);
                h.assertTrue(chargeTarget.getHealth()<chargeHealth,"Swept sword tip missed a query-visible charge target");
                float after=chargeTarget.getHealth();chargeTarget.invulnerableTime=0;
                // Re-enter the same swept segment while the target cooldown is still active.
                sword.setPos(beforeCharge);sword.positionRider(p);p.tickCount++;
                loadedSkyTicks(sword,p,1);
                h.assertTrue(chargeTarget.getHealth()==after,"Charge ignored per-target cooldown");
                h.succeed();
            }finally{YujianController.stop(p);chargeTarget.discard();p.discard();}
        }catch(Throwable error){YujianController.stop(p);p.discard();throw error;}
    }

    /** Keep collision targets in the GameTest's entity-visible chunk, not in manually generated far chunks. */
    private static void returnToFixtureAir(GameTestHelper h,FakePlayer player,ControlledSwordEntity sword) {
        var at=h.absolutePos(new net.minecraft.core.BlockPos(3,200,3));
        sword.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
        sword.positionRider(player);
    }
    private static void assertVisible(GameTestHelper h,net.minecraft.world.entity.animal.IronGolem target) {
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class,
                target.getBoundingBox().inflate(.01)).contains(target),"Charge target must be query-visible before collision is tested");
    }

    /** Flight behavior fixtures need a loaded path; unloaded-boundary tests keep using ticks(). */
    private static net.minecraft.world.entity.animal.IronGolem ironGolem(GameTestHelper h,net.minecraft.world.phys.Vec3 position) {
        var target=net.minecraft.world.entity.EntityType.IRON_GOLEM.create(h.getLevel());
        target.setNoAi(true);target.setNoGravity(true);target.setPos(position);
        var chunk=net.minecraft.core.BlockPos.containing(position);
        h.getLevel().getChunk(chunk.getX()>>4,chunk.getZ()>>4);
        h.getLevel().addFreshEntity(target);
        return target;
    }

    private static void loadedSkyTicks(ControlledSwordEntity sword,FakePlayer player,int count) {
        for(int i=0;i<count;i++) {
            int x=sword.blockPosition().getX()>>4,z=sword.blockPosition().getZ()>>4;
            // These behavior fixtures require a loaded braking/look-ahead
            // corridor, not just the immediate collision neighbourhood. With
            // only 3x3 chunks the real terrain governor can reduce speed and
            // hence the roll limit, depending on unrelated batch ticket loads.
            for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++)player.serverLevel().getChunk(x+dx,z+dz);
            ticks(sword,player,1);
        }
    }

    @GameTest(template="furnace_empty",timeoutTicks=100)
    public static void unboundedRollBothDirectionsAndRecovery(GameTestHelper h){
        for(int sign:new int[]{-1,1}){
            var roll=new ManualRoll();double max=Math.toRadians(330)/20,acc=Math.toRadians(160)/400,start=Math.toRadians(90)/20;
            for(int i=0;i<180;i++){
                double old=roll.angle;roll.tick(sign,max,acc,start,.87);
                h.assertTrue(sign*(roll.angle-old)>0,"Held roll reversed at tick "+i);
                h.assertTrue(Math.abs(roll.velocity)<=max+1e-9,"Roll exceeded cap");
            }
            h.assertTrue(sign*roll.angle>Math.PI*10,"Fewer than five full revolutions");
            double release=roll.angle;roll.tick(0,max,acc,start,.87,1,ManualRoll.ReleaseMode.RECENTER,false);
            h.assertTrue(sign*(roll.angle-release)>0,"Release had no inertia");
            for(int i=0;i<200;i++)roll.tick(0,max,acc,start,.87,1,ManualRoll.ReleaseMode.RECENTER,false);
            double upright=Math.rint(roll.angle/(Math.PI*2))*Math.PI*2;
            h.assertTrue(Math.abs(roll.angle-upright)<.001,"Release did not settle upright");
            h.assertTrue(sign*roll.angle>Math.PI*10,"Recovery reset to absolute zero");
            for(int i=0;i<50;i++)roll.tick(sign,max,acc,start,.87);
            for(int i=0;i<110;i++){
                double oldVelocity=roll.velocity;
                roll.tick(-sign,max,acc,start,.87);
                h.assertTrue(Math.abs(roll.velocity-oldVelocity)<=acc*1.8+1e-9,"Direction reversal jumped angular velocity");
            }
            h.assertTrue(-sign*roll.velocity>0,"Direction reversal failed");
        }
        h.succeed();
    }

    @GameTest(template="furnace_empty",timeoutTicks=100)
    public static void wallStopsChargeAndProtectsTarget(GameTestHelper h){
        var p=player(h);var blocks=new java.util.ArrayList<net.minecraft.core.BlockPos>();
        net.minecraft.world.entity.animal.IronGolem target=null;
        try{
            YujianController.cast(p,YujianController.FLIGHT,0);var sword=root(h,p);ticks(sword,p,12);
            input(p,false,false,true,false,true);ticks(sword,p,25);
            float cruise=sword.flightSpeed();
            returnToFixtureAir(h,p,sword);
            var center=sword.blockPosition().offset(0,0,2);
            for(int x=-2;x<=2;x++)for(int y=-3;y<=4;y++){
                var pos=center.offset(x,y,0);blocks.add(pos);
                h.getLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
            target=ironGolem(h,sword.position().add(0,0,4));assertVisible(h,target);float health=target.getHealth();
            ticks(sword,p,5);
            h.assertTrue(sword.getZ()<center.getZ(),"Flight crossed the wall");
            h.assertTrue(sword.flightSpeed()<cruise*0.4&&sword.impact()>0,"Wall did not interrupt charge: speed="+sword.flightSpeed()+" impact="+sword.impact()+" pos="+sword.position()+" wall="+center);
            h.assertTrue(target.getHealth()==health,"Charge damaged target through wall");
        }finally{
            YujianController.stop(p);if(target!=null)target.discard();p.discard();
            for(var pos:blocks)h.getLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }
}
