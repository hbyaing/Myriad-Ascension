package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.YujianClientController;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig;
import cn.blockforge.generated.generatedmod.yujian.YujianPhase;
import com.google.gson.GsonBuilder;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Isolated ordinary-stone wall fixture. Observes production camera/overlay code; never changes it. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class HuahongWallQa {
    static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.huahongWallQa");
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.huahongOutput","../../reports/huahong-client"));
    private static final List<Map<String,Object>> SERVER=Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String,Object>> CLIENT=Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String,Object>> FRAMES=Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String,Object>> OVERLAYS=Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String,Object>> PHOTOS=Collections.synchronizedList(new ArrayList<>());
    private static final List<CompletableFuture<Void>> IMAGE_WRITES=new ArrayList<>();
    private static final Set<String> PHOTO_NAMES=new HashSet<>();
    private static final Set<Long> thinOverlayFrames=new HashSet<>(),thickOverlayFrames=new HashSet<>(),exitOverlayFrames=new HashSet<>();
    private static volatile Wall thin,thick;
    private static volatile boolean finishing;
    private static volatile int currentPhase;
    private static long frameSequence;
    private static long lastFrameNanos;
    private static int thirdSince=Integer.MIN_VALUE;
    private static Integer originalFrameLimit;
    private static volatile boolean brakeObserved,brakeBeganInside;
    private static volatile int brakeStartingStage;
    private static CameraType originalCamera,lastCamera;
    private static ExecutorService imageWriter;
    private static Map<String,Object> verification=Map.of("passed",false,"status","not verified");

    private static final class Wall {
        final String name;final ServerLevel level;final AABB bounds;final int impact,blocks,fullChunks;
        final float initialHealth;
        final LinkedHashMap<BlockPos,BlockState> old=new LinkedHashMap<>();
        volatile boolean restored;
        Wall(String name,ServerPlayer player,ControlledSwordEntity sword,int length){
            this.name=name;level=player.serverLevel();impact=sword.impact();initialHealth=player.getHealth();
            int cx=BlockPos.containing(sword.position()).getX(),y=BlockPos.containing(sword.position()).getY();
            int front=BlockPos.containing(sword.position().add(0,0,2)).getZ();
            if(y+2>=level.getMaxBuildHeight())throw new AssertionError("Wall fixture is outside build height");
            bounds=new AABB(cx-1,y,front,cx+2,y+3,front+length);blocks=length*9;
            if(blocks>4608)throw new AssertionError("Wall fixture exceeds 4608 blocks");
            Set<Long> chunks=new HashSet<>();
            for(int x=(cx-1)>>4;x<=(cx+1)>>4;x++)for(int z=front>>4;z<=(front+length-1)>>4;z++){
                // Explicit synchronous QA preparation, separate from cold/MSPT acceptance.
                level.getChunk(x,z);chunks.add(net.minecraft.world.level.ChunkPos.asLong(x,z));
            }
            fullChunks=chunks.size();
            for(int x=cx-1;x<=cx+1;x++)for(int dy=0;dy<3;dy++)for(int z=front;z<front+length;z++){
                BlockPos p=new BlockPos(x,y+dy,z);BlockState state=level.getBlockState(p);
                if(state.hasBlockEntity())throw new AssertionError("Refusing to overwrite a block entity in wall fixture: "+p);
                old.put(p,state);
            }
            for(var p:old.keySet())level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
        }
        void restore(){
            if(restored)return;
            for(var entry:old.entrySet())level.setBlock(entry.getKey(),entry.getValue(),2);
            for(var entry:old.entrySet())if(!level.getBlockState(entry.getKey()).equals(entry.getValue()))
                throw new AssertionError("Wall block restoration did not read back: "+entry.getKey());
            restored=true;
        }
        Map<String,Object> info(){return Map.of("name",name,"bounds",List.of(bounds.minX,bounds.minY,bounds.minZ,bounds.maxX,bounds.maxY,bounds.maxZ),
                "blocks",blocks,"explicitFullChunks",fullChunks,"preparation","Synchronous fixture-only FULL preparation; not cold-streaming/MSPT evidence", "restored",restored,"initialHealth",initialHealth);}
    }

    static boolean cameraActive(){return ENABLED;}
    static void beforeInput(ServerPlayer player,ControlledSwordEntity sword,int phase){
        if(!ENABLED||finishing)return;
        if(!player.serverLevel().getServer().isSameThread())throw new AssertionError("Wall setup must run on the server thread");
        // At 30 blocks/tick and 30 FPS a 20-block wall offers only one
        // inside render. 96 retains at least four real inside frames; the
        // separate server GameTest covers passage through a four-block wall.
        if(phase>=100&&phase<130&&thin==null)thin=new Wall("thin",player,sword,96);
        if(phase>=130&&thin!=null)thin.restore();
        if(phase>=145&&phase<200&&thick==null)thick=new Wall("thick",player,sword,512);
        if(phase==160&&!brakeObserved){
            brakeObserved=true;brakeStartingStage=sword.huahongStage();
            brakeBeganInside=player.serverLevel().getBlockCollisions(player,player.getBoundingBox()).iterator().hasNext()
                    ||player.serverLevel().getBlockCollisions(sword,sword.getBoundingBox()).iterator().hasNext();
        }
        if(phase>=200&&thick!=null)thick.restore();
    }

    static void clientTick(Minecraft mc,int phase){
        if(!ENABLED)return;currentPhase=phase;
        if(mc.player==null||mc.level==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return;
        if(originalCamera==null)originalCamera=mc.options.getCameraType();
        if(originalFrameLimit==null)originalFrameLimit=mc.options.framerateLimit().get();
        mc.options.framerateLimit().set(160);
        // The existing QA input mixin suppresses the production input tick;
        // activate only its real camera lifecycle, without synthesizing input.
        YujianClientController.setActive(true);
        boolean first=phase<140?phase>=90&&thinOverlayFrames.size()<4:thickOverlayFrames.size()<4;
        if(phase>=160&&phase<200&&exitOverlayFrames.isEmpty())first=true;
        CameraType wanted=first?CameraType.FIRST_PERSON:CameraType.THIRD_PERSON_BACK;
        mc.options.setCameraType(wanted);
        if(lastCamera!=wanted){lastCamera=wanted;thirdSince=first?Integer.MIN_VALUE:mc.player.tickCount;}
        Map<String,Object> row=pose(mc,sword,phase);row.put("kind","client-tick");CLIENT.add(row);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
    public static void overlay(RenderBlockScreenEffectEvent event){
        if(!ENABLED||event.getOverlayType()!=RenderBlockScreenEffectEvent.OverlayType.BLOCK)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||event.getPlayer()!=mc.player||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return;
        long frame=frameSequence+1;boolean inside=inside(mc,sword),exit=sword.huahongStage()==0||sword.huahongStage()==3;
        Map<String,Object> row=new LinkedHashMap<>();row.put("frame",frame);row.put("phase",currentPhase);row.put("stage",sword.huahongStage());
        row.put("inside",inside);row.put("bufferedPhasing",sword.bufferedPhasing());row.put("canceled",event.isCanceled());
        row.put("cameraType",mc.options.getCameraType().name());row.put("block",event.getBlockState().toString());
        row.put("blockPos",List.of(event.getBlockPos().getX(),event.getBlockPos().getY(),event.getBlockPos().getZ()));OVERLAYS.add(row);
        if(inside&&event.isCanceled()){
            if(currentPhase<140)thinOverlayFrames.add(frame);else thickOverlayFrames.add(frame);
            if(currentPhase>=160&&exit)exitOverlayFrames.add(frame);
        }
    }

    static void frame(Minecraft mc,int phase){
        if(!ENABLED)return;currentPhase=phase;frameSequence++;
        if(mc.player==null||mc.level==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return;
        Map<String,Object> row=pose(mc,sword,phase);row.put("frame",frameSequence);
        long nanos=System.nanoTime();double delta=lastFrameNanos==0?0:(nanos-lastFrameNanos)/1e9;lastFrameNanos=nanos;
        row.put("realFrameDeltaSeconds",delta);row.put("realFps",delta>0?1/delta:0);row.put("qaFrameLimit",mc.options.framerateLimit().get());
        boolean third=mc.options.getCameraType()==CameraType.THIRD_PERSON_BACK;
        boolean settled=third&&thirdSince!=Integer.MIN_VALUE&&mc.player.tickCount-thirdSince>=3;
        row.put("thirdPersonSettled",settled);row.put("thinCanceledOverlayFrames",thinOverlayFrames.size());
        row.put("thickCanceledOverlayFrames",thickOverlayFrames.size());row.put("exitCanceledOverlayFrames",exitOverlayFrames.size());FRAMES.add(row);
        boolean inside=Boolean.TRUE.equals(row.get("inside"));
        if(phase>=95&&phase<100)photo(mc,row,"thin-approach");
        if(phase>=100&&phase<130){
            if(inside&&mc.options.getCameraType().isFirstPerson()&&!thinOverlayFrames.isEmpty())photo(mc,row,"thin-first-inside-canceled");
            if(settled)photo(mc,row,"thin-third-distance");
        }
        if(phase>=140&&phase<145)photo(mc,row,"thick-approach");
        if(phase>=145&&phase<160){
            if(inside&&mc.options.getCameraType().isFirstPerson()&&!thickOverlayFrames.isEmpty())photo(mc,row,"thick-first-inside-canceled");
            if(settled)photo(mc,row,"thick-third-distance");
        }
        if(phase>=160&&phase<200&&inside&&(sword.huahongStage()==0||sword.huahongStage()==3)
                &&mc.options.getCameraType().isFirstPerson()&&!exitOverlayFrames.isEmpty())photo(mc,row,"thick-exit-inside-canceled");
        if(phase>=205&&!inside&&settled)photo(mc,row,"thick-exit-safe-third");
    }

    static void serverRow(ServerPlayer player,ControlledSwordEntity sword,Map<String,Object> row,int phase){
        if(!ENABLED)return;Wall wall=phase<140?thin:thick;
        boolean inside=player.serverLevel().getBlockCollisions(player,player.getBoundingBox()).iterator().hasNext()
                ||player.serverLevel().getBlockCollisions(sword,sword.getBoundingBox()).iterator().hasNext();
        Map<String,Object> sample=new LinkedHashMap<>();sample.put("phase",phase);sample.put("worldTick",player.serverLevel().getGameTime());
        sample.put("rootTick",sword.tickCount);sample.put("playerTick",player.tickCount);sample.put("stage",sword.huahongStage());
        sample.put("advancedPhase",sword.advancedPhase().name());sample.put("phasesSolidBlocks",sword.phasesSolidBlocks());
        sample.put("strength",sword.huahongStrength());sample.put("speed",sword.flightSpeed());sample.put("position",position(sword.position()));
        sample.put("health",player.getHealth());sample.put("invulnerable",player.isInvulnerable());sample.put("creative",player.isCreative());
        sample.put("inside",inside);sample.put("impact",sword.impact());sample.put("wall",wall==null?"not-built":wall.name);
        sample.put("wallRestored",wall!=null&&wall.restored);sample.put("riderInWallBounds",wall!=null&&wall.bounds.intersects(player.getBoundingBox()));
        row.put("wallQa",sample);SERVER.add(sample);
    }

    private static boolean inside(Minecraft mc,ControlledSwordEntity sword){
        return mc.level.getBlockCollisions(mc.player,mc.player.getBoundingBox()).iterator().hasNext()
                ||mc.level.getBlockCollisions(sword,sword.getBoundingBox()).iterator().hasNext();
    }
    private static List<Double> position(Vec3 p){return List.of(p.x,p.y,p.z);}
    private static Map<String,Object> pose(Minecraft mc,ControlledSwordEntity sword,int phase){
        Map<String,Object> row=new LinkedHashMap<>();row.put("phase",phase);row.put("stage",sword.huahongStage());row.put("strength",sword.huahongStrength());
        row.put("advancedPhase",sword.advancedPhase().name());row.put("bufferedPhasing",sword.bufferedPhasing());row.put("inside",inside(mc,sword));
        row.put("rootTick",sword.tickCount);row.put("playerTick",mc.player.tickCount);row.put("position",position(sword.position()));
        var camera=mc.gameRenderer.getMainCamera();row.put("cameraPosition",position(camera.getPosition()));row.put("cameraType",mc.options.getCameraType().name());
        row.put("productionCameraDistance",YujianClientController.cameraDistance());row.put("productionCameraActive",YujianClientController.active());
        BlockPos eye=BlockPos.containing(camera.getPosition());row.put("cameraInsideSolid",mc.level.getBlockState(eye).isSolidRender(mc.level,eye));return row;
    }

    private static void photo(Minecraft mc,Map<String,Object> row,String name){
        if(!PHOTO_NAMES.add(name))return;
        if(imageWriter==null)imageWriter=Executors.newSingleThreadExecutor(task->{Thread t=new Thread(task,"Huahong wall image writer");t.setDaemon(true);return t;});
        var image=Screenshot.takeScreenshot(mc.getMainRenderTarget());Map<String,Object> record=new LinkedHashMap<>(row);
        record.put("image","wall-"+name+".png");PHOTOS.add(record);
        IMAGE_WRITES.add(CompletableFuture.runAsync(()->{try(image){Files.createDirectories(OUT);image.writeToFile(OUT.resolve("wall-"+name+".png"));}
            catch(Exception e){throw new java.util.concurrent.CompletionException(e);}},imageWriter));
    }
    private static List<Map<String,Object>> snapshot(List<Map<String,Object>> rows){synchronized(rows){return List.copyOf(rows);}}
    private static int phase(Map<String,Object> row){return ((Number)row.get("phase")).intValue();}
    private static int stage(Map<String,Object> row){return ((Number)row.get("stage")).intValue();}
    private static boolean exited(Map<String,Object> row){return stage(row)==0||stage(row)==3;}

    static Map<String,Object> verify(){
        List<String> failures=new ArrayList<>();var server=snapshot(SERVER);var frames=snapshot(FRAMES);var overlays=snapshot(OVERLAYS);
        if(thin==null||thick==null)failures.add("Both real walls were not built");
        if(thin!=null&&!thin.restored||thick!=null&&!thick.restored)failures.add("Fixture blocks were not restored");
        boolean thinInside=server.stream().anyMatch(r->phase(r)>=100&&phase(r)<130&&stage(r)==2
                &&!YujianPhase.DUNXU.name().equals(r.get("advancedPhase"))&&Boolean.TRUE.equals(r.get("inside")));
        boolean thinPassed=thin!=null&&server.stream().anyMatch(r->phase(r)>=100&&phase(r)<130&&stage(r)==2
                &&!YujianPhase.DUNXU.name().equals(r.get("advancedPhase"))&&((Number)((List<?>)r.get("position")).get(2)).doubleValue()>thin.bounds.maxZ
                &&((Number)r.get("impact")).intValue()==thin.impact);
        if(!thinInside||!thinPassed)failures.add("Non-DUNXU stage2 did not really enter and pass the thin ordinary stone wall without impact");
        if(thin!=null&&server.stream().anyMatch(r->phase(r)>=100&&phase(r)<130&&((Number)r.get("impact")).intValue()!=thin.impact))failures.add("Thin wall caused an impact");
        if(server.stream().noneMatch(r->phase(r)>=145&&phase(r)<160&&stage(r)==2&&Boolean.TRUE.equals(r.get("inside"))))failures.add("Stage2 did not really enter the thick wall");
        if(!brakeObserved||!brakeBeganInside||brakeStartingStage!=2)failures.add("Phase160 brake did not actually begin inside the thick wall in stage2");
        if(thick!=null&&server.stream().anyMatch(r->phase(r)>=145&&phase(r)<200&&((Number)r.get("health")).doubleValue()<thick.initialHealth-.01))
            failures.add("Ordinary-wall passage or safe exit reduced player health");
        if(server.stream().noneMatch(r->phase(r)>=160&&phase(r)<200&&exited(r)&&!Boolean.TRUE.equals(r.get("inside"))
                &&!Boolean.TRUE.equals(r.get("phasesSolidBlocks"))))failures.add("Braking inside the thick wall did not restore a safe non-phasing pose");
        if(server.stream().noneMatch(r->phase(r)>=220&&phase(r)<250&&stage(r)==2&&((Number)r.get("speed")).doubleValue()>=8))failures.add("Real forward/sprint did not recover stage2 after wall exit");
        long exitInside=frames.stream().filter(r->phase(r)>=160&&phase(r)<200&&exited(r)&&Boolean.TRUE.equals(r.get("inside"))).count();
        long bufferedExitInside=frames.stream().filter(r->phase(r)>=160&&phase(r)<200&&exited(r)&&Boolean.TRUE.equals(r.get("inside"))&&Boolean.TRUE.equals(r.get("bufferedPhasing"))).count();
        if(exitInside==0||bufferedExitInside==0)failures.add("No actual delayed client inside-wall pose after phase exit metadata");
        if(thinOverlayFrames.size()<4||thickOverlayFrames.size()<4)failures.add("Fewer than four actual canceled first-person BLOCK overlay frames for thin/thick cases");
        if(exitOverlayFrames.isEmpty())failures.add("No actual canceled inside-wall BLOCK overlay after phase exit metadata");
        long uncanceled=overlays.stream().filter(r->Boolean.TRUE.equals(r.get("inside"))&&!Boolean.TRUE.equals(r.get("canceled"))).count();
        if(uncanceled>0)failures.add("Production left "+uncanceled+" inside-wall BLOCK overlays uncanceled");
        double minCamera=Double.POSITIVE_INFINITY;
        for(int low:new int[]{100,145}){
            int high=low==100?130:200;var third=frames.stream().filter(r->phase(r)>=low&&phase(r)<high&&Boolean.TRUE.equals(r.get("thirdPersonSettled"))
                    &&(Boolean.TRUE.equals(r.get("bufferedPhasing"))||Boolean.TRUE.equals(r.get("inside")))).toList();
            if(third.isEmpty())failures.add("No actual settled third-person camera for wall "+low);
            for(var row:third){double distance=((Number)row.get("productionCameraDistance")).doubleValue();minCamera=Math.min(minCamera,distance);
                double floor=Math.min(1,Math.max(.25,Math.min(FlyingSwordConfig.CAMERA_NORMAL.get(),FlyingSwordConfig.CAMERA_SPRINT.get())*.25));
                if(!Double.isFinite(distance)||distance<floor)failures.add("Production third-person camera collapsed during phasing: "+distance);}
        }
        verification=new LinkedHashMap<>();verification.put("check","real-ordinary-wall-phase-presentation");verification.put("passed",failures.isEmpty());verification.put("failures",failures);
        verification.put("serverRows",server.size());verification.put("renderFrames",frames.size());verification.put("thinPassed",thinPassed);
        verification.put("brakeObserved",brakeObserved);verification.put("brakeBeganInside",brakeBeganInside);verification.put("brakeStartingStage",brakeStartingStage);
        verification.put("clientExitInsideFrames",exitInside);verification.put("bufferedExitInsideFrames",bufferedExitInside);
        verification.put("thinCanceledOverlayFrames",thinOverlayFrames.size());verification.put("thickCanceledOverlayFrames",thickOverlayFrames.size());
        verification.put("exitCanceledOverlayFrames",exitOverlayFrames.size());verification.put("uncanceledInsideOverlays",uncanceled);
        verification.put("settledThirdExitInsideFrames",frames.stream().filter(r->phase(r)>=160&&phase(r)<200&&Boolean.TRUE.equals(r.get("thirdPersonSettled"))&&Boolean.TRUE.equals(r.get("inside"))).count());
        verification.put("minimumSettledThirdPersonDistance",Double.isFinite(minCamera)?minCamera:0);
        verification.put("fixturePreparation","Synchronous FULL chunks allowed only for isolated wall fixture; not cold-streaming or MSPT acceptance");
        verification.put("frameLimit",160);verification.put("originalFrameLimit",originalFrameLimit==null?0:originalFrameLimit);
        if(!failures.isEmpty())throw new AssertionError("Wall QA: "+failures);return verification;
    }

    static void write(Path out)throws Exception{
        if(!ENABLED)return;finishing=true;
        try{
            // Also clean an early failed run, without changing its red verdict.
            Wall owner=thin!=null?thin:thick;
            if(owner!=null){
                Runnable restore=()->{if(thin!=null)thin.restore();if(thick!=null)thick.restore();};
                if(owner.level.getServer().isSameThread())restore.run();else owner.level.getServer().submitAsync(restore).join();
            }
            Files.createDirectories(out);CompletableFuture.allOf(IMAGE_WRITES.toArray(CompletableFuture[]::new)).join();
            var gson=new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(out.resolve("wall-server.json"),gson.toJson(snapshot(SERVER)));Files.writeString(out.resolve("wall-client.json"),gson.toJson(snapshot(CLIENT)));
            Files.writeString(out.resolve("wall-frames.json"),gson.toJson(snapshot(FRAMES)));Files.writeString(out.resolve("wall-overlays.json"),gson.toJson(snapshot(OVERLAYS)));
            Files.writeString(out.resolve("wall-photos.json"),gson.toJson(snapshot(PHOTOS)));Files.writeString(out.resolve("wall-summary.json"),gson.toJson(verification));
            Files.writeString(out.resolve("wall-fixtures.json"),gson.toJson(List.of(thin==null?Map.of("thin","not-built"):thin.info(),thick==null?Map.of("thick","not-built"):thick.info())));
        }finally{
            if(imageWriter!=null)imageWriter.shutdown();
            Minecraft.getInstance().execute(()->{
                if(originalCamera!=null)Minecraft.getInstance().options.setCameraType(originalCamera);
                if(originalFrameLimit!=null)Minecraft.getInstance().options.framerateLimit().set(originalFrameLimit);
            });
        }
    }
    private HuahongWallQa(){}
}
