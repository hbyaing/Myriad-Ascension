package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.alchemy.batch.*;
import cn.blockforge.generated.generatedmod.alchemy.simulation.AlchemySimulation;
import com.google.gson.GsonBuilder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Test-source-only observation: production persistence and normal server ticks remain unchanged. */
@GameTestHolder("wuxingxiantu_entry") @PrefixGameTestTemplate(false)
public final class ArchitectureIoGameTests {
    @GameTest(template="empty",batch="architecture_live_io",timeoutTicks=650)
    public static void completedFurnaceAndReceiptIoLedger(GameTestHelper h)throws Exception{
        var f=V5BatchGameTests.setup(h);var recording=new Recording();
        f.player().cleanupAfter(h,()->{try{recording.close();}finally{f.close();}});
        for(int x:new int[]{-2,2})for(int z:new int[]{-2,2})
            f.player().keepTicking(h.getLevel(),f.pos().offset(x,0,z));
        recording.enable("jdk.FileRead").withThreshold(Duration.ZERO).withStackTrace();
        recording.enable("jdk.FileWrite").withThreshold(Duration.ZERO).withStackTrace();
        recording.start();long started=System.nanoTime(),startWorld=h.getLevel().getGameTime();
        var admission=f.furnace().tryStartV5Batch(f.player(),null);long admissionNanos=System.nanoTime()-started;
        h.assertTrue(admission.started(),"Measured admission failed: "+admission);
        h.runAfterDelay(470,()->{
            try{
                var doc=f.furnace().v5ServerRecord();var core=V5Batch.core(doc);
                h.assertTrue(doc.getString("State").equals("READY")&&core.status()==AlchemySimulation.Status.FINISHED,
                        "Measured live batch did not finish: "+core.status()+" / "+core.reason());
                long claimStart=System.nanoTime();int received=f.furnace().collectResult(f.player());long claimNanos=System.nanoTime()-claimStart;
                h.assertTrue(received==1,"Measured claim failed");
                var journal=new BatchJournal(f.player().server);
                h.assertTrue(journal.read(admission.batchId()).getString("State").equals("CLAIMED")
                        &&journal.pendingFor(f.player().getUUID()).isEmpty(),"Receipt was not durably acknowledged");
                long elapsed=System.nanoTime()-started;recording.stop();
                Path directory=Path.of("architecture-io").toAbsolutePath();Files.createDirectories(directory);
                Path jfr=directory.resolve("ordinary-furnace-"+admission.batchId()+".jfr");recording.dump(jfr);
                var groups=new TreeMap<String,Map<String,Object>>();
                for(var event:RecordingFile.readAllEvents(jfr)){
                    String type=event.getEventType().getName();
                    if(!type.equals("jdk.FileRead")&&!type.equals("jdk.FileWrite"))continue;
                    String path=event.getString("path");if(path==null)continue;path=path.replace('\\','/');
                    String group=classify(path,f.player().getUUID().toString());if(group==null)continue;
                    String key=group+"/"+type;
                    var row=groups.computeIfAbsent(key,k->{var m=new LinkedHashMap<String,Object>();m.put("events",0L);m.put("bytes",0L);m.put("totalNanos",0L);m.put("maxNanos",0L);return m;});
                    long duration=event.getDuration().toNanos();
                    row.put("events",(long)row.get("events")+1);row.put("bytes",(long)row.get("bytes")+Math.max(0,event.getLong(type.equals("jdk.FileRead")?"bytesRead":"bytesWritten")));
                    row.put("totalNanos",(long)row.get("totalNanos")+duration);row.put("maxNanos",Math.max((long)row.get("maxNanos"),duration));
                }
                var report=new LinkedHashMap<String,Object>();report.put("schema",1);report.put("batchId",admission.batchId().toString());
                report.put("worldTicks",h.getLevel().getGameTime()-startWorld);report.put("reactionSteps",core.tick());
                report.put("admissionNanos",admissionNanos);report.put("claimNanos",claimNanos);report.put("elapsedNanos",elapsed);
                report.put("io",groups);report.put("jfr",jfr.toString());
                report.put("scope","One ordinary 4-herb furnace, real 470-tick lifecycle and claim, instrumented Java 17 GameTest. File events exclude force latency; phase wall times include it. No production timing instrumentation, throughput or uninstrumented performance claim.");
                Path output=directory.resolve("ordinary-furnace-"+admission.batchId()+".json");
                Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(report));
                com.mojang.logging.LogUtils.getLogger().info("Architecture live I/O report: {}",output);h.succeed();
            }catch(Exception error){throw new IllegalStateException("Cannot complete measured lifecycle",error);}
        });
    }
    private static String classify(String path,String owner){
        int index=path.indexOf("/data/alchemy-v5/");
        if(index>=0){String rest=path.substring(index+17);int slash=rest.indexOf('/');return "alchemy-v5/"+(slash<0?"root":rest.substring(0,slash));}
        if(path.contains("/playerdata/"+owner+".dat"))return "measured-player";
        if(path.endsWith(".mca"))return "world-region-all-loaded-chunks";
        return null;
    }
}
