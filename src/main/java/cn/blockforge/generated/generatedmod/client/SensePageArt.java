package cn.blockforge.generated.generatedmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Formal card and addon skins extracted from the user-approved sense reference. */
public final class SensePageArt {
    private static final Map<String,int[]> SIZES=Map.ofEntries(
        Map.entry("panel",new int[]{599,206}),Map.entry("status",new int[]{1229,81}),
        Map.entry("button",new int[]{185,63}),Map.entry("creation",new int[]{567,93}),Map.entry("ascension",new int[]{381,94}),
        Map.entry("scan",new int[]{96,91}),Map.entry("identify",new int[]{117,93}),Map.entry("lock",new int[]{99,94}),Map.entry("survey",new int[]{111,99}),
        Map.entry("cost",new int[]{31,38}),Map.entry("creation_icon",new int[]{184,79}),Map.entry("ascension_icon",new int[]{104,75}),Map.entry("arrow",new int[]{30,33}));
    private static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath("wuxingxiantu","textures/gui/sense_v2/"+name+".png");}
    public static void frame(GuiGraphics g,String name,int x,int y,int w,int h){
        int[] size=SIZES.get(name);int sw=size[0],sh=size[1],s=name.equals("creation")||name.equals("ascension")?14:12,d=3;
        int[] u={0,s,sw-s,sw},v={0,s,sh-s,sh},xx={x,x+d,x+w-d,x+w},yy={y,y+d,y+h-d,y+h};
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        for(int r=0;r<3;r++)for(int c=0;c<3;c++)g.blit(texture(name),xx[c],yy[r],xx[c+1]-xx[c],yy[r+1]-yy[r],u[c],v[r],u[c+1]-u[c],v[r+1]-v[r],sw,sh);
        g.flush();RenderSystem.disableBlend();
    }
    public static void icon(GuiGraphics g,String name,int x,int y,int w,int h){
        int[] size=SIZES.get(name);g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        g.blit(texture(name),x,y,w,h,0,0,size[0],size[1],size[0],size[1]);g.flush();RenderSystem.disableBlend();
    }
    public static void addonSurface(GuiGraphics g,CultivationUi.Action b,boolean creation){
        float tint=!b.active?.45f:b.isHoveredOrFocused()?1f:.82f;g.setColor(tint,tint,tint,1);
        frame(g,creation?"creation":"ascension",b.getX(),b.getY(),b.getWidth(),b.getHeight());
        // These source crops include the inner left trim. Preserve their authored
        // offset and aspect ratio so that trim meets the button's left cap instead
        // of drawing a second, inset edge with an empty gutter beside it.
        double scale=b.getHeight()/(creation?93.0:94.0);
        icon(g,creation?"creation_icon":"ascension_icon",
                b.getX()+(int)Math.round((creation?12:10)*scale),b.getY()+(int)Math.round((creation?7:12)*scale),
                (int)Math.round((creation?184:104)*scale),(int)Math.round((creation?79:75)*scale));
        icon(g,"arrow",b.getX()+b.getWidth()-14,b.getY()+(b.getHeight()-8)/2,8,8);g.setColor(1,1,1,1);
    }
    public static class Button extends InkButton {
        public Button(int x,int y,int w,int h,String text,OnPress press){super(x,y,w,h,Component.literal(text),press);}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            float tint=!active?.45f:isHoveredOrFocused()?1f:.8f;g.setColor(tint,tint,tint,1);frame(g,"button",getX(),getY(),getWidth(),getHeight());g.setColor(1,1,1,1);
            var font=Minecraft.getInstance().font;String text=font.plainSubstrByWidth(getMessage().getString(),getWidth()-8);
            g.drawString(font,text,getX()+(getWidth()-font.width(text))/2,getY()+(getHeight()-9)/2,active?CultivationUi.TEXT:CultivationUi.MUTED,false);
        }
    }
    private SensePageArt(){}
}
