package cn.blockforge.generated.generatedmod;

import cn.blockforge.examples.admission.AdmissionAddon;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

/** Commands are parsed/dispatched through the real Forge-registered command tree. */
@GameTestHolder("wuxingxiantu_entry") @PrefixGameTestTemplate(false)
public final class AdmissionAddonGameTests {
    private static int command(V5BatchGameTests.Fixture f,int permission,String action){
        return f.player().server.getCommands().performPrefixedCommand(f.player().createCommandSourceStack().withPermission(permission),
                "admission_probe "+action+" "+f.pos().getX()+" "+f.pos().getY()+" "+f.pos().getZ());
    }
    @GameTest(template="empty",batch="admission_example") public static void operatorStartsOnceAndInspectionDoesNotMutate(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            h.assertTrue(command(f,2,"start")==1,"Separate addon command did not start furnace");
            var admitted=f.furnace().v5ServerRecord();
            h.assertTrue(command(f,2,"inspect")==1&&admitted.equals(f.furnace().v5ServerRecord()),"Inspection mutated the batch");
            h.assertTrue(command(f,2,"start")==0&&admitted.equals(f.furnace().v5ServerRecord()),"Second consumer duplicated/replaced batch");
        }h.succeed();
    }
    @GameTest(template="empty",batch="admission_example") public static void permissionAndPreparationRejectionsRetainInputs(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var before=f.furnace().saveWithoutMetadata();
            h.assertTrue(command(f,0,"start")==0&&before.equals(f.furnace().saveWithoutMetadata()),"Non-operator started a batch");
            f.furnace().setItem(8,ItemStack.EMPTY);before=f.furnace().saveWithoutMetadata();
            h.assertTrue(command(f,2,"start")==0&&before.equals(f.furnace().saveWithoutMetadata()),"Addon bypassed furnace fire requirement");
        }h.succeed();
    }
    @GameTest(template="empty",batch="admission_example_faults") public static void addonLeavesCommittedFailureForOriginalHostRecovery(GameTestHelper h)throws Exception{
        try(var f=V5BatchGameTests.setup(h)){
            Path root=f.player().server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/alchemy-v5");
            String dimension=UUID.nameUUIDFromBytes(h.getLevel().dimension().location().toString().getBytes(StandardCharsets.UTF_8)).toString();
            Path link=root.resolve("positions/"+dimension+"_"+Long.toHexString(f.pos().asLong())+".nbt");
            Path backup=link.resolveSibling(link.getFileName()+".example-"+UUID.randomUUID());boolean existed=Files.exists(link);
            if(existed)Files.move(link,backup);Files.createDirectories(link);
            UUID original;
            try{
                h.assertTrue(command(f,2,"start")==0,"Failed publication was reported complete");
                original=f.furnace().saveWithoutMetadata().getUUID("V5Ref");
                h.assertTrue(command(f,2,"start")==0&&original.equals(f.furnace().saveWithoutMetadata().getUUID("V5Ref")),"Addon replay changed uncertain identity");
                var result=new cn.blockforge.generated.generatedmod.alchemy.batch.BatchStartResult(
                        cn.blockforge.generated.generatedmod.alchemy.batch.BatchStartResult.Reason.RECOVERY_REQUIRED,
                        cn.blockforge.generated.generatedmod.alchemy.batch.BatchStartResult.CommitStage.JOURNAL_CONFIRMED,original,"");
                h.assertTrue(AdmissionAddon.describe(result).contains(original.toString())&&AdmissionAddon.describe(result).contains("do not resubmit"),"Diagnostic lost recovery identity/instruction");
            }finally{Files.delete(link);if(existed)Files.move(backup,link);}
            new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(f.player().server).link(h.getLevel(),f.pos(),original);
            f.furnace().restoreV5();h.assertTrue(f.furnace().v5ServerRecord().getUUID("Id").equals(original)&&f.furnace().getItem(0).isEmpty(),"Original host could not recover addon-started batch");
        }h.succeed();
    }
}
