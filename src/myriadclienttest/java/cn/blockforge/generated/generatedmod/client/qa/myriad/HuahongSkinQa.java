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
                    if(!HuahongCapture.FULL_SPEED&&p.getVehicle() instanceof ControlledSwordEntity s){s.setPos(8,180,-24);s.positionRider(p);}});
            }
            if(phase==44||phase==60){
                if(!(mc.player.getVehicle() instanceof ControlledSwordEntity sword)||sword.huahongSkin()!=HuahongSkin.values()[k])throw new AssertionError("Synced skin missing: "+k);
                check(sword.huahongStage()==2&&sword.huahongStrength()>=.995F,"Skin capture is not stable cruise: "+k+" / "+HuahongCapture.flightDiagnostics(sword));
                pending="skin-"+k+"-"+HuahongSkin.values()[k].name().toLowerCase(Locale.ROOT)+(phase==44?"-side.png":"-turn.png");
            }
            return false;
        }
        if(tick<900)return false;
        int index=(tick-900)/30,phase=(tick-900)%30;
        if(index>=CASES.length){HuahongSkinPreferences.select(HuahongSkin.METEOR_PRISM);return footerTick(mc,tick-1020,rows);}
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
                var toggle=skinToggle(host);check(toggle instanceof CultivationUi.Action,"Vanilla skin dropdown");
                set(host,"scroll",Math.max(0,toggle.getY()-80));click(host,toggle);
                page=(CultivationPage)get(host,"page");
                check(Boolean.TRUE.equals(get(page,"huahongSkinsExpanded")),"Skin dropdown did not open");
                verifyDropdownVisible(host);
                var button=page.children().stream().filter(w->w instanceof AbstractWidget a&&a.getMessage().getString().equals(skin.label)).map(w->(AbstractWidget)w).findFirst().orElseThrow();
                check(button instanceof CultivationUi.Action,"Vanilla skin option");
                click(host,button);
                check(FlyingSwordConfig.HUAHONG_SKIN.get()==skin,"Skin hitbox/config mismatch "+skin);
                page=(CultivationPage)get(host,"page");
                check(!Boolean.TRUE.equals(get(page,"huahongSkinsExpanded")),"Selecting a skin did not collapse dropdown");
                check(skinToggle(host).getMessage().getString().equals(skin.label+"  ▾"),"Dropdown label omitted selected skin or domain disclosure");
                check(page.children().stream().noneMatch(w->w instanceof AbstractWidget a&&Arrays.stream(HuahongSkin.values()).anyMatch(s->s.label.equals(a.getMessage().getString()))),"Collapsed dropdown retained option buttons");
                check(page.getFocused()==skinToggle(host)&&page.children().contains(page.getFocused()),"Mouse selection left focus on a retired widget");
                check(host.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0),"Focused skin selector ignored Enter");host.keyReleased(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);
                page=(CultivationPage)get(host,"page");
                check(Boolean.TRUE.equals(get(page,"huahongSkinsExpanded"))&&page.getFocused()==skinToggle(host)&&page.children().contains(page.getFocused()),"Keyboard expansion lost the real selector focus");
                verifyDropdownVisible(host);
                check(host.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0),"Expanded skin dropdown ignored Escape");host.keyReleased(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0);
                page=(CultivationPage)get(host,"page");
                check(mc.screen==host&&!Boolean.TRUE.equals(get(page,"huahongSkinsExpanded"))&&page.getFocused()==skinToggle(host)&&page.children().contains(page.getFocused())&&FlyingSwordConfig.HUAHONG_SKIN.get()==skin,"Escape did not preserve the selected skin and real selector focus");
            }
            check(scale==mc.getWindow().getGuiScale(),"Setting changed host scale");
            page.setFocused(null);skinToggle(host).setFocused(false);
            set(host,"scroll",630);lastClick=true;
            uiRow=new LinkedHashMap<>();uiRow.put("size",List.of(c[0],c[1]));uiRow.put("guiOption",c[2]);uiRow.put("scale",scale);uiRow.put("skinOptions",8);uiRow.put("closedButtons",1);uiRow.put("clicks",lastClick);uiRow.put("pageWidth",page.width);
        }
        if(phase>=10){
            var host=(CultivationMainScreen)mc.screen;var toggle=skinToggle(host);
            double x=-50,y=-50;
            if(phase>=11&&phase<=19){
                var b=toggle;double s=((Number)get(host,"pageScale")).doubleValue();
                x=(int)get(host,"contentX")+(b.getX()+b.getWidth()*.5)*s;
                y=(int)get(host,"contentY")+(b.getY()+b.getHeight()*.5-(int)get(host,"scroll"))*s;
            }
            // Synthetic client coordinates only; no GLFW/OS pointer movement.
            for(String axis:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(axis);f.setAccessible(true);f.setDouble(mc.mouseHandler,axis.equals("xpos")?x*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth():y*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight());}
            if(phase==20)toggle.setFocused(true);
            if(phase==22){toggle.setFocused(false);toggle.active=false;}
            if(phase==24){toggle.active=true;click(host,toggle);}
            if(phase==25){
                var page=(CultivationPage)get(host,"page");
                check(Boolean.TRUE.equals(get(page,"huahongSkinsExpanded")),"Dropdown failed to expand for review");
                verifyDropdownVisible(host);
                var options=page.children().stream().filter(w->w instanceof CultivationUi.Action a&&Arrays.stream(HuahongSkin.values()).anyMatch(s->s.label.equals(a.getMessage().getString()))).map(w->(AbstractWidget)w).toList();
                check(options.size()==8,"Expanded dropdown lost skin options");options.get(0).setFocused(true);options.get(1).active=false;
            }
            if(phase==10||phase==15||phase==20||phase==22||phase==25)pending="settings-"+c[0]+"x"+c[1]+"-gui"+c[2]+(phase==10?"-normal.png":phase==15?"-hover.png":phase==20?"-focus.png":phase==22?"-disabled.png":"-expanded-focus-disabled.png");
        }
        return false;
    }
    static void frame(Minecraft mc,Path out,List<Object> rows)throws Exception{
        if(pending==null)return;
        PointerSafety.verify();int gl=org.lwjgl.opengl.GL11.glGetError();check(gl==0,"Skin GL error "+gl);
        if(Boolean.getBoolean("wuxingxiantu.myriadExternalShaders"))check(TrailShaderBridge.shaderPackDetected(),"Requested external shaderpack is inactive in UI/skin capture");
        if(pending.startsWith("skin-")){
            check(HuahongVisuals.vertices>0,"Skin has no actual mesh");
            check(mc.player.getVehicle() instanceof ControlledSwordEntity s&&s.huahongStage()==2&&s.huahongStrength()>=.995F,"Skin left stable cruise before frame: "+(mc.player.getVehicle() instanceof ControlledSwordEntity s?HuahongCapture.flightDiagnostics(s):"no sword vehicle"));
        }
        if(pending.startsWith("settings")&&pending.endsWith("-hover.png")){
            var host=(CultivationMainScreen)mc.screen;var button=skinToggle(host);
            check(button.isHovered(),"Skin dropdown hover missed its rendered hitbox: "+hoverDiagnostics(mc,host,button));
        }
        if(pending.startsWith("sense-shortcuts-")&&pending.endsWith("-hover.png")){
            var host=(CultivationMainScreen)mc.screen;var buttons=footerButtons(host);var button=buttons.get(pending.endsWith("-creation-hover.png")?0:1);
            check(button.isHovered(),"Sense shortcut hover missed its rendered hitbox: "+hoverDiagnostics(mc,host,button));
        }
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            if(pending.startsWith("settings")||pending.startsWith("sense-shortcuts-")){
                var host=(CultivationMainScreen)mc.screen;
                var expected=(List<?>)uiRow.get("size");
                check(image.getWidth()==((Number)expected.get(0)).intValue()&&image.getHeight()==((Number)expected.get(1)).intValue(),"UI image resolution does not match requested case: "+pending+" actual="+image.getWidth()+"x"+image.getHeight());
                check(get(host,"pageId").equals(pending.startsWith("settings")?"settings":"sense"),"UI screenshot would capture the wrong page: "+pending+" page="+get(host,"pageId"));
                uiRow.put("actualImageSize",List.of(image.getWidth(),image.getHeight()));uiRow.put("pageId",get(host,"pageId"));
                uiRow.put("contentOrigin",List.of(get(host,"contentX"),get(host,"contentY")));uiRow.put("pageScale",get(host,"pageScale"));uiRow.put("scroll",get(host,"scroll"));
                if(pending.startsWith("settings")&&pending.contains("-expanded-")){verifyDropdownVisible(host);uiRow.put("expandedOptionsVisible",8);}
                if(pending.startsWith("sense-shortcuts-"))uiRow.put("shortcuts",footerButtons(host).stream().map(b->Map.of("label",b.getMessage().getString(),"bounds",List.of(b.getX(),b.getY(),b.getWidth(),b.getHeight()),"active",b.active,"hovered",b.isHovered(),"focused",b.isFocused())).toList());
            }
            image.writeToFile(out.resolve(pending));
            if(pending.startsWith("sense-shortcuts-")){
                var host=(CultivationMainScreen)mc.screen;
                for(var button:footerButtons(host))captureLeft(image,host,button,out.resolve(pending.replace(".png",button.getMessage().getString().startsWith("创世")?"-creation-left4x.png":"-ascension-left4x.png")));
            }
        }
        var row=new LinkedHashMap<String,Object>();row.put("image",pending);row.put("vertices",HuahongVisuals.vertices);row.put("shaderPack",TrailShaderBridge.shaderPackDetected());row.put("glError",gl);
        if(pending.startsWith("skin-")&&mc.player.getVehicle() instanceof ControlledSwordEntity s){row.put("stage",s.huahongStage());row.put("strength",s.huahongStrength());row.put("yaw",s.getYRot());row.put("view",HuahongCapture.FULL_SPEED?(pending.endsWith("-side.png")?"near-side":"near-axis"):"side-or-turn");}
        if(pending.startsWith("settings")||pending.startsWith("sense-shortcuts-"))row.putAll(uiRow);rows.add(row);pending=null;
    }
    private static boolean footerTick(Minecraft mc,int elapsed,List<Object> rows)throws Exception{
        if(!net.minecraftforge.fml.ModList.get().isLoaded("wuxingchuangshi")){
            if(elapsed==0)rows.add(Map.of("senseShortcuts","skipped","reason","Optional wuxingchuangshi fixture is absent; use -PheavenlySmallWorld"));
            return true;
        }
        int index=elapsed/32,phase=elapsed%32;if(index>=CASES.length)return true;int[] c=CASES[index];
        var state=new net.minecraft.nbt.CompoundTag();state.putBoolean("SenseOpen",true);state.putFloat("Sense",3600);state.putInt("Capacity",3600);state.putInt("Radius",64);state.putInt("Realm",99);PlayerProgressionClient.state=state;
        if(phase==0){
            mc.setScreen(null);mc.getWindow().setWindowed(c[0],c[1]);mc.getWindow().setWidth(c[0]);mc.getWindow().setHeight(c[1]);mc.options.guiScale().set(c[2]);mc.resizeDisplay();mc.options.hideGui=false;
            var host=new CultivationMainScreen(mc.player,"sense");mc.setScreen(host);var buttons=footerButtons(host);
            check(buttons.size()==2,"Missing actual sense addon shortcuts");
            set(host,"scroll",Math.max(0,buttons.get(0).getY()-100));
            var page=(CultivationPage)get(host,"page");
            for(var button:buttons)check(button instanceof CultivationUi.Action&&button.getX()>=0&&button.getX()+button.getWidth()<=page.width,"Shortcut uses vanilla appearance or exceeds page");
            uiRow=new LinkedHashMap<>();uiRow.put("size",List.of(c[0],c[1]));uiRow.put("guiOption",c[2]);uiRow.put("scale",mc.getWindow().getGuiScale());uiRow.put("shortcuts",buttons.stream().map(b->Map.of("label",b.getMessage().getString(),"bounds",List.of(b.getX(),b.getY(),b.getWidth(),b.getHeight()))).toList());
        }
        var host=(CultivationMainScreen)mc.screen;var buttons=footerButtons(host);double x=-50,y=-50;
        if(phase>=10&&phase<=15){var b=buttons.get(phase<13?0:1);double s=((Number)get(host,"pageScale")).doubleValue();x=(int)get(host,"contentX")+(b.getX()+b.getWidth()*.5)*s;y=(int)get(host,"contentY")+(b.getY()+b.getHeight()*.5-(int)get(host,"scroll"))*s;}
        for(String axis:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(axis);f.setAccessible(true);f.setDouble(mc.mouseHandler,axis.equals("xpos")?x*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth():y*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight());}
        if(phase==20){buttons.get(0).setFocused(true);buttons.get(1).setFocused(false);}
        if(phase==22){buttons.get(0).setFocused(false);buttons.get(1).setFocused(true);}
        if(phase==24)for(var b:buttons){b.setFocused(false);b.active=false;}
        if(phase==8||phase==12||phase==15||phase==20||phase==22||phase==24)
            pending="sense-shortcuts-"+c[0]+"x"+c[1]+"-gui"+c[2]+(phase==8?"-normal.png":phase==12?"-creation-hover.png":phase==15?"-ascension-hover.png":phase==20?"-creation-focus.png":phase==22?"-ascension-focus.png":"-disabled.png");
        return false;
    }
    private static List<AbstractWidget> footerButtons(CultivationMainScreen host)throws Exception{
        var page=(CultivationPage)get(host,"page");
        return page.children().stream().filter(w->w instanceof AbstractWidget a&&(a.getMessage().getString().equals("创世 · 管理小世界")||a.getMessage().getString().equals("天仙渡劫"))).map(w->(AbstractWidget)w).sorted(Comparator.comparing(b->b.getMessage().getString().startsWith("创世")?0:1)).toList();
    }
    private static void captureLeft(com.mojang.blaze3d.platform.NativeImage image,CultivationMainScreen host,AbstractWidget button,Path out)throws Exception{
        double s=((Number)get(host,"pageScale")).doubleValue(),sx=image.getWidth()/(double)host.width,sy=image.getHeight()/(double)host.height;
        int x=(int)Math.floor(((int)get(host,"contentX")+button.getX()*s)*sx),y=(int)Math.floor(((int)get(host,"contentY")+(button.getY()-(int)get(host,"scroll"))*s)*sy);
        int w=(int)Math.ceil(Math.min(64,button.getWidth())*s*sx),h=(int)Math.ceil(button.getHeight()*s*sy);
        check(x>=0&&y>=0&&x+w<=image.getWidth()&&y+h<=image.getHeight(),"Shortcut screenshot crop exceeds framebuffer");
        try(var crop=new com.mojang.blaze3d.platform.NativeImage(w*4,h*4,false)){
            for(int px=0;px<w*4;px++)for(int py=0;py<h*4;py++)crop.setPixelRGBA(px,py,image.getPixelRGBA(x+px/4,y+py/4));crop.writeToFile(out);
        }
    }
    private static Object get(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static Map<String,Object> hoverDiagnostics(Minecraft mc,CultivationMainScreen host,AbstractWidget button)throws Exception{
        double mx=((Number)get(mc.mouseHandler,"xpos")).doubleValue()*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth();
        double my=((Number)get(mc.mouseHandler,"ypos")).doubleValue()*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight();
        double scale=((Number)get(host,"pageScale")).doubleValue();
        return Map.of("image",pending,"cursorGui",List.of(mx,my),"cursorPage",List.of((mx-(int)get(host,"contentX"))/scale,(my-(int)get(host,"contentY"))/scale+(int)get(host,"scroll")),"bounds",List.of(button.getX(),button.getY(),button.getWidth(),button.getHeight()),"contentOrigin",List.of(get(host,"contentX"),get(host,"contentY")),"pageScale",scale,"scroll",get(host,"scroll"));
    }
    private static AbstractWidget skinToggle(CultivationMainScreen host)throws Exception{
        var page=(CultivationPage)get(host,"page");
        var selector=(AbstractWidget)get(page,"huahongSkinSelector");
        check(page.children().contains(selector),"Dropdown selector is not a live page control");
        check(selector instanceof CultivationUi.Action action&&action.chrome==1&&selector.getHeight()==20,"Dropdown no longer matches the domain control");
        return selector;
    }
    private static void verifyDropdownVisible(CultivationMainScreen host)throws Exception{
        var page=(CultivationPage)get(host,"page");double scale=((Number)get(host,"pageScale")).doubleValue();
        double top=((Number)get(host,"scroll")).doubleValue(),bottom=top+((Number)get(host,"contentH")).doubleValue()/scale;
        var choices=page.children().stream().filter(w->w instanceof AbstractWidget a&&Arrays.stream(HuahongSkin.values()).anyMatch(s->s.label.equals(a.getMessage().getString()))).map(w->(AbstractWidget)w).toList();
        check(choices.size()==8,"Expanded skin dropdown did not contain eight options");
        var selector=skinToggle(host);
        for(var button:java.util.stream.Stream.concat(java.util.stream.Stream.of(selector),choices.stream()).toList())
            check(button.visible&&button.getX()>=0&&button.getX()+button.getWidth()<=page.width&&button.getY()>=top&&button.getY()+button.getHeight()<=bottom,
                    "Expanded skin control is outside actual host viewport: "+button.getMessage().getString()+" bounds="+List.of(button.getX(),button.getY(),button.getWidth(),button.getHeight())+" viewport="+List.of(top,bottom));
        for(int i=0;i<choices.size();i++)for(int j=i+1;j<choices.size();j++){
            var a=choices.get(i);var b=choices.get(j);
            check(a.getX()+a.getWidth()<=b.getX()||b.getX()+b.getWidth()<=a.getX()||a.getY()+a.getHeight()<=b.getY()||b.getY()+b.getHeight()<=a.getY(),"Expanded skin options overlap: "+a.getMessage().getString()+" / "+b.getMessage().getString());
        }
    }
    private static void set(Object object,String name,Object value)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    private static void click(CultivationMainScreen host,AbstractWidget widget)throws Exception{
        double scale=((Number)get(host,"pageScale")).doubleValue(),x=(int)get(host,"contentX")+(widget.getX()+widget.getWidth()*.5)*scale;
        double y=(int)get(host,"contentY")+(widget.getY()+widget.getHeight()*.5-(int)get(host,"scroll"))*scale;
        check(host.mouseClicked(x,y,0),"Host click missed "+widget.getMessage().getString());host.mouseReleased(x,y,0);
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
