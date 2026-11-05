package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.util.Arrays;

@GameTestHolder("wuxingxiantu_entry") @PrefixGameTestTemplate(false)
public final class V5JournalReadGameTests {
    @GameTest(template="empty",batch="v5_journal_read_faults")
    public static void bufferedReaderRetainsCorruptionAndByteBudgetRejection(GameTestHelper h)throws Exception{
        try(var f=V5BatchGameTests.setup(h)){
            var admission=f.furnace().tryStartV5Batch(f.player(),null);h.assertTrue(admission.started(),"Fixture start failed");
            var journal=new BatchJournal(f.player().server);var original=journal.read(admission.batchId());
            Path file=f.player().server.getWorldPath(LevelResource.ROOT).resolve("data/alchemy-v5/batches/"+admission.batchId()+".nbt");
            byte[] bytes=Files.readAllBytes(file);
            try{
                for(byte[] invalid:new byte[][]{Arrays.copyOf(bytes,bytes.length+1),Arrays.copyOf(bytes,1),new byte[BatchJournal.MAX_BYTES+1]}){
                    Files.write(file,invalid);boolean rejected=false;
                    try{journal.read(admission.batchId());}catch(IllegalStateException expected){rejected=true;}
                    h.assertTrue(rejected,"Invalid/truncated/oversized journal accepted");
                }
            }finally{Files.write(file,bytes);}
            h.assertTrue(journal.read(admission.batchId()).equals(original),"Restored checkpoint changed identity or payload");
        }h.succeed();
    }
}
