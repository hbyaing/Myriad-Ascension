package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.alchemy.batch.*;
import cn.blockforge.generated.generatedmod.alchemy.batch.BatchStartResult.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

@GameTestHolder("wuxingxiantu_entry") @PrefixGameTestTemplate(false)
public final class V5AdmissionGameTests {
    @GameTest(template="empty") public static void missingFireIsExplicitAndDoesNotMutateInputs(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            f.furnace().setItem(8,ItemStack.EMPTY);
            var before=f.furnace().saveWithoutMetadata();
            var result=f.furnace().tryStartV5Batch(f.player(),null);
            h.assertTrue(result.reason()==Reason.FIRE_REQUIRED&&result.stage()==CommitStage.NOT_ATTEMPTED
                    &&result.batchId()==null&&!result.needsRecovery(),"Missing fire was not a no-commit rejection");
            h.assertTrue(before.equals(f.furnace().saveWithoutMetadata()),"Rejection changed furnace");
        }h.succeed();
    }
    @GameTest(template="empty") public static void unsupportedMaterialFailsBeforeCommitWithoutMintingIdentity(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            f.furnace().setItem(1,new ItemStack(Items.APPLE));var before=f.furnace().saveWithoutMetadata();
            var result=f.furnace().tryStartV5Batch(f.player(),null);
            h.assertTrue(result.reason()==Reason.PREPARATION_FAILED&&result.stage()==CommitStage.NOT_ATTEMPTED
                    &&result.batchId()==null&&!result.detail().isBlank(),"Preparation failure was reported as a submitted batch");
            h.assertTrue(before.equals(f.furnace().saveWithoutMetadata()),"Preparation failure consumed inputs");
        }h.succeed();
    }
    @GameTest(template="empty") public static void successReturnsJournalIdentityAndRepeatedStartIsOccupied(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var result=f.furnace().tryStartV5Batch(f.player(),null);
            h.assertTrue(result.started()&&result.stage()==CommitStage.COMPLETED,"Start not completed");
            var original=f.furnace().v5ServerRecord();
            h.assertTrue(result.batchId().equals(original.getUUID("Id")),"Returned a different batch identity");
            var duplicate=f.furnace().tryStartV5Batch(f.player(),null);
            h.assertTrue(duplicate.reason()==Reason.FURNACE_OCCUPIED&&!duplicate.started()
                    &&original.equals(f.furnace().v5ServerRecord()),"Repeat start changed admitted batch");
        }h.succeed();
    }
    @GameTest(template="empty",batch="v5_admission_faults") public static void journalFailureKeepsUnconfirmedIdentityAndUntouchedInputs(GameTestHelper h)throws Exception{
        try(var f=V5BatchGameTests.setup(h)){
            Path file=f.player().server.getWorldPath(LevelResource.ROOT).resolve("data/alchemy-v5/certification/"+f.player().getUUID()+".nbt");
            h.assertTrue(!Files.exists(file),"Unexpected certification fixture");
            Files.createDirectories(file);
            try{
                var before=f.furnace().saveWithoutMetadata();
                var result=f.furnace().tryStartV5Batch(f.player(),null);
                h.assertTrue(result.needsRecovery()&&result.stage()==CommitStage.UNCONFIRMED&&result.batchId()!=null,
                        "Attempted journal operation was misreported as safe to retry");
                assertQuarantinedInputs(h,f,before,result);
            }finally{Files.delete(file);}
        }h.succeed();
    }
    @GameTest(template="empty",batch="v5_admission_faults") public static void failedPositionPublicationReturnsCommittedBatchAndCanResumeSameIdentity(GameTestHelper h)throws Exception{
        try(var f=V5BatchGameTests.setup(h)){
            Path root=f.player().server.getWorldPath(LevelResource.ROOT).resolve("data/alchemy-v5");
            String dimension=UUID.nameUUIDFromBytes(h.getLevel().dimension().location().toString().getBytes(StandardCharsets.UTF_8)).toString();
            Path link=root.resolve("positions/"+dimension+"_"+Long.toHexString(f.pos().asLong())+".nbt");
            Path backup=link.resolveSibling(link.getFileName()+".fixture-"+UUID.randomUUID());
            boolean existed=Files.exists(link);if(existed)Files.move(link,backup);
            BatchStartResult result;
            var before=f.furnace().saveWithoutMetadata();
            Files.createDirectories(link);
            try{
                result=f.furnace().tryStartV5Batch(f.player(),null);
                h.assertTrue(result.needsRecovery()&&result.stage()==CommitStage.JOURNAL_CONFIRMED
                        &&result.batchId()!=null,"Committed batch was misreported as an ordinary rejection");
                var journal=new BatchJournal(f.player().server);
                h.assertTrue(!journal.read(result.batchId()).isEmpty(),"Committed journal was lost");
                assertQuarantinedInputs(h,f,before,result);
            }finally{Files.delete(link);if(existed)Files.move(backup,link);}
            var journal=new BatchJournal(f.player().server);
            journal.link(h.getLevel(),f.pos(),result.batchId());f.furnace().restoreV5();
            var restored=f.furnace().v5ServerRecord();
            h.assertTrue(restored.getUUID("Id").equals(result.batchId())&&f.furnace().getItem(0).isEmpty(),
                    "Host recovery replaced identity or restored consumed material");
            f.furnace().restoreV5();
            h.assertTrue(restored.equals(f.furnace().v5ServerRecord()),"Recovery changed the batch twice");
        }h.succeed();
    }
    private static void assertQuarantinedInputs(GameTestHelper h,V5BatchGameTests.Fixture f,
            net.minecraft.nbt.CompoundTag before,BatchStartResult original){
        var saved=f.furnace().saveWithoutMetadata();
        h.assertTrue(before.getList("Items",net.minecraft.nbt.Tag.TAG_COMPOUND).equals(saved.getList("Items",net.minecraft.nbt.Tag.TAG_COMPOUND))
                &&before.getInt("SpiritFuel")==saved.getInt("SpiritFuel"),"Unpublished fault changed actual input inventory/fuel");
        h.assertTrue(saved.hasUUID("V5Ref")&&saved.getUUID("V5Ref").equals(original.batchId()),"Original uncertain identity not retained");
        h.assertTrue(f.furnace().tryStartV5Batch(f.player(),null).reason()==Reason.FURNACE_OCCUPIED,
                "Unresolved admission allowed another batch from the same inputs");
        f.furnace().load(saved);
        h.assertTrue(f.furnace().tryStartV5Batch(f.player(),null).reason()==Reason.FURNACE_OCCUPIED
                &&f.furnace().saveWithoutMetadata().getUUID("V5Ref").equals(original.batchId()),
                "Block-entity reload lost admission quarantine/identity");
    }
}
