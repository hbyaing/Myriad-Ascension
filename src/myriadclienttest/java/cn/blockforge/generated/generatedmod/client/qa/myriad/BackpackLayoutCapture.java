package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.*;
import com.google.gson.GsonBuilder;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

/** Real ScreenEvent lifecycle regression; disposable client only, never included in release jars. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class BackpackLayoutCapture {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.backpackLayoutCapture");
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.backpackCaptureOutput","../../reports/backpack-layout-20261030"));
    private static final int[][] CASES={{1920,1080,3},{2560,1440,3},{2560,1440,4},{1920,1080,4},{1280,900,4},{2560,1440,2}};
    private static final List<Object> RESULTS=new ArrayList<>();
    private static final List<String> FAILURES=new ArrayList<>();
    private static final long START=System.nanoTime();
    private static boolean opened,finished; private static int tick,stage;
    private static String captureName;private static Map<String,Object> captureRow;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!ENABLED||finished||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT); GLFW.glfwHideWindow(mc.getWindow().getWindow());
            mc.options.pauseOnLostFocus=false;mc.setWindowActive(true);
            if((System.nanoTime()-START)/1e9>420)throw new AssertionError("Backpack capture timeout: "+stage);
            if(!opened&&mc.player!=null)throw new AssertionError("Refuses an existing player world");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                opened=true;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(40);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("backpack-isolated-"+System.currentTimeMillis(),new LevelSettings("Backpack isolated QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(7215,false,false),net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);
            }
            if(!opened||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
            if(captureName!=null)return;
            if(Boolean.getBoolean("wuxingxiantu.backpackExpectShaders")){
                check(net.minecraftforge.fml.ModList.get().isLoaded("jei"),"JEI was not loaded");
                boolean shaders=false;
                for(String apiName:new String[]{"net.irisshaders.iris.api.v0.IrisApi","net.coderbot.iris.api.v0.IrisApi"})try{
                    var api=Class.forName(apiName);shaders=(boolean)api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null));break;
                }catch(ClassNotFoundException ignored){}
                check(shaders,"Expected shader pack was not active");
            }
            var entries=CultivationPageRegistry.visible();int count=entries.size();int[] c=CASES[stage/count];String id=entries.get(stage%count).id();
            tick++;
            if(tick==1){
                mc.setScreen(null);mc.getWindow().setWindowed(c[0],c[1]);
                // Framebuffer dimensions are explicit so hidden windows above desktop size are also tested.
                mc.getWindow().setWidth(c[0]);mc.getWindow().setHeight(c[1]);
                mc.options.guiScale().set(c[2]);mc.resizeDisplay();
                mc.player.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(cn.blockforge.generated.generatedmod.WuXingItems.DARK_IRON_SWORD.get()));
                mc.setScreen(new CultivationMainScreen(mc.player));
                double before=mc.getWindow().getGuiScale();
                ((CultivationMainScreen)mc.screen).selectPage(id);
                check(before==mc.getWindow().getGuiScale(),"Embedded page changed global scale: "+id);
                return;
            }
            if(tick<8)return;
            var host=(CultivationMainScreen)mc.screen;
            check(host.width==mc.getWindow().getGuiScaledWidth()&&host.height==mc.getWindow().getGuiScaledHeight(),"Stale host canvas: "+id);
            check(host.getGuiLeft()>=0&&host.getGuiTop()>=0&&host.getGuiLeft()+host.getXSize()<=host.width&&host.getGuiTop()+host.getYSize()<=host.height,"Frame clipped: "+id);
            var navField=CultivationMainScreen.class.getDeclaredField("navButtons");navField.setAccessible(true);
            var nav=(AbstractWidget)((List<?>)navField.get(host)).get(stage%count);
            double scaleBeforeClick=mc.getWindow().getGuiScale();
            check(host.mouseClicked(nav.getX()+nav.getWidth()/2.0,nav.getY()+nav.getHeight()/2.0,0),"Navigation hitbox missed: "+id);
            check(scaleBeforeClick==mc.getWindow().getGuiScale(),"Navigation changed host scale: "+id);
            var field=CultivationMainScreen.class.getDeclaredField("page");field.setAccessible(true);var page=(CultivationPage)field.get(host);
            var method=CultivationMainScreen.class.getDeclaredMethod("pageWidth");method.setAccessible(true);int expected=(int)method.invoke(host);
            if(page!=null){
                check(page.width==expected,"Embedded viewport overwritten: "+id+" expected "+expected+" got "+page.width);
                for(var child:page.children())if(child instanceof AbstractWidget w&&w.visible)check(w.getX()>=0&&w.getX()+w.getWidth()<=page.width,"Widget outside page: "+id+" / "+w.getMessage().getString());
            }
            mc.mouseHandler.grabMouse();PointerSafety.verify();
            String name=c[0]+"x"+c[1]+"-gui"+c[2]+"-"+id;
            captureRow=Map.of("case",name,"scale",mc.getWindow().getGuiScale(),"canvas",List.of(host.width,host.height),"pageWidth",page==null?0:page.width,"expectedPageWidth",expected);
            captureName=name;
        }catch(Throwable failure){FAILURES.add(failure.toString());finish(mc);}
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(!ENABLED||finished||captureName==null||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            boolean nonBlack=false;
            for(int y=0;y<image.getHeight()&&!nonBlack;y+=16)for(int x=0;x<image.getWidth();x+=16)if((image.getPixelRGBA(x,y)&0xffffff)!=0){nonBlack=true;break;}
            check(nonBlack,"Black framebuffer capture: "+captureName);
            image.writeToFile(OUT.resolve(captureName+".png"));RESULTS.add(captureRow);captureName=null;captureRow=null;tick=0;
            if(++stage==CASES.length*CultivationPageRegistry.visible().size())finish(mc);
        }catch(Throwable failure){FAILURES.add(failure.toString());finish(mc);}
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void finish(Minecraft mc){
        finished=true;
        try{Files.writeString(OUT.resolve("verification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("cases",RESULTS,"failures",FAILURES,"jeiLoaded",net.minecraftforge.fml.ModList.get().isLoaded("jei"),"shaderPackRequiredAndChecked",Boolean.getBoolean("wuxingxiantu.backpackExpectShaders"),"pointerSafety",PointerSafety.snapshot())));}catch(Exception ex){throw new RuntimeException(ex);}
        mc.stop();
    }
}
