package cn.blockforge.generated.generatedmod.alchemy.batch;

import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** SERVER ONLY. Write-before-publish journal; never put its contents in a block update or item. */
public final class BatchJournal {
    public static final int MAX_BYTES=1048576;
    private final Path root;
    private final MinecraftServer server;
    private static final Map<MinecraftServer,Map<UUID,UUID>> ACTIVE=new WeakHashMap<>();
    public BatchJournal(MinecraftServer server){this.server=server;root=server.getWorldPath(LevelResource.ROOT).resolve("data/alchemy-v5");}
    private Path batch(UUID id){return root.resolve("batches").resolve(id+".nbt");}
    private Path position(ServerLevel level,BlockPos pos){
        return root.resolve("positions").resolve(UUID.nameUUIDFromBytes(level.dimension().location().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))+"_"+Long.toHexString(pos.asLong())+".nbt");
    }
    public CompoundTag read(UUID id){return readFile(batch(id));}
    public CompoundTag imported(UUID id){return readFile(root.resolve("imports").resolve(id+".nbt"));}
    public void imported(UUID id,CompoundTag doc){cn.blockforge.generated.generatedmod.alchemy.effects.LegacyPillImport.validate(doc);if(!doc.getUUID("Id").equals(id))throw new IllegalArgumentException("Import identity");writeFile(root.resolve("imports").resolve(id+".nbt"),doc);}
    public CompoundTag readDoseAuthority(UUID id){var normal=read(id);var imported=imported(id);if(!normal.isEmpty()&&!imported.isEmpty())throw new IllegalStateException("Ambiguous dose authority");return normal.isEmpty()?imported:normal;}
    public static void validateDoseAuthority(CompoundTag doc){if(doc.contains("Authority"))cn.blockforge.generated.generatedmod.alchemy.effects.LegacyPillImport.validate(doc);else V5Batch.validate(doc);}
    public void writeDoseAuthority(CompoundTag doc){if(doc.contains("Authority"))imported(doc.getUUID("Id"),doc);else write(doc);}
    private Path importIntentPath(UUID owner,int slot){if(slot<0||slot>40)throw new IllegalArgumentException("Inventory slot");return root.resolve("import-intents").resolve(owner+"_"+slot+".nbt");}
    public CompoundTag importIntent(UUID owner,int slot){return readFile(importIntentPath(owner,slot));}
    public void importIntent(UUID owner,int slot,CompoundTag intent){writeFile(importIntentPath(owner,slot),intent);}
    public void clearImportIntent(UUID owner,int slot){try{Files.deleteIfExists(importIntentPath(owner,slot));}catch(IOException error){throw new IllegalStateException("Cannot close import intent",error);}}
    public CompoundTag certification(UUID owner){return readFile(root.resolve("certification").resolve(owner+".nbt"));}
    public void certification(UUID owner,CompoundTag data){writeFile(root.resolve("certification").resolve(owner+".nbt"),data);}
    public java.util.List<CompoundTag> ownerHistory(UUID owner){
        var directory=root.resolve("batches");if(!Files.isDirectory(directory))return java.util.List.of();
        try(var paths=Files.list(directory)){var files=paths.filter(p->p.getFileName().toString().endsWith(".nbt")).limit(8193).toList();if(files.size()>8192)throw new IllegalStateException("Certification archive budget exceeded; archive review required");var out=new java.util.ArrayList<CompoundTag>();for(var p:files){var n=readFile(p);if(n.hasUUID("Owner")&&n.getUUID("Owner").equals(owner)){V5Batch.validate(n);out.add(n);}}return java.util.List.copyOf(out);}catch(IOException e){throw new IllegalStateException("Cannot audit batch archive",e);}
    }
    public void write(CompoundTag data){V5Batch.validate(data);PortableBatch.validate(data);cn.blockforge.generated.generatedmod.alchemy.research.AlchemyCertification.reserve(server,this,data);writeFile(batch(data.getUUID("Id")),data);
        cn.blockforge.generated.generatedmod.alchemy.research.AlchemyCertification.record(server,this,data);
        cn.blockforge.generated.generatedmod.alchemy.research.ResearchPractice.record(server,data);
        var cached=ACTIVE.get(server);if(cached!=null&&(!data.getString("State").equals("ACTIVE")||data.getBoolean("Detached")))cached.remove(data.getUUID("Id"));
    }
    public CompoundTag at(ServerLevel level,BlockPos pos){var link=readFile(position(level,pos));return link.hasUUID("Id")?read(link.getUUID("Id")):new CompoundTag();}
    public void link(ServerLevel level,BlockPos pos,UUID id){var tag=new CompoundTag();tag.putUUID("Id",id);writeFile(position(level,pos),tag);
        var cached=ACTIVE.get(server);if(cached!=null){var doc=read(id);if(doc.getString("State").equals("ACTIVE")&&!doc.getBoolean("Detached"))cached.put(id,doc.getUUID("Owner"));}
    }
    public int activeCount(UUID owner){
        var cached=ACTIVE.get(server);if(cached==null){
            cached=new HashMap<>();var directory=root.resolve("positions");
            if(Files.isDirectory(directory))try(var paths=Files.list(directory)){
                var files=paths.filter(p->p.getFileName().toString().endsWith(".nbt")).limit(4097).toList();if(files.size()>4096)throw new IllegalStateException("Furnace position budget exceeded");
                for(var file:files){var link=readFile(file);if(!link.hasUUID("Id"))throw new IllegalStateException("Invalid position index");var doc=read(link.getUUID("Id"));
                    if(doc.getString("State").equals("ACTIVE")&&!doc.getBoolean("Detached"))cached.put(doc.getUUID("Id"),doc.getUUID("Owner"));}
            }catch(IOException e){throw new IllegalStateException("Cannot rebuild active V5 index",e);}
            var portable=root.resolve("portable");
            if(Files.isDirectory(portable))try(var paths=Files.list(portable)){
                var files=paths.filter(p->p.getFileName().toString().endsWith(".nbt")).limit(4097).toList();if(files.size()>4096)throw new IllegalStateException("Portable index budget exceeded");
                for(var file:files){var link=readFile(file);if(!link.hasUUID("Id"))throw new IllegalStateException("Invalid portable index");var doc=read(link.getUUID("Id"));if(doc.getString("State").equals("ACTIVE"))cached.put(doc.getUUID("Id"),doc.getUUID("Owner"));}
            }catch(IOException e){throw new IllegalStateException("Cannot rebuild portable V5 index",e);}
            ACTIVE.put(server,cached);
        }
        int count=0;for(UUID id:cached.values())if(id.equals(owner))count++;return count;
    }
    public CompoundTag portable(UUID owner){var link=readFile(root.resolve("portable").resolve(owner+".nbt"));return link.hasUUID("Id")?read(link.getUUID("Id")):new CompoundTag();}
    public void linkPortable(UUID owner,UUID id){var link=new CompoundTag();link.putUUID("Id",id);writeFile(root.resolve("portable").resolve(owner+".nbt"),link);
        var cached=ACTIVE.get(server);if(cached!=null){var doc=read(id);if(doc.getString("State").equals("ACTIVE"))cached.put(id,owner);}
    }
    private Path pending(UUID player){return root.resolve("deliveries").resolve(player+".nbt");}
    public Set<UUID> pendingFor(UUID player){var result=new LinkedHashSet<UUID>();var tag=readFile(pending(player));
        for(Tag entry:tag.getList("Ids",Tag.TAG_COMPOUND)){var n=(CompoundTag)entry;if(!n.hasUUID("Id")||!result.add(n.getUUID("Id")))throw new IllegalArgumentException("Invalid delivery index");}
        if(result.size()>256)throw new IllegalArgumentException("Delivery budget exceeded");return result;
    }
    public void pending(UUID player,UUID batch,boolean add){var ids=pendingFor(player);if(add)ids.add(batch);else ids.remove(batch);if(ids.size()>256)throw new IllegalArgumentException("Delivery budget exceeded");
        var tag=new CompoundTag();var list=new ListTag();for(var id:ids){var n=new CompoundTag();n.putUUID("Id",id);list.add(n);}tag.put("Ids",list);writeFile(pending(player),tag);
    }
    public Set<UUID> pendingDoses(UUID player){
        var ids=new LinkedHashSet<UUID>();var tag=readFile(root.resolve("doses").resolve(player+".nbt"));
        for(var entry:tag.getList("Ids",Tag.TAG_COMPOUND)){var n=(CompoundTag)entry;if(!n.hasUUID("Id")||!ids.add(n.getUUID("Id")))throw new IllegalArgumentException("Invalid dose index");}
        if(ids.size()>256)throw new IllegalArgumentException("Dose index budget");return ids;
    }
    public void pendingDose(UUID player,UUID batch,boolean add){
        var ids=pendingDoses(player);if(add)ids.add(batch);else ids.remove(batch);if(ids.size()>256)throw new IllegalArgumentException("Dose index budget");
        var n=new CompoundTag();var list=new ListTag();for(var id:ids){var entry=new CompoundTag();entry.putUUID("Id",id);list.add(entry);}n.put("Ids",list);writeFile(root.resolve("doses").resolve(player+".nbt"),n);
    }
    private CompoundTag readFile(Path path){
        if(!Files.exists(path))return new CompoundTag();
        try{if(Files.size(path)>MAX_BYTES)throw new IOException("Journal byte budget exceeded");
            // NBT reads many primitive fields; buffering avoids a filesystem read per
            // byte/primitive while retaining the same byte budget and trailing-data check.
            try(var input=new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))){var tag=NbtIo.read(input,new NbtAccounter(MAX_BYTES*4L));if(tag==null||input.read()!=-1)throw new IOException("Invalid/trailing journal data");return tag;}
        }catch(IOException e){throw new IllegalStateException("Cannot read V5 journal "+path.getFileName(),e);}
    }
    private void writeFile(Path path,CompoundTag tag){
        Path temporary=path.resolveSibling(path.getFileName()+"."+UUID.randomUUID()+".tmp");
        try{
            var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){NbtIo.write(tag,out);}if(bytes.size()>MAX_BYTES)throw new IOException("Journal byte budget exceeded");
            Files.createDirectories(path.getParent());try(var channel=FileChannel.open(temporary,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
                var data=ByteBuffer.wrap(bytes.toByteArray());while(data.hasRemaining())channel.write(data);channel.force(true);
            }
            // No non-atomic fallback: on unsupported filesystems retain the previous journal and stop.
            for(int attempt=0;;attempt++)try{
                Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);break;
            }catch(AccessDeniedException denied){
                // Windows scanners may briefly hold a target without delete sharing. Keep the same
                // forced temp file and retry only atomic replacement, bounded to 30 ms total.
                if(attempt>=3)throw denied;
                java.util.concurrent.locks.LockSupport.parkNanos((attempt+1)*5000000L);
            }
        }catch(IOException e){throw new IllegalStateException("Cannot commit V5 journal "+path.getFileName(),e);}
    }
}
