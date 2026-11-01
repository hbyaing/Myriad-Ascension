package cn.blockforge.generated.generatedmod;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("wuxingxiantu")
@PrefixGameTestTemplate(false)
public final class StructureNeighbourLoadingGameTests {
    @GameTest(template="furnace_empty",timeoutTicks=40)
    public static void irrelevantNeighbourSearchDoesNotLoadTerrain(GameTestHelper h){
        var level=h.getLevel();
        // Deliberately outside every fixture/player ticket; no block placement
        // or getChunk call may warm this area before the production probes.
        var p=new BlockPos(2_000_015,90,2_000_015);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            h.assertTrue(level.getChunkSource().getChunkNow((p.getX()>>4)+x,(p.getZ()>>4)+z)==null,"Cold neighbour fixture was already loaded");
        h.assertTrue(!RefiningForgeStructure.hasComponentNear(level,p),"Unknown terrain was treated as a component");
        var event=new net.minecraftforge.event.level.BlockEvent.NeighborNotifyEvent(level,p,
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                java.util.EnumSet.allOf(net.minecraft.core.Direction.class),false);
        cn.blockforge.generated.generatedmod.event.AlchemyEvents.refiningForgeNeighbor(event);
        cn.blockforge.generated.generatedmod.event.AlchemyEvents.alchemyFurnaceNeighbor(event);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            h.assertTrue(level.getChunkSource().getChunkNow((p.getX()>>4)+x,(p.getZ()>>4)+z)==null,"Neighbour notification synchronously loaded terrain");
        h.succeed();
    }
    private StructureNeighbourLoadingGameTests(){}
}
