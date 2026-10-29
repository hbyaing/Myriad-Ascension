package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.alchemy.batch.*;
import cn.blockforge.generated.generatedmod.alchemy.inputs.*;
import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.util.*;

/** Isolated namespace: other acceptance tests intentionally change shared world time. */
@GameTestHolder("wuxingxiantu_v5dawn") @PrefixGameTestTemplate(false)
public final class AlchemyNaturalDawnGameTests {
    @GameTest(template="empty",batch="v5_natural_dawn",timeoutTicks=2700) public static void naturallyAdvancingDaylightStopsRealBottleCollection(GameTestHelper h){
        var level=h.getLevel();long time=level.getDayTime();boolean daylight=level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT),rain=level.isRaining(),thunder=level.isThundering();
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,3,1)).atY(level.getMaxBuildHeight()-5);var player=new GameTestPlayer(level);player.keepTicking(level,pos);
        player.cleanupAfter(h,()->{level.removeBlock(pos,false);level.setDayTime(time);level.updateSkyBrightness();level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(daylight,level.getServer());level.setWeatherParameters(10000,10000,rain,thunder);});
        level.setWeatherParameters(10000,0,false,false);level.setDayTime(22000);level.updateSkyBrightness();level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(true,level.getServer());
        level.setBlockAndUpdate(pos,WuXingBlocks.MOON_BOTTLE.get().defaultBlockState());
        h.assertTrue(AlchemyEnvironment.sample(level,pos).values().get("night")==1,"Initial real world not night");
        h.runAfterDelay(2500,()->{
            h.assertTrue(level.getDayTime()>=24500&&AlchemyEnvironment.sample(level,pos).values().get("night")==0,"Natural dawn not observed: day="+level.getDayTime()+" daylight="+level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)+" sample="+AlchemyEnvironment.sample(level,pos));
            var bottle=(MoonBottleBlockEntity)level.getBlockEntity(pos);long collected=bottle.contents().total();h.assertTrue(collected>0,"Real loaded bottle never collected moonlight");while(bottle.contents().total()>1000)bottle.release(Math.min(1000,bottle.contents().total()-1000));long amount=bottle.contents().total();
            h.runAfterDelay(60,()->{h.assertTrue(bottle.contents().total()==amount,"Daylight continued moonlight collection");h.succeed();});
        });
    }
}
