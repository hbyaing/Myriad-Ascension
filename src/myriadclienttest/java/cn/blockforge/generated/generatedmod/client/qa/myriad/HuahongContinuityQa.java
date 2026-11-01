package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.phys.Vec3;

/** High-render-rate, actual sampler/attachment evidence. Only in the isolated QA source set. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=cn.blockforge.generated.generatedmod.GeneratedMod.MOD_ID,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class HuahongContinuityQa {
    static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.huahongContinuity");
    private static final List<Map<String,Object>> ROWS=new ArrayList<>();
    private static final Map<String,Integer> IMAGES=new HashMap<>();
    private static Map<String,Object> canonicalCheck;
    private static Map<String,Object> atlasFilter;
    private static final ExecutorService WRITER=Executors.newFixedThreadPool(2,r->{var t=new Thread(r,"continuity-png");t.setDaemon(true);return t;});
    private static final AtomicInteger BACKLOG=new AtomicInteger();
    private static final List<String> IMAGE_ERRORS=Collections.synchronizedList(new ArrayList<>());
    private static int renderedRiders;
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void begin(net.minecraftforge.event.TickEvent.RenderTickEvent event){
        if(ENABLED&&event.phase==net.minecraftforge.event.TickEvent.Phase.START)renderedRiders=0;
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void rider(net.minecraftforge.client.event.RenderPlayerEvent.Post event){
        if(ENABLED&&event.getEntity()==Minecraft.getInstance().player&&!TrailShaderBridge.shadowPass())renderedRiders++;
    }
    private static Object field(Class<?> type,Object instance,String name)throws Exception{
        var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(instance);
    }
    private static List<Double> p(Vec3 p){return List.of(p.x,p.y,p.z);}
    private static Vec3 v(SwordFlightTrailHistory.Point p){return new Vec3(p.x(),p.y(),p.z());}
    static void frame(Minecraft mc,float partial,Path out)throws Exception{
        if(!ENABLED||!HuahongDynamicsQa.started()||mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return;
        var c=HuahongDynamicsQa.control();var f=InterpolatedFlyingSwordTransform.get(sword,partial);
        double now=FlyingSwordEffects.samplerTick()-1+partial;
        var row=new LinkedHashMap<String,Object>();ROWS.add(row);
        row.put("canonicalStrength",f.huahong());row.put("roll",f.roll());
        row.put("riderDraws",renderedRiders);
        if(atlasFilter==null&&c.phase()>=60){
            int active=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
            org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
            int bound=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
            try{
                int texture=mc.getTextureManager().getTexture(new net.minecraft.resources.ResourceLocation("wuxingxiantu","textures/effect/huahong/smoke-atlas.png")).getId();
                org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,texture);
                int min=org.lwjgl.opengl.GL11.glGetTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER);
                int mag=org.lwjgl.opengl.GL11.glGetTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER);
                atlasFilter=Map.of("min",min,"mag",mag,"linear",min==org.lwjgl.opengl.GL11.GL_LINEAR&&mag==org.lwjgl.opengl.GL11.GL_LINEAR);
            }finally{org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,bound);org.lwjgl.opengl.GL13.glActiveTexture(active);}
        }
        row.put("frame",ROWS.size());row.put("scenario",HuahongDynamicsQa.tick());row.put("phase",c.phase());
        row.put("partial",partial);row.put("now",now);row.put("entityTick",sword.tickCount);
        row.put("position",p(sword.position()));row.put("old",List.of(sword.xOld,sword.yOld,sword.zOld));row.put("renderPosition",p(f.position()));
        row.put("stage",sword.huahongStage());row.put("rawStrength",sword.huahongStrength());row.put("strength",sword.renderHuahong(partial));
        row.put("metadataSpeed",sword.flightSpeed());row.put("presentedSpeed",sword.renderSpeed(1));
        row.put("skin",sword.huahongSkin().name());row.put("normalVertices",FlyingSwordEffects.normalTrailVertexCount);row.put("rainbowVertices",HuahongVisuals.vertices);
        row.put("post",FlyingSwordEffects.activeTrailRenderPath);row.put("submitted",FlyingSwordEffects.normalTrailSubmitted);
        row.put("renderEyeDistance",f.position().distanceTo(mc.gameRenderer.getMainCamera().getPosition()));
        row.put("tickEyeDistance",sword.position().distanceTo(mc.gameRenderer.getMainCamera().getPosition()));
        if(sword.huahongSkin().name().equals("DENSE")&&sword.huahongStage()==2)row.put("volume",HuahongVolumeRenderer.diagnostics());
        if(canonicalCheck==null&&c.phase()>=40&&f.position().distanceTo(sword.position())>.1)canonicalCheck=canonical(sword,partial);
        @SuppressWarnings("unchecked") var normal=(Map<ControlledSwordEntity,SwordFlightTrailHistory>)field(FlyingSwordEffects.class,null,"TRAILS");
        Vec3 anchor=SwordFlightAttachment.tail(sword,f.position(),f.yaw(),f.pitch(),f.roll());
        if(normal.containsKey(sword))row.put("normalHistory",history(normal.get(sword),now,anchor));
        @SuppressWarnings("unchecked") var rainbow=(Map<ControlledSwordEntity,?>)field(HuahongVisuals.class,null,"TRAILS");
        Object trail=rainbow.get(sword);
        if(trail!=null)row.put("rainbowHistory",history((SwordFlightTrailHistory)field(trail.getClass(),trail,"history"),now,f.position().add(0,.85,0)));
        String key=sword.huahongSkin().name()+"-"+(f.huahong()<.01?"ordinary":c.phase()<110?"straight":c.phase()<230?"turn":"exit");
        boolean moving=sword.position().distanceTo(new Vec3(sword.xOld,sword.yOld,sword.zOld))>.3;
        row.put("moving",moving);
        boolean capture=moving&&(c.phase()>=42&&c.phase()<75&&f.huahong()<.01||c.phase()>=82&&c.phase()<110&&f.huahong()>=.995||c.phase()>=183&&c.phase()<210&&f.huahong()>=.995||c.phase()>=251&&c.phase()<266);
        if(capture&&IMAGES.getOrDefault(key,0)<24&&BACKLOG.get()<8){
            IMAGES.merge(key,1,Integer::sum);String name=String.format(Locale.ROOT,"continuity/%s-%06d.png",key,ROWS.size());
            Files.createDirectories(out.resolve("continuity"));
            var image=Screenshot.takeScreenshot(mc.getMainRenderTarget());BACKLOG.incrementAndGet();
            WRITER.execute(()->{try(image){image.writeToFile(out.resolve(name));}catch(Throwable error){IMAGE_ERRORS.add(error.toString());}finally{BACKLOG.decrementAndGet();}});
            row.put("image",name);
        }
    }
    private static Map<String,Object> history(SwordFlightTrailHistory history,double now,Vec3 anchor){
        var raw=history.nodes();var drawn=history.renderNodes(now,new SwordFlightTrailHistory.Point(anchor.x,anchor.y,anchor.z));
        var row=new LinkedHashMap<String,Object>();row.put("samples",raw.size());row.put("rendered",drawn.size());
        if(!raw.isEmpty()){
            var last=raw.get(raw.size()-1);row.put("sampleTick",last.tick());row.put("sample",p(v(last.point())));row.put("sampleArc",last.arc());
            SwordFlightTrailHistory.Node a=null,b=null;
            for(var n:raw){if(n.tick()<=now)a=n;else{b=n;break;}}
            if(a!=null&&b!=null){double t=(now-a.tick())/(b.tick()-a.tick());Vec3 expected=v(a.point()).lerp(v(b.point()),t);row.put("clockAttachmentError",expected.distanceTo(anchor));}
        }
        if(!drawn.isEmpty()){
            var head=drawn.get(drawn.size()-1);row.put("headArc",head.arc());row.put("arcLength",head.arc()-drawn.get(0).arc());row.put("headError",v(head.point()).distanceTo(anchor));
            if(drawn.size()>1)row.put("lastSegment",v(drawn.get(drawn.size()-2).point()).distanceTo(anchor));
        }
        return row;
    }
    static void write(Path out)throws Exception{
        if(ENABLED){WRITER.shutdown();if(!WRITER.awaitTermination(55,TimeUnit.SECONDS))throw new AssertionError("PNG writer did not finish");Files.writeString(out.resolve("continuity-frames.json"),new GsonBuilder().setPrettyPrinting().create().toJson(ROWS));}
    }
    private static Map<String,Object> canonical(ControlledSwordEntity sword,float actual)throws Exception{
        @SuppressWarnings("unchecked") var frames=(Map<ControlledSwordEntity,InterpolatedFlyingSwordTransform>)field(InterpolatedFlyingSwordTransform.class,null,"FRAMES");
        var saved=new IdentityHashMap<>(frames);
        boolean active=(Boolean)field(InterpolatedFlyingSwordTransform.class,null,"rendering");
        float oldPartial=(Float)field(InterpolatedFlyingSwordTransform.class,null,"framePartial");
        try{
            InterpolatedFlyingSwordTransform.frame(new net.minecraftforge.event.TickEvent.RenderTickEvent(net.minecraftforge.event.TickEvent.Phase.START,.375F));
            var wrongCaller=InterpolatedFlyingSwordTransform.get(sword,.91F);
            Vec3 expected=new Vec3(sword.xOld,sword.yOld,sword.zOld).lerp(sword.position(),.375);
            double error=wrongCaller.position().distanceTo(expected);
            if(error>1e-8)throw new AssertionError("First caller poisoned shared render partial: "+error);
            if(wrongCaller!=InterpolatedFlyingSwordTransform.get(sword,.01F))throw new AssertionError("Frame consumers did not share pose");
            return Map.of("framePartial",.375,"firstCallerPartial",.91,"secondCallerPartial",.01,"positionError",error,"passed",true);
        }finally{
            InterpolatedFlyingSwordTransform.frame(new net.minecraftforge.event.TickEvent.RenderTickEvent(net.minecraftforge.event.TickEvent.Phase.START,actual));
            frames.clear();frames.putAll(saved);
            var activeField=InterpolatedFlyingSwordTransform.class.getDeclaredField("rendering");activeField.setAccessible(true);activeField.setBoolean(null,active);
            var partialField=InterpolatedFlyingSwordTransform.class.getDeclaredField("framePartial");partialField.setAccessible(true);partialField.setFloat(null,oldPartial);
        }
    }
    static void verify(Path out)throws Exception{
        if(!ENABLED)return;
        var failures=new ArrayList<String>(IMAGE_ERRORS);var skins=new TreeSet<String>();
        int normalPhotos=0,fullPhotos=0,movingIntervals=0,withinTick=0,ghost=0,missingRider=0,historyResets=0;
        Map<String,Object> previousRainbow=null;
        double maxClockError=0;Map<String,Object> previous=null;
        for(var row:ROWS){
            String skin=(String)row.get("skin");skins.add(skin);
            if(row.containsKey("image")){if(((Number)row.get("canonicalStrength")).doubleValue()<.01)normalPhotos++;else if(((Number)row.get("canonicalStrength")).doubleValue()>=.995)fullPhotos++;}
            if(((Number)row.get("canonicalStrength")).doubleValue()>=.9999&&((Number)row.get("normalVertices")).intValue()>((Number)row.get("rainbowVertices")).intValue())ghost++;
            if(Boolean.TRUE.equals(row.get("moving"))&&((Number)row.get("canonicalStrength")).doubleValue()<.01&&((Number)row.get("riderDraws")).intValue()==0)missingRider++;
            if(row.get("rainbowHistory") instanceof Map<?,?> h&&h.get("sampleArc") instanceof Number arc){
                if(previousRainbow!=null&&skin.equals(previousRainbow.get("skin"))&&((Number)row.get("canonicalStrength")).doubleValue()>.01&&((Number)previousRainbow.get("canonicalStrength")).doubleValue()>.01){
                    var old=(Map<?,?>)previousRainbow.get("rainbowHistory");
                    if(arc.doubleValue()<((Number)old.get("sampleArc")).doubleValue()-1e-5)historyResets++;
                }
                previousRainbow=row;
            }
            for(String key:List.of("normalHistory","rainbowHistory"))if(row.get(key) instanceof Map<?,?> h&&h.get("clockAttachmentError") instanceof Number n)maxClockError=Math.max(maxClockError,n.doubleValue());
            if(previous!=null&&!row.get("renderPosition").equals(previous.get("renderPosition"))){movingIntervals++;if(row.get("entityTick").equals(previous.get("entityTick")))withinTick++;}
            previous=row;
        }
        if(canonicalCheck==null)failures.add("Missing canonical-partial check");
        if(atlasFilter==null||!Boolean.TRUE.equals(atlasFilter.get("linear")))failures.add("Actual smoke atlas did not use linear filtering: "+atlasFilter);
        if(normalPhotos<1||fullPhotos<1)failures.add("Missing ordinary/full-strength actual PNGs");
        if(withinTick<30)failures.add("Insufficient moving subframes: "+withinTick);
        if(maxClockError>.35)failures.add("Sampler/attachment mismatch "+maxClockError);
        if(ghost>0)failures.add("Ordinary trail leaked into full rainbow: "+ghost);
        if(missingRider>0)failures.add("Moving ordinary-flight rider culled in "+missingRider+" frames");
        if(historyResets>0)failures.add("Non-teleport rainbow history resets: "+historyResets);
        for(String skin:skins){
            if(IMAGES.getOrDefault(skin+"-straight",0)==0||IMAGES.getOrDefault(skin+"-turn",0)==0)failures.add("Missing straight/turn PNGs: "+skin);
            if(skin.equals("DENSE")&&ROWS.stream().filter(r->skin.equals(r.get("skin"))).noneMatch(r->r.get("volume") instanceof Map<?,?> volume&&((Number)volume.get("totalDraws")).intValue()>0))failures.add("Missing DENSE submitted volume");
        }
        var summary=new LinkedHashMap<String,Object>();summary.put("frames",ROWS.size());summary.put("skins",skins);summary.put("canonicalPartial",canonicalCheck);summary.put("normalPhotos",normalPhotos);summary.put("fullPhotos",fullPhotos);summary.put("movingIntervals",movingIntervals);summary.put("movingSubframesWithinOneTick",withinTick);summary.put("maxClockAttachmentError",maxClockError);summary.put("fullRainbowOrdinaryLeaks",ghost);summary.put("images",IMAGES);summary.put("failures",failures);
        summary.put("missingMovingOrdinaryRiderFrames",missingRider);
        summary.put("actualSmokeAtlasFilter",atlasFilter);
        summary.put("nonTeleportHistoryResets",historyResets);
        Files.writeString(out.resolve("continuity-summary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(summary));
        if(!failures.isEmpty())throw new AssertionError("Continuity acceptance: "+failures);
    }
}
