package cn.blockforge.examples.admission;

import cn.blockforge.generated.generatedmod.AlchemyFurnaceBlockEntity;
import cn.blockforge.generated.generatedmod.alchemy.batch.BatchStartResult;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

/** Optional operator example, a separate mod; no retries, escrow, RNG or journal ownership. */
@Mod(AdmissionAddon.ID)
public final class AdmissionAddon {
    public static final String ID="myriad_admission_example";
    public AdmissionAddon(){MinecraftForge.EVENT_BUS.addListener(AdmissionAddon::commands);}
    private static void commands(RegisterCommandsEvent event){
        var root=Commands.literal("admission_probe").requires(source->source.hasPermission(2));
        root.then(Commands.literal("inspect").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(context->{
            var source=context.getSource();var pos=BlockPosArgument.getLoadedBlockPos(context,"pos");
            if(!(source.getLevel().getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace)){
                source.sendFailure(Component.literal("No ordinary alchemy furnace at "+pos.toShortString()));return 0;
            }
            var snapshot=furnace.v5ServerRecord();
            // Expose status and identity only. Seeds, materials and the private checkpoint stay server-side.
            String text="occupied="+furnace.inventoryLocked()+", state="+snapshot.getString("State")
                    +", batch="+(snapshot.hasUUID("Id")?snapshot.getUUID("Id"):"not published");
            source.sendSuccess(()->Component.literal(text),false);return 1;
        })));
        root.then(Commands.literal("start").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(context->{
            var source=context.getSource();var player=source.getPlayerOrException();
            var pos=BlockPosArgument.getLoadedBlockPos(context,"pos");
            if(!(source.getLevel().getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace)){
                source.sendFailure(Component.literal("No ordinary alchemy furnace at "+pos.toShortString()));return 0;
            }
            // The real furnace remains the rule and mutation authority, including distance checks.
            var result=furnace.tryStartV5Batch(player,null);String text=describe(result);
            if(result.started())source.sendSuccess(()->Component.literal(text),false);
            else source.sendFailure(Component.literal(text));
            // A zero command result is NOT proof of no commit. Read the stage/identity before acting.
            return result.started()?1:0;
        })));
        event.getDispatcher().register(root);
    }
    public static String describe(BatchStartResult result){
        String text="reason="+result.reason()+", stage="+result.stage()+", batch="+result.batchId();
        return result.needsRecovery()?text+"; inspect/repair the original batch; do not resubmit":text;
    }
}
