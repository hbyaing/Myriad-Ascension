package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

/** Actual server ticks with the explicitly supplied creation Mod and its real dimension. */
@GameTestHolder("wuxingxiantu_flightcompat")
@PrefixGameTestTemplate(false)
public final class XianjieFlightGameTests {
    private static final double[] HEIGHTS={-80,-65,-64,-32,-1,4,31.9,32,41.9,42,47.9,48,63.9,64,95.9,96,111.9,112,117.9,118,123.9,127.9,128,140,160,194,260};

    @GameTest(template="empty",timeoutTicks=1700)
    public static void summonAndClimbAcrossCloudLayerWithRealServerTicks(GameTestHelper h) {
        var level=h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation("wuxingchuangshi","xianjie")));
        h.assertTrue(level!=null,"The explicit creation compatibility fixture is missing");
        var p=new GameTestPlayer(level);
        p.cleanupAfter(h,()->YujianController.stop(p));
        // Only this generated test world is changed; a narrow empty shaft isolates altitude
        // and environment rules from real islands/structures that should obstruct flight.
        var base=new BlockPos(-504,0,504);
        p.setPos(base.getX()+.5,200,base.getZ()+.5);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            p.keepTicking(level,base.offset(x*16,0,z*16));
        for(var pos:BlockPos.betweenClosed(base.offset(-5,0,-5),base.offset(5,280,5)))
            if(!level.isEmptyBlock(pos))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        p.setNoGravity(true);p.join();
        var data=GeneratedMod.data(p);data.wuxingxiantu$setRealm(99);
        data.wuxingxiantu$learnArt(YujianController.FLIGHT);data.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);
        data.wuxingxiantu$setMaxMana(1000000);data.wuxingxiantu$setAura(1000000);
        p.getInventory().setItem(0,new ItemStack(WuXingItems.PURE_YANG_SWORD.get()));
        final ControlledSwordEntity[] sword={null};
        final double[] startY={0};
        h.onEachTick(()->{
            // This fixture deliberately dwells below the void line. Replenish
            // only test-player health so normal void damage does not end the altitude exercise.
            p.setHealth(p.getMaxHealth());
            long time=h.getTick()-20;
            if(time<0)return;
            int scenario=(int)(time/55),step=(int)(time%55);
            if(scenario>=HEIGHTS.length){h.succeed();return;}
            double y=HEIGHTS[scenario];
            if(step==0){
                YujianController.stop(p);p.setPos(base.getX()+.5,y,base.getZ()+.5);
                p.getPersistentData().remove("SwordFlightCooldown");data.wuxingxiantu$setAura(1000000);
                YujianController.cast(p,YujianController.FLIGHT,100);
                var roots=level.getEntitiesOfClass(ControlledSwordEntity.class,p.getBoundingBox().inflate(8),
                        s->s.flightRoot()&&p.getUUID().equals(s.ownerId));
                h.assertTrue(roots.size()==1,"Xianjie summon failed at Y="+y+", reason="+YujianController.flightDenialReason(p));
                sword[0]=roots.get(0);
            }
            if(step>=16){
                h.assertTrue(p.getVehicle()==sword[0]&&!sword[0].isRemoved(),
                        "Xianjie lost the mounted sword at Y="+y+", step="+step+", player="+p.getY()+", sword="+sword[0].getY());
                if(step==16)startY[0]=sword[0].getY();
                YujianController.handleInput(p,false,false,false,false,step<34,step>=34,false,false,false,false,0,0,0,0);
                if(step==33)h.assertTrue(sword[0].getY()>startY[0]+.1,"Xianjie ascent blocked at Y="+y);
                if(step==34)startY[0]=sword[0].getY();
                if(step==53)h.assertTrue(sword[0].getY()<startY[0]-.1,"Xianjie descent blocked at Y="+y);
            }
        });
    }
}
