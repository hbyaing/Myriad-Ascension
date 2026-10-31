package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.registry.ModEntities;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;

@GameTestHolder("wuxingxiantu_entry")
@PrefixGameTestTemplate(false)
public final class FlightAltitudeGameTests {
    @GameTest(template="empty",timeoutTicks=200)
    public static void summonAndFlightCrossHeightAndSectionBoundaries(GameTestHelper h) {
        try(var p=new GameTestPlayer(h.getLevel())) {
            var origin=h.absolutePos(new BlockPos(8,0,8));
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)
                h.getLevel().getChunk((origin.getX()>>4)+x,(origin.getZ()>>4)+z);
            var d=GeneratedMod.data(p);d.wuxingxiantu$setRealm(99);
            d.wuxingxiantu$learnArt(YujianController.FLIGHT);d.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);
            d.wuxingxiantu$setMaxMana(1000000);d.wuxingxiantu$setAura(1000000);
            p.getInventory().setItem(0,new ItemStack(WuXingItems.PURE_YANG_SWORD.get()));
            p.setPos(origin.getX(),400,origin.getZ());p.join();
            try {
                // Below the void line and above terrain; the flat world's bedrock
                // at -64 must still reject an intersecting mount. Xianjie has its own empty-shaft test.
                for(double y:new double[]{-144,-129,-128,255.9,256,319.9,320,335.9,336,511.9,512,1024}) {
                    p.setPos(origin.getX(),y,origin.getZ());p.getPersistentData().remove("SwordFlightCooldown");
                    YujianController.cast(p,YujianController.FLIGHT,100);
                    var roots=h.getLevel().getEntitiesOfClass(ControlledSwordEntity.class,p.getBoundingBox().inflate(8),s->s.flightRoot()&&p.getUUID().equals(s.ownerId));
                    h.assertTrue(roots.size()==1,"Summon missing at Y="+y);
                    var root=roots.get(0);
                    for(int t=0;t<14;t++)YujianController.tickFlightEntity(root,p);
                    h.assertTrue(p.getVehicle()==root,"Mount failed at Y="+y);
                    for(int t=0;t<12;t++) {
                        YujianController.handleInput(p,false,false,true,false,true,false,false,false,false,false,1,0,0,-45);
                        root.tick();root.positionRider(p);
                        h.assertTrue(!root.isRemoved()&&p.getVehicle()==root,"Flight exited at Y="+root.getY());
                    }
                    h.assertTrue(root.getY()>y,"Cannot climb at Y="+y);
                    YujianController.stop(p);
                }
            } finally {YujianController.stop(p);}
        }
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void voidCleanupStillRemovesUncontrolledSwords(GameTestHelper h) {
        try(var p=new GameTestPlayer(h.getLevel())){
            p.setPos(h.absolutePos(new BlockPos(8,0,8)).getX(),h.getLevel().getMinBuildHeight()-80,8);p.join();
            var stack=new ItemStack(WuXingItems.PURE_YANG_SWORD.get());
            var ordinary=ModEntities.CONTROLLED_SWORD.get().create(h.getLevel());
            ordinary.bind(p,stack,0,false);ordinary.checkBelowWorld();
            h.assertTrue(ordinary.isRemoved(),"Ordinary attack swords lost vanilla void cleanup");
            var orphan=ModEntities.CONTROLLED_SWORD.get().create(h.getLevel());
            orphan.bind(p,stack,0,true);orphan.flightRoot(true);orphan.ownerId=java.util.UUID.randomUUID();
            orphan.checkBelowWorld();
            h.assertTrue(orphan.isRemoved(),"Ownerless flight sword survived void cleanup");
            var controlled=ModEntities.CONTROLLED_SWORD.get().create(h.getLevel());
            controlled.bind(p,stack,0,true);controlled.flightRoot(true);
            float health=p.getHealth();controlled.checkBelowWorld();
            h.assertTrue(!controlled.isRemoved()&&p.getHealth()==health,"Carrier cleanup changed player health or removed the live carrier");
            controlled.discard();
        }
        h.succeed();
    }
}
