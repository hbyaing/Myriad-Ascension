package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.AbstractWidget;
import java.nio.file.*;
import java.util.*;

/** Exercises real client settings, C2S preference and synced sword metadata. Test-only. */
final class HuahongSkinQa {
    private static final int[][] CASES={{1920,1080,3},{2560,1440,3},{1920,1080,4},{2560,1440,4}};
    static String pending;
    private static Map<String,Object> uiRow;
    private static boolean lastClick;
    static boolean tick(Minecraft mc,int tick,Path out,List<Object> rows)throws Exception{
        if(tick<320)return false;
        if(tick<880){
            int k=(tick-320)/70,phase=(tick-320)%70;
            if(phase==0){FlyingSwordConfig.TRAILS.set(true);HuahongSkinPreferences.select(HuahongSkin.values()[k]);
                var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    cn.blockforge.generated.generatedmod.GeneratedMod.data(p).wuxingxiantu$setAura(1000000);
                    if(p.getVehicle() instanceof ControlledSwordEntity s){s.setPos(8,180,-24);s.positionRider(p);}});
            }
            if(phase==44||phase==60){
                if(!(mc.player.getVehicle() instanceof ControlledSwordEntity sword)||sword.huahongSkin()!=HuahongSkin.values()[k])throw new AssertionError("Synced skin missing: "+k);
                check(sword.huahongStage()==2&&sword.huahongStrength()>=.995F,"Skin capture is not stable cruise: "+k);
                pending="skin-"+k+"-"+HuahongSkin.values()[k].name().toLowerCase(Locale.ROOT)+(phase==44?"-side.png":"-turn.png");
            }
            return false;
        }
        if(tick<900)return false;
        int index=(tick-900)/30,phase=(tick-900)%30;
        if(index>=CASES.length){HuahongSkinPreferences.select(HuahongSkin.METEOR_PRISM);return true;}
        int[] c=CASES[index];
        if(phase==0){
            mc.setScreen(null);mc.getWindow().setWindowed(c[0],c[1]);mc.getWindow().setWidth(c[0]);mc.getWindow().setHeight(c[1]);mc.options.guiScale().set(c[2]);mc.resizeDisplay();mc.options.hideGui=false;
            var host=new CultivationMainScreen(mc.player);mc.setScreen(host);double before=mc.getWindow().getGuiScale();host.selectPage("settings");
            var page=(CultivationPage)get(host,"page");
            var tab=page.children().stream().filter(w->w instanceof AbstractWidget a&&a.getMessage().getString().equals("御剑飞行")).map(w->(AbstractWidget)w).findFirst().orElseThrow();
            click(host,tab);check(before==mc.getWindow().getGuiScale(),"Category changed global scale");
        }
        if(phase==5){
            var host=(CultivationMainScreen)mc.screen;var page=(CultivationPage)get(host,"page");
            double scale=mc.getWindow().getGuiScale();
            check(host.getGuiLeft()>=0&&host.getGuiTop()>=0&&host.getGuiLeft()+host.getXSize()<=host.width&&host.getGuiTop()+host.getYSize()<=host.height,"Outer frame clipped");
            for(var skin:HuahongSkin.values()){
                var button=page.children().stream().filter(w->w instanceof AbstractWidget a&&a.getMessage().getString().equals(skin.label)).map(w->(AbstractWidget)w).findFirst().orElseThrow();
                check(button instanceof CultivationUi.Action,"Vanilla skin button");
                set(host,"scroll",Math.max(0,button.getY()-80));click(host,button);
                check(FlyingSwordConfig.HUAHONG_SKIN.get()==skin,"Skin hitbox/config mismatch "+skin);
            }
            check(scale==mc.getWindow().getGuiScale(),"Setting changed host scale");
            set(host,"scroll",630);lastClick=true;
            uiRow=new LinkedHashMap<>();uiRow.put("size",List.of(c[0],c[1]));uiRow.put("guiOption",c[2]);uiRow.put("scale",scale);uiRow.put("skinButtons",8);uiRow.put("clicks",lastClick);uiRow.put("pageWidth",page.width);
        }
        if(phase>=10){
            var host=(CultivationMainScreen)mc.screen;var page=(CultivationPage)get(host,"page");
            var buttons=page.children().stream().filter(w->w instanceof CultivationUi.Action a&&Arrays.stream(HuahongSkin.values()).anyMatch(s->s.label.equals(a.getMessage().getString()))).map(w->(AbstractWidget)w).toList();
            double x=-50,y=-50;
            if(phase>=11&&phase<=19){
                var b=buttons.get(2);double s=((Number)get(host,"pageScale")).doubleValue();
                x=(int)get(host,"contentX")+(b.getX()+b.getWidth()*.5)*s;
                y=(int)get(host,"contentY")+(b.getY()+b.getHeight()*.5-(int)get(host,"scroll"))*s;
            }
            // Synthetic client coordinates only; no GLFW/OS pointer movement.
            for(String axis:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(axis);f.setAccessible(true);f.setDouble(mc.mouseHandler,axis.equals("xpos")?x*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth():y*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight());}
            if(phase==20){buttons.get(0).setFocused(true);buttons.get(1).active=false;}
            if(phase==10||phase==15||phase==20)pending="settings-"+c[0]+"x"+c[1]+"-gui"+c[2]+(phase==10?"-normal.png":phase==15?"-hover.png":"-focus-disabled.png");
        }
        return false;
    }
    static void frame(Minecraft mc,Path out,List<Object> rows)throws Exception{
        if(pending==null)return;
        PointerSafety.verify();int gl=org.lwjgl.opengl.GL11.glGetError();check(gl==0,"Skin GL error "+gl);
        if(pending.startsWith("skin-")){
            check(HuahongVisuals.vertices>0,"Skin has no actual mesh");
            check(mc.player.getVehicle() instanceof ControlledSwordEntity s&&s.huahongStage()==2&&s.huahongStrength()>=.995F,"Skin left stable cruise before frame");
        }
        if(pending.endsWith("-hover.png")){
            var page=(CultivationPage)get(mc.screen,"page");
            check(page.children().stream().anyMatch(w->w instanceof AbstractWidget a&&a.getMessage().getString().equals(HuahongSkin.NEEDLE.label)&&a.isHovered()),"Skin hover missed its rendered hitbox");
        }
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(pending));}
        var row=new LinkedHashMap<String,Object>();row.put("image",pending);row.put("vertices",HuahongVisuals.vertices);row.put("shaderPack",TrailShaderBridge.shaderPackDetected());row.put("glError",gl);
        if(pending.startsWith("skin-")&&mc.player.getVehicle() instanceof ControlledSwordEntity s){row.put("stage",s.huahongStage());row.put("strength",s.huahongStrength());row.put("yaw",s.getYRot());}
        if(pending.startsWith("settings"))row.putAll(uiRow);rows.add(row);pending=null;
    }
    private static Object get(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static void set(Object object,String name,Object value)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    private static void click(CultivationMainScreen host,AbstractWidget widget)throws Exception{
        double scale=((Number)get(host,"pageScale")).doubleValue(),x=(int)get(host,"contentX")+(widget.getX()+widget.getWidth()*.5)*scale;
        double y=(int)get(host,"contentY")+(widget.getY()+widget.getHeight()*.5-(int)get(host,"scroll"))*scale;
        check(host.mouseClicked(x,y,0),"Host click missed "+widget.getMessage().getString());host.mouseReleased(x,y,0);
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
