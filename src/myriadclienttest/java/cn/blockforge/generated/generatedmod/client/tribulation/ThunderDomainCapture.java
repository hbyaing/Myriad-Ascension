package cn.blockforge.generated.generatedmod.client.tribulation;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.TrailShaderBridge;
import cn.blockforge.generated.generatedmod.client.qa.myriad.PointerSafety;
import cn.blockforge.generated.generatedmod.tribulation.ThunderField;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Isolated production network/render capture, with no access to an existing player world. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class ThunderDomainCapture {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.thunderDomainCapture");
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.myriadCaptureOutput","captures"));
    private static final long START=System.nanoTime();
    private static boolean opened,ready,finished,capture,baseline;
    private static long drawsBeforeEmission,firstDrawTick;
    private static int stage,ticks,shadowPasses,worldPasses;
    private static CompletableFuture<?> work;
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    private static Map<String,Object> projection=Map.of();
    private static Map<String,Object> screenWidths=Map.of();
    private static Vec3 strikeAnchor=new Vec3(0,160,0);

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!ENABLED||finished||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT);mc.setWindowActive(true);mc.options.pauseOnLostFocus=false;
            GLFW.glfwHideWindow(mc.getWindow().getWindow());
            if((System.nanoTime()-START)/1e9>360)throw new AssertionError("Thunder domain QA timeout");
            if(!opened&&mc.player!=null)throw new AssertionError("Refuses existing world");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                screenWidths=checkScreenWidths();
                opened=true;mc.getWindow().setWindowed(1280,900);mc.options.renderDistance().set(5);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(40);mc.options.hideGui=true;
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("thunder-domain-qa-"+System.currentTimeMillis(),new LevelSettings("Thunder domain projection QA",GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(7217,false,false),net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);
            }
            if(mc.player==null||mc.getOverlay()!=null)return;
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!ready){
                ready=true;ticks=0;mc.setScreen(null);mc.options.hideGui=stage<6;firstDrawTick=-1;mc.mouseHandler.grabMouse();PointerSafety.verify();
                int scene=stage;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
                work=server.submit(()->{
                    var p=server.getPlayerList().getPlayer(id);var level=p.serverLevel();
                    if(scene==0)for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)level.setBlock(new BlockPos(x,159,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
                    boolean rain=scene==4||scene==5;
                    level.setDayTime(scene==2||scene==3?18000:6000);
                    level.setWeatherParameters(rain?0:12000,rain?12000:0,rain,false);
                    if(rain){
                        // A real landscape and a steep first-person camera close to the upper channels.
                        int x=-122,z=543;
                        int ground=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                        strikeAnchor=new Vec3(x+.5,ground,z+.5);
                        p.teleportTo(level,x+.5,ground+27,z+5.5,180,67);
                    }else{
                        strikeAnchor=new Vec3(0,160,0);
                        p.teleportTo(level,scene==1?33:scene==2?-25:0,178,scene==0?40:scene==1?33:35,scene==1?135:scene==2?-144:180,scene==2?8:0);
                    }
                    p.setInvulnerable(true);p.getAbilities().flying=true;p.getAbilities().mayfly=true;p.onUpdateAbilities();
                });return;
            }
            ticks++;
            if(stage<6){
                if(stage>=4)mc.level.setRainLevel(1f);
                if(ticks==36)baseline=true;
                if(ticks==38){
                    drawsBeforeEmission=LightningShaderBridge.draws;
                    var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
                    boolean strong=(stage&1)!=0;
                    server.execute(()->{
                        var p=server.getPlayerList().getPlayer(id);
                        ThunderField.emit(p.serverLevel(),p,strikeAnchor,7,strong,true);
                    });
                }
                if(ticks>38&&LightningShaderBridge.draws>drawsBeforeEmission){
                    if(firstDrawTick<0)firstDrawTick=mc.level.getGameTime();
                    capture=mc.level.getGameTime()-firstDrawTick>=3;
                }
                if(ticks>90)throw new AssertionError("Thunder packet did not reach the render stage");
            }else if(ticks==46)capture=true;
        }catch(Throwable error){fail(error);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hudBackground(RenderGuiEvent.Pre e){
        if(ENABLED&&!finished&&ready&&stage>=6)e.getGuiGraphics().fill(0,0,520,160,stage==6?0xFFF1E9CB:0xFF17232D);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void world(RenderLevelStageEvent e){
        if(!ENABLED||finished||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        if(TrailShaderBridge.shadowPass()){shadowPasses++;return;}worldPasses++;
        var camera=e.getCamera().getPosition();
        Matrix4f actual=new Matrix4f(LightningShaderBridge.projection).mul(LightningShaderBridge.modelView).mul(e.getPoseStack().last().pose());
        Matrix4f expected=new Matrix4f(e.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(e.getPoseStack().last().pose());
        var bottom=new Vector4f((float)(strikeAnchor.x-camera.x),(float)(strikeAnchor.y-camera.y),(float)(strikeAnchor.z-camera.z),1);
        var top=new Vector4f(bottom).add(0,32,0,0);
        Vector4f at=actual.transform(new Vector4f(top)),ab=actual.transform(new Vector4f(bottom));
        Vector4f et=expected.transform(new Vector4f(top)),eb=expected.transform(new Vector4f(bottom));
        projection=Map.of("actualAxisDx",at.x/at.w-ab.x/ab.w,"actualAxisDy",at.y/at.w-ab.y/ab.w,
            "expectedAxisDx",et.x/et.w-eb.x/eb.w,"expectedAxisDy",et.y/et.w-eb.y/eb.w,
            "shadowPasses",shadowPasses,"worldPasses",worldPasses);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void frame(TickEvent.RenderTickEvent e){
        if(!ENABLED||finished||(!capture&&!baseline)||e.phase!=TickEvent.Phase.END)return;
        try{
            var mc=Minecraft.getInstance();int error=GL11.glGetError();if(error!=0)throw new AssertionError("GL error "+error);
            String name=stage<6?((stage&1)==0?"a1-":"b2-")+(stage<2?"day":stage<4?"night":"rain-steep"):stage==6?"hud-light":"hud-dark";
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
                image.writeToFile(OUT.resolve(name+(baseline?"-baseline":"")+".png"));
            }
            if(baseline){baseline=false;return;}
            boolean shaderPack=TrailShaderBridge.shaderPackDetected();
            if(shaderPack!=Boolean.getBoolean("wuxingxiantu.myriadExternalShaders"))
                throw new AssertionError("Unexpected shader pack state: "+shaderPack);
            RESULTS.add(Map.of("scene",name,"projection",projection,"glError",error,"shaderPack",shaderPack,
                "lateDraws",LightningShaderBridge.draws,"sceneDraws",stage<6?LightningShaderBridge.draws-drawsBeforeEmission:0,"rainLevel",mc.level.getRainLevel(1)));
            double dx=((Number)projection.get("actualAxisDx")).doubleValue()-((Number)projection.get("expectedAxisDx")).doubleValue();
            double dy=((Number)projection.get("actualAxisDy")).doubleValue()-((Number)projection.get("expectedAxisDy")).doubleValue();
            if(stage<6&&(Math.abs(dx)>.0002||Math.abs(dy)>.0002))throw new AssertionError("Lightning projection differs from world camera: "+projection);
            capture=false;if(++stage<8){ready=false;return;}
            PointerSafety.verify();Files.writeString(OUT.resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",true,"scenes",RESULTS,"pointer",PointerSafety.snapshot(),"screenWidths",screenWidths)));
            finished=true;mc.stop();
        }catch(Throwable error){fail(error);}
    }
    private static Map<String,Object> checkScreenWidths(){
        Map<String,Object> samples=new LinkedHashMap<>();
        for(int height:new int[]{900,1440}){
            int width=height*16/9;
            var matrix=new Matrix4f().perspective((float)Math.toRadians(70),(float)width/height,.05f,256f);
            var projection=HaloGeometry.prepare(matrix,width,height);
            for(boolean halo:new boolean[]{false,true}){
                double radius=halo?.37:.042,limit=(halo?12:2.4)*height/900.0;
                double last=0;
                for(double depth:new double[]{40,25,18,14,10,8,5,3,1,.1}){
                    // A bent path exercises both the convex strips and the round joint.
                    var points=List.of(new HaloGeometry.Point(new Vec3(-.6,0,-depth),radius,0,1),
                        new HaloGeometry.Point(new Vec3(0,0,-depth),radius,.5,1),
                        new HaloGeometry.Point(new Vec3(.6,.2,-depth),radius,1,1));
                    var recipe=new LightningRecipe();
                    var original=HaloGeometry.build(points,projection,recipe,halo);
                    var bounded=HaloGeometry.build(points,projection,recipe,halo,true);
                    if(depth>=25&&(!positions(original).equals(positions(bounded))||!original.bounds().equals(bounded.bounds())))
                        throw new AssertionError("Mid/far geometry changed at "+depth);
                    double half=screenHalfWidth(bounded.body(),matrix,width,height);
                    if(half<last-.001||half>limit+.001)
                        throw new AssertionError("Stroke width "+half+" outside monotonic cap "+limit);
                    last=half;
                    if(depth==.1){
                        double joint=screenHalfWidth(bounded.round(),matrix,width,height);
                        double uncapped=screenHalfWidth(original.body(),matrix,width,height);
                        if(Math.abs(half-limit)>.001||Math.abs(joint-limit)>.001||uncapped<=limit)
                            throw new AssertionError("Near cap did not constrain the strip and joint");
                        samples.put((halo?"halo":"core")+"-"+height,Map.of("uncappedHalfPixels",uncapped,"cappedHalfPixels",half,"jointHalfPixels",joint));
                    }
                }
            }
        }
        return samples;
    }
    private static List<Vec3> positions(HaloGeometry.Mesh mesh){
        return java.util.stream.Stream.concat(mesh.body().stream(),mesh.round().stream()).map(HaloGeometry.Vertex::p).toList();
    }
    private static double screenHalfWidth(List<HaloGeometry.Vertex> vertices,Matrix4f matrix,int width,int height){
        if(vertices.size()<2)throw new AssertionError("Missing projected quad");
        var a=vertices.get(0).p();var b=vertices.get(1).p();
        var pa=matrix.transform(new Vector4f((float)a.x,(float)a.y,(float)a.z,1));
        var pb=matrix.transform(new Vector4f((float)b.x,(float)b.y,(float)b.z,1));
        return .25*Math.hypot((pa.x/pa.w-pb.x/pb.w)*width,(pa.y/pa.w-pb.y/pb.w)*height);
    }
    private static void fail(Throwable error){
        error.printStackTrace();try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("failure.txt"),error.toString());}catch(Exception ignored){}
        finished=true;Minecraft.getInstance().stop();
    }
}
