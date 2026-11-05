package cn.blockforge.generated.generatedmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;

/** 各页面共用的像素边框、按钮状态、数值格式与进度条。 */
public final class CultivationUi {
    private static final java.util.Map<String, net.minecraft.resources.ResourceLocation> TEXTURES = new java.util.HashMap<>();
    public static final int BACK = 0xEE0D1A23, PANEL = 0xE5172730, GOLD = 0xFFE8C88B;
    public static final int TEXT = 0xFFE6E3D5, MUTED = 0xFFB6B8AB, CYAN = 0xFF87BDCB;
    public static final int GREEN = 0xFF96BEA2, RED = 0xFFCF887A, BORDER = 0xFF998053;
    public static final int INK = 0xFF292A26, PAPER = 0xFFE5DBC5;
    private CultivationUi() {}
    public static void summaryIcon(GuiGraphics g, int index, int x, int y) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        String[] names = {"realm", "cultivation", "mana", "physique", "heart_method", "domain", "exotic_fire", "dao_heart", "heart_demon", "tribulation"};
        if(index==5||index==6)CultivationMaskIcons.draw(g,names[index],x,y,18);
        else if(index==0||index==3||index==4)CultivationVisionAssets.draw(g,names[index]+"_gold",x,y,18,18);
        else CultivationReferenceAssets.draw(g, "icons/summary/" + names[index], x, y, 18, 18);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    public static class Action extends Button {
        public java.util.function.BiConsumer<GuiGraphics,Action> customSurface;
        public int chrome=2;
        protected final BooleanSupplier selected;
        private long pressedUntil;
        public Action(int x, int y, int w, int h, Component label, OnPress action) {
            this(x, y, w, h, label, action, () -> false);
        }
        public Action(int x, int y, int w, int h, Component label, OnPress action, BooleanSupplier selected) {
            super(Button.builder(label, action).bounds(x, y, w, h));
            this.selected = selected;
        }
        @Override public void onPress() { pressedUntil = net.minecraft.Util.getMillis() + 140; super.onPress(); }
        protected void surface(GuiGraphics g, boolean chosen) {
            if(customSurface!=null){customSurface.accept(g,this);return;}
            boolean pressed = active && (net.minecraft.Util.getMillis() < pressedUntil || isHovered()
                    && org.lwjgl.glfw.GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), 0) == 1);
            if(width>=36)referenceButton(g,getX(),getY(),width,height,chrome,active,chosen,isHoveredOrFocused(),pressed);
            else buttonSurface(g, getX(), getY(), width, height, active, chosen, isHoveredOrFocused(), pressed);
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
            boolean chosen = selected.getAsBoolean();
            surface(g, chosen);
            var font = Minecraft.getInstance().font;
            fit(g, getMessage(), getX() + Math.max(4, (width - font.width(getMessage())) / 2), getY() + (height - 8) / 2,
                    width - 8, !active ? MUTED : chosen ? GOLD : TEXT, mx, my);
            if (isMouseOver(mx,my) && font.width(getMessage()) > width - 8 && Minecraft.getInstance().screen != null)
                Minecraft.getInstance().screen.setTooltipForNextRenderPass(font.split(getMessage(), 240));
        }
    }
    public static class Tab extends Action {
        public Tab(int x, int y, int w, int h, Component label, OnPress action, BooleanSupplier selected) {
            super(x, y, w, h, label, action, selected);
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
            boolean chosen = selected.getAsBoolean();
            surface(g, chosen);
            if (chosen) g.fill(getX() + 7, getY() + height - 3, getX() + width - 7, getY() + height - 2, GOLD);
            var font = Minecraft.getInstance().font;
            fit(g, getMessage(), getX() + Math.max(4, (width - font.width(getMessage())) / 2), getY() + (height - 8) / 2,
                    width - 8, !active ? MUTED : chosen ? GOLD : TEXT, mx, my);
        }
    }
    public static final class CloseButton extends Action {
        public CloseButton(int x,int y,int size,OnPress action) {
            super(x,y,size,size,Component.literal("关闭"),action);
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("关闭（Esc）")));
        }
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float delta) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            float tint=!active?.5F:isHoveredOrFocused()?1F:.86F;
            g.setColor(tint,tint,tint,1);
            g.blit(texture("close_reference"),getX(),getY(),width,height,0,0,256,256,256,256);
            g.setColor(1,1,1,1);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }
    public static class Navigation extends Action {
        private final int icon;
        public Navigation(int x, int y, int w, Component label, String pageId, OnPress action, BooleanSupplier selected) {
            this(x, y, w, 24, label, pageId, action, selected);
        }
        public Navigation(int x, int y, int w, int h, Component label, String pageId, OnPress action, BooleanSupplier selected) {
            super(x, y, w, h, label, action, selected);
            this.icon = switch (pageId) { case "inventory" -> 0; case "vision" -> 1; case "arts" -> 2; case "crafts" -> 6; case "sense" -> 7;
                case "sword" -> 3; case "advancements" -> 4; default -> 5; };
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
            boolean chosen = selected.getAsBoolean();
            NavigationUiAssets.surface(g,getX(),getY(),width,height,chosen,isHoveredOrFocused(),active);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            int size = height >= 32 ? 24 : 20;
            String[] names = {"inventory", "vision", "arts", "sword", "advancements", "settings", "crafts", "sense"};
            CultivationReferenceAssets.draw(g, "icons/navigation/" + names[icon],
                    getX() + (width < 44 ? (width - size) / 2 : 5), getY() + (height - size) / 2, size, size);
            if(width>=44)NavigationUiAssets.draw(g,"center_rule",getX()+size+7,getY()+7,2,height-14,32,128);
            g.setColor(1, 1, 1, 1);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            if (width >= 44) fit(g, getMessage(), getX() + size + 12, getY() + (height - 8) / 2, width - size - 16, !active ? MUTED : chosen ? GOLD : TEXT, mx, my);
            else if (isMouseOver(mx,my)) tooltip(getMessage());
        }
    }
    public static class Icon extends Action {
        private final Supplier<ItemStack> icon;
        private final Supplier<Component> hint;
        private final String index;
        private Supplier<Component> symbol;
        private int itemOffsetY = -2;
        public Icon itemOffsetY(int value) { itemOffsetY = value; return this; }
        public Icon symbol(Supplier<Component> value) { symbol = value; return this; }
        public Icon(int x, int y, int size, String index, Supplier<ItemStack> icon,
                    Supplier<Component> hint, OnPress action, BooleanSupplier selected) {
            super(x, y, size, size, hint.get(), action, selected);
            this.icon = icon; this.hint = hint; this.index = index;
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
            slot(g, getX(), getY(), width, height, selected.getAsBoolean() ? GOLD : BORDER);
            if (isHoveredOrFocused()) g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x2249B4BC);
            ItemStack stack = icon.get();
            if (symbol != null) g.drawCenteredString(Minecraft.getInstance().font, symbol.get(), getX() + width / 2, getY() + 5, GOLD);
            else if (!stack.isEmpty()) g.renderItem(stack, getX() + (width - 16) / 2, getY() + (height - 16) / 2 + itemOffsetY);
            else g.drawCenteredString(Minecraft.getInstance().font, active ? "+" : "-", getX() + width / 2, getY() + 5, MUTED);
            if (!active) g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x99252C2F);
            g.drawString(Minecraft.getInstance().font, index, getX() + 2, getY() + height - 9, active ? TEXT : MUTED, false);
            setMessage(hint.get());
            if (isMouseOver(mx,my)) tooltip(hint.get());
        }
    }
    public static class Toggle extends Action {
        private final ForgeConfigSpec.BooleanValue value;
        public Toggle(int x, int y, int w, Component label, ForgeConfigSpec.BooleanValue value) {
            super(x, y, w, 22, label, b -> { value.set(!value.get()); value.save(); });
            this.value = value;
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
            var font = Minecraft.getInstance().font;
            surface(g, false);
            fit(g, getMessage(), getX() + 5, getY() + 7, width - 43, TEXT, mx, my);
            int x=getX()+width-SettingsUiAssets.RIGHT_INSET-SettingsUiAssets.TOGGLE_WIDTH;
            int y=getY()+(height-SettingsUiAssets.TOGGLE_HEIGHT)/2;
            SettingsUiAssets.toggle(g,x,y,value.get(),active);
            if (isMouseOver(mx,my)) tooltip(getMessage().copy().append(value.get() ? "：开" : "：关"));
        }
    }
    public static class Slider extends AbstractSliderButton {
        private final ForgeConfigSpec.DoubleValue setting;
        private final double min, max, step;
        private final String label;
        public Slider(int x, int y, int w, String label, ForgeConfigSpec.DoubleValue setting, double min, double max, double step) {
            super(x, y, w, 30, Component.literal(label), (setting.get() - min) / (max - min));
            this.setting = setting; this.min = min; this.max = max; this.step = step; this.label = label;
            updateMessage();
        }
        @Override protected void updateMessage() {
            if (setting != null) setMessage(Component.literal(label + "：" + String.format(Locale.ROOT, "%.2f", setting.get())));
        }
        @Override protected void applyValue() {
            double raw = min + value * (max - min);
            double rounded = value >= 1 ? max : value <= 0 ? min : Math.round(raw / step) * step;
            setting.set(Math.max(min, Math.min(max, Math.round(rounded * 100) / 100.0)));
            updateMessage();
        }
        @Override public void onRelease(double x, double y) { super.onRelease(x, y); setting.save(); }
        @Override public boolean keyPressed(int key, int scan, int mods) {
            boolean handled = super.keyPressed(key, scan, mods);
            if (handled) setting.save();
            return handled;
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float delta) {
            fit(g, getMessage(), getX(), getY() + 2, width, TEXT, mx, my);
            SettingsUiAssets.slider(g,getX(),getY(),width,value,active,isHoveredOrFocused());
        }
    }
    /** Exact button chrome used by the backpack/inventory UI, shared by secondary screens. */
    public static void backpackButton(GuiGraphics g,int x,int y,int w,int h,boolean enabled,boolean selected,boolean hovered,boolean pressed){
        referenceButton(g,x,y,w,h,2,enabled,selected,hovered,pressed);
    }
    private static void referenceButton(GuiGraphics g,int x,int y,int w,int h,int style,boolean enabled,boolean selected,boolean hovered,boolean pressed){
        int u=style==1?32:47,v=style==1?281:733,sw=style==1?1191:1161,sh=style==1?248:316;
        int cap=Math.min(6,h/4),center=Math.min(9,w/6);
        int[] sx={0,90,(sw-100)/2,(sw+100)/2,sw-90,sw},dx={0,cap,(w-center)/2,(w+center)/2,w-cap,w};
        int[] sy={0,70,sh-70,sh},dy={0,cap,h-cap,h};
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        // Tint the button artwork only; restore white before drawing its label.
        float tint=!enabled?.52F:pressed?.62F:hovered?1F:selected?.85F:.68F;g.setColor(tint,tint,tint,1);
        var asset=texture("button/reference_atlas");
        for(int row=0;row<3;row++)for(int col=0;col<5;col++)g.blit(asset,x+dx[col],y+dy[row],dx[col+1]-dx[col],dy[row+1]-dy[row],u+sx[col],v+sy[row],sx[col+1]-sx[col],sy[row+1]-sy[row],1254,1254);
        g.setColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    private static void buttonSurface(GuiGraphics g, int x, int y, int w, int h, boolean enabled, boolean selected, boolean hovered, boolean pressed) {
        String state = !enabled ? "disabled" : pressed ? "pressed" : selected ? "selected" : hovered ? "hover" : "normal";
        g.fill(x + 1, y + 2, x + w, y + h + 1, 0x5510110F);
        float tint=!enabled?1F:pressed?.78F:hovered?1F:selected?.9F:.75F;
        g.setColor(tint,tint,tint,1);
        nineSlice(g, "button/button_" + state, x, y, w, h, 32, 24, 4);
        g.setColor(1,1,1,1);
    }
    public static void slot(GuiGraphics g, int x, int y, int w, int h, int border) {
        CultivationReferenceAssets.draw(g, "slot/empty", x, y, w, h);
        if (border == GOLD) g.renderOutline(x, y, w, h, GOLD);
    }
    public static void equipmentSlot(GuiGraphics g, String name, int x, int y, int size) {
        CultivationReferenceAssets.draw(g, "slot/" + name, x, y, size, size);
    }
    public static void background(GuiGraphics g, String name, int x, int y, int w, int h) {
        CultivationReferenceAssets.draw(g, "background/" + name, x, y, w, h);
    }
    public static void referenceFrame(GuiGraphics g, String name, int x, int y, int w, int h) {
        nineSlice(g, "reference/frame/" + name, x, y, w, h, 32, 32, 5);
    }
    public static void paper(GuiGraphics g, int x, int y, int w, int h) {
        for (int row = 0; row < h; row += 64) for (int col = 0; col < w; col += 64)
            g.blit(texture("background/paper_base"), x + col, y + row, 0, 0, Math.min(64, w - col), Math.min(64, h - row), 64, 64);
    }
    public static void ornamentFrame(GuiGraphics g, int x, int y, int w, int h) {
        CultivationOuterFrame.draw(g, x, y, w, h);
    }
    /** One-pixel bright rail aligned to the entity viewport, without stretching a noisy crop. */
    public static void portraitFrame(GuiGraphics g,int x,int y,int w,int h) {
        int gold=0xFFFADB8B;
        g.fill(x+2,y,x+w-2,y+1,gold);g.fill(x+2,y+h-1,x+w-2,y+h,gold);
        g.fill(x,y+2,x+1,y+h-2,gold);g.fill(x+w-1,y+2,x+w,y+h-2,gold);
        for(int dx:new int[]{1,w-2})for(int dy:new int[]{1,h-2})g.fill(x+dx,y+dy,x+dx+1,y+dy+1,gold);
    }
    private static net.minecraft.resources.ResourceLocation texture(String name) {
        return TEXTURES.computeIfAbsent(name, key -> new net.minecraft.resources.ResourceLocation("wuxingxiantu", "textures/gui/wuxia/" + key + ".png"));
    }
    // Keep corners at native GUI size; stretch only the edges and center.
    private static void nineSlice(GuiGraphics g, String name, int x, int y, int w, int h, int tw, int th, int edge) {
        if (w <= 0 || h <= 0) return;
        int e = Math.min(edge, Math.min(w, h) / 2);
        int[] dx = {0, e, w - e, w}, dy = {0, e, h - e, h};
        int[] sx = {0, e, tw - e, tw}, sy = {0, e, th - e, th};
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            if (dx[col + 1] > dx[col] && dy[row + 1] > dy[row])
                g.blit(texture(name), x + dx[col], y + dy[row], dx[col + 1] - dx[col], dy[row + 1] - dy[row],
                        sx[col], sy[row], sx[col + 1] - sx[col], sy[row + 1] - sy[row], tw, th);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    static void pickerPanel(GuiGraphics g, int x, int y, int w, int h, String title) {
        g.fill(x-2,y-2,x+w+2,y+h+2,0xFF101E26);
        g.renderOutline(x-2,y-2,w+4,h+4,GOLD);
        g.drawString(Minecraft.getInstance().font,title,x+4,y+3,GOLD,false);
    }
    public static void panel(GuiGraphics g, int x, int y, int w, int h, boolean primary) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        var resource = texture("panel/panel_" + (primary ? "main" : "secondary"));
        for (int row = 0; row < h; row += 32) for (int col = 0; col < w; col += 32)
            g.blit(resource, x + col, y + row, 0, 0, Math.min(32, w - col), Math.min(32, h - row), 32, 32);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        int edge=primary?0x74B7A181:0x646F674F;
        softDivider(g,x,y,w,false,edge);softDivider(g,x,y+h-1,w,false,edge);
        softDivider(g,x,y,h,true,edge);softDivider(g,x+w-1,y,h,true,edge);
    }
    public static void scenery(GuiGraphics g, int x, int y, int w, int h) {
        // Preserve the supplied painting's aspect ratio with a centered cover crop.
        int tw = 1672, th = 941;
        double scale = Math.max(w / (double) tw, h / (double) th);
        int sw = Math.min(tw, (int) Math.round(w / scale)), sh = Math.min(th, (int) Math.round(h / scale));
        g.blit(texture("background/landscape"), x, y, w, h, (tw - sw) / 2F, (th - sh) / 2F, sw, sh, tw, th);
    }
    private static void pixel(GuiGraphics g, int x, int y, int color) { g.fill(x, y, x + 1, y + 1, color); }
    public static void rule(GuiGraphics g, int x, int y, int w) {
        CultivationBorderAssets.draw(g, "rule_gold", x, y - 2, w, 4);
    }
    /** Shared extracted gold rail and capped thumb, preserving each screen's scroll math. */
    public static void scrollbar(GuiGraphics g,int x,int y,int h,int top,int thumb) {
        var tex=ResourceLocation.fromNamespaceAndPath("wuxingxiantu","textures/gui/wuxia/scrollbar_reference.png");
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        // Keep both ornaments inside the rail's bounds, including clipped pages.
        int railCap=6;
        g.blit(tex,x-2,y,6,railCap,0,0,42,100,84,1384);
        g.blit(tex,x-2,y+railCap,6,Math.max(1,h-railCap*2),0,500,42,1,84,1384);
        // The atlas already contains the mirrored cap. A negative pose scale
        // reverses the quad winding and can make this cap disappear under culling.
        g.blit(tex,x-2,y+h-railCap,6,railCap,0,1284,42,100,84,1384);
        int cap=Math.min(4,thumb/2);
        g.blit(tex,x-2,top,6,cap,42,0,42,40,84,1384);
        g.blit(tex,x-2,top+cap,6,Math.max(1,thumb-cap*2),42,40,42,240,84,1384);
        g.blit(tex,x-2,top+thumb-cap,6,cap,42,280,42,40,84,1384);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    /** Subtle frame separator: one GUI pixel, with softly fading ends. */
    public static void softDivider(GuiGraphics g,int x,int y,int length,boolean vertical) {
        softDivider(g,x,y,length,vertical,0x74B7A181);
    }
    public static void softDivider(GuiGraphics g,int x,int y,int length,boolean vertical,int tint) {
        if(length<=0)return;
        int fade=Math.max(1,(int)Math.ceil(length*.14));
        int transparent=tint&0xFFFFFF;
        // Three batched rectangles, rather than many tiny fill calls per separator.
        g.pose().pushPose();g.pose().translate(x,vertical?y:y+1,0);
        if(!vertical)g.pose().mulPose(new org.joml.Quaternionf().rotationZ(-(float)Math.PI/2));
        g.fillGradient(0,0,1,fade,transparent,tint);
        if(length>fade*2)g.fill(0,fade,1,length-fade,tint);
        g.fillGradient(0,Math.max(fade,length-fade),1,length,tint,transparent);
        g.pose().popPose();
    }
    public static void tooltip(Component text) {
        var mc = Minecraft.getInstance();
        if (mc.screen != null) mc.screen.setTooltipForNextRenderPass(mc.font.split(text, Math.min(240, mc.screen.width - 16)));
    }
    public static void fit(GuiGraphics g, Component text, int x, int y, int w, int color, int mx, int my) {
        var font = Minecraft.getInstance().font;
        boolean clipped = font.width(text) > w;
        if (clipped) g.drawString(font, font.plainSubstrByWidth(text.getString(), Math.max(0, w - font.width("..."))) + "...", x, y, color, false);
        else g.drawString(font, text, x, y, color, false);
        if (clipped && mx >= x && mx < x + w && my >= y && my < y + 10) tooltip(text);
    }
    public static String number(double value) {
        double magnitude = Math.abs(value);
        if (magnitude >= 100000000) return compactNumber(value / 100000000) + "亿";
        if (magnitude >= 10000) return compactNumber(value / 10000) + "万";
        return String.format(Locale.ROOT, "%.0f", value);
    }
    private static String compactNumber(double value) {
        return String.format(Locale.ROOT, Math.abs(value - Math.rint(value)) < .05 ? "%.0f" : "%.1f", value);
    }
    public static void bar(GuiGraphics g, String label, int x, int y, int w, double value, double max, int color, int mx, int my) {
        var font = Minecraft.getInstance().font;
        fit(g, Component.literal(label + " " + number(value) + " / " + number(max)), x, y, w, TEXT, mx, my);
        String asset = label.equals("生命") ? "bar_health" : label.equals("灵力") ? "bar_mana" : "bar_cultivation";
        CultivationVisionAssets.draw(g, asset + "_track", x, y + 12, w, 7);
        CultivationVisionAssets.drawProgress(g, asset, x, y + 12, w, 7, value / Math.max(1, max));
        if (mx >= x && mx < x + w && my >= y && my < y + 18) {
            var screen = Minecraft.getInstance().screen;
            if (screen != null) screen.setTooltipForNextRenderPass(font.split(label.equals("灵力")?manaTooltip(cn.blockforge.generated.generatedmod.GeneratedMod.data(Minecraft.getInstance().player),value,max):Component.literal(String.format(Locale.ROOT, "%s：%,.0f / %,.0f", label, value, max)), 240));
        }
    }
    static Component manaTooltip(cn.blockforge.generated.generatedmod.CultivationData data,double value,double maximum){
        float recovery=data==null?0:cn.blockforge.generated.generatedmod.items.CultivationLogic.regeneration(data,data.wuxingxiantu$isMeditating());
        return Component.literal("灵力："+number(value)+" / "+number(maximum)+"\n当前恢复："+number(recovery)+" / 秒").withStyle(net.minecraft.ChatFormatting.AQUA);
    }
}
