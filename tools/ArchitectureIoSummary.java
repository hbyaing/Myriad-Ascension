import jdk.jfr.consumer.RecordingFile;
import java.nio.file.*;
import java.util.*;

/** JDK-only summary of the measured batch vs other archive activity; does not launch Minecraft. */
class ArchitectureIoSummary {
    public static void main(String[] args)throws Exception{
        if(args.length!=3)throw new IllegalArgumentException("recording.jfr batch-uuid output.json");
        var groups=new TreeMap<String,long[]>();
        for(var event:RecordingFile.readAllEvents(Path.of(args[0]))){
            String type=event.getEventType().getName();
            if(!type.equals("jdk.FileRead")&&!type.equals("jdk.FileWrite"))continue;
            String path=event.getString("path");if(path==null)continue;path=path.replace('\\','/');
            String group;
            if(path.contains("/batches/"+args[1]+".nbt"))group="measured-batch";
            else if(path.contains("/data/alchemy-v5/batches/"))group="other-batches";
            else if(path.contains("/data/alchemy-v5/"))group="other-journal";
            else if(path.contains("/playerdata/")&&(path.endsWith(".dat")||path.endsWith(".dat_old")||path.endsWith(".dat.tmp")))group="player-files";
            else if(path.endsWith(".mca"))group="world-region";
            else continue;
            long[] row=groups.computeIfAbsent(group+"/"+type,k->new long[4]);
            row[0]++;row[1]+=Math.max(0,event.getLong(type.equals("jdk.FileRead")?"bytesRead":"bytesWritten"));
            long duration=event.getDuration().toNanos();row[2]+=duration;row[3]=Math.max(row[3],duration);
        }
        var json=new StringBuilder("{\n");boolean first=true;
        for(var entry:groups.entrySet()){
            if(!first)json.append(",\n");first=false;var r=entry.getValue();
            json.append("  \"").append(entry.getKey()).append("\": {\"events\": ").append(r[0])
                    .append(", \"bytes\": ").append(r[1]).append(", \"totalNanos\": ").append(r[2])
                    .append(", \"maxNanos\": ").append(r[3]).append("}");
        }
        json.append("\n}\n");var output=Path.of(args[2]);Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output,json);System.out.print(json);
    }
}
