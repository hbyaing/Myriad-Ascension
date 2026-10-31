package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import java.util.*;

/** 同一原版容器内切页；仅在本界面调整槽位坐标，关闭时还原。 */
public final class CultivationMainScreen extends InventoryScreen {
    private final Map<String, CultivationPage> pages = new HashMap<>();
    private List<CultivationPageRegistry.Entry> navigation;
    private final List<CultivationUi.Action> navButtons = new ArrayList<>();
    private CultivationPage page;
    private String pageId;
    private int panelX, panelY, panelW, panelH, contentX, contentY, contentW, contentH, scroll;
    private CultivationLayout layout;
    private net.minecraft.client.gui.components.AbstractWidget recipeButton;
    private net.minecraft.client.gui.components.AbstractWidget accessoriesButton;
    void attachAccessories(net.minecraft.client.gui.components.AbstractWidget button) {
        accessoriesButton = button;
        positionInventory();
    }
    private final Map<Slot, int[]> originalSlotPositions = new IdentityHashMap<>();
    private final Map<String, Integer> pageScroll = new HashMap<>();
    private boolean pointerInPage, draggingScrollbar;
    private boolean recipeButtonClicked;
    private double scrollbarGrab;
    private float pageScale = 1;
    private CultivationUi.Action close;

    public CultivationMainScreen(Player player) { this(player, "inventory"); }
    public CultivationMainScreen(Player player, String pageId) {
        super(player); this.pageId = pageId;
        imageWidth = CultivationLayout.INVENTORY_WIDTH; imageHeight = CultivationLayout.INVENTORY_HEIGHT;
    }
    private boolean inventory() { return page == null; }
    boolean hasCustomPage() { return !inventory(); }
    /** Match vanilla drawing, rather than the outer-window getters used by JEI. */
    public List<net.minecraft.client.renderer.Rect2i> jeiEffectAreas() {
        if(!inventory() || minecraft.player==null) return List.of();
        var effects=minecraft.player.getActiveEffects().stream()
                .filter(effect->net.minecraftforge.client.extensions.common.IClientMobEffectExtensions.of(effect).isVisibleInInventory(effect)).toList();
        int x=leftPos+imageWidth+2;
        if(effects.isEmpty() || width-x<32) return List.of();
        int step=effects.size()>5 ? 132/(effects.size()-1) : 33;
        var areas=new ArrayList<net.minecraft.client.renderer.Rect2i>();
        for(int i=0;i<effects.size();i++) areas.add(new net.minecraft.client.renderer.Rect2i(x,topPos+i*step,width-x>=120?120:32,32));
        return areas;
    }
    boolean ownsClick(double x,double y) {
        if(!inventory())return x>=panelX&&x<panelX+panelW&&y>=panelY&&y<panelY+panelH;
        if(getRecipeBookComponent().isVisible()&&getRecipeBookComponent().isMouseOver(x,y))return false;
        return close.isMouseOver(x,y)||(recipeButton!=null&&recipeButton.isMouseOver(x,y))
                ||navButtons.stream().anyMatch(b->b.isMouseOver(x,y));
    }
    // Forge/JEI must reserve all three columns, including navigation and the overview.
    // Actual inventory slots keep their own leftPos/topPos and imageWidth/imageHeight.
    @Override public int getGuiLeft() { return layout==null ? leftPos : panelX; }
    @Override public int getGuiTop() { return layout==null ? topPos : panelY; }
    @Override public int getXSize() { return layout==null ? imageWidth : panelW; }
    @Override public int getYSize() { return layout==null ? imageHeight : panelH; }
    @Override public net.minecraft.world.inventory.Slot getSlotUnderMouse() {
        return inventory()&&!getRecipeBookComponent().isVisible() ? super.getSlotUnderMouse() : null;
    }
    @Override protected void init() {
        InventoryResolutionFit.apply(minecraft);
        width=minecraft.getWindow().getGuiScaledWidth();
        height=minecraft.getWindow().getGuiScaledHeight();
        // Use vanilla's modal recipe rendering, but give it a dedicated page instead of covering slots.
        int screenWidth = width;
        width = Math.min(width, 378);
        try { super.init(); } finally { width = screenWidth; }
        recipeButton = children().stream().filter(e -> e instanceof net.minecraft.client.gui.components.ImageButton)
                .map(e -> (net.minecraft.client.gui.components.AbstractWidget)e).findFirst().orElse(null);
        if (recipeButton != null) removeWidget(recipeButton);
        recipeButton = addRenderableWidget(new CultivationUi.Icon(0, 0, 20, "",
                () -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK),
                () -> Component.translatable(getRecipeBookComponent().isVisible()
                        ?"screen.wuxingxiantu.recipe_close":"screen.wuxingxiantu.recipe_open"),
                b -> { getRecipeBookComponent().toggleVisibility(); positionInventory(); recipeButtonClicked = true; },
                () -> getRecipeBookComponent().isVisible()).itemOffsetY(0));
        layout = CultivationLayout.of(width, height);
        imageWidth = layout.inventoryWidth(); imageHeight = layout.inventoryHeight();
        panelW = layout.width(); panelH = layout.height(); panelX = layout.x(); panelY = layout.y();
        contentX = layout.contentX(); contentY = layout.contentY();
        contentW = layout.contentWidth(); contentH = layout.contentHeight();
        pageScale = 1; // Native item icons and text must not be shrunk with the whole page.
        navigation = CultivationPageRegistry.visible();
        navButtons.clear();
        for (int i = 0; i < navigation.size(); i++) {
            var entry = navigation.get(i);
            navButtons.add(new CultivationUi.Navigation(panelX + 6, layout.navY(i), layout.sidebarWidth(), layout.navigationHeight(),
                    Component.literal(entry.title()), entry.id(),
                    b -> selectPage(entry.id()), () -> pageId.equals(entry.id())));
        }
        close = new CultivationUi.CloseButton(panelX + panelW - 25, panelY + (layout.headerHeight() - 18) / 2, 18,
                b -> onClose());
        if (minecraft.player.isCreative()) {
            addRenderableWidget(new InkButton(panelX + 100, panelY + 5, 72, 18,
                    Component.literal("创造物品"), b -> CreativeInventorySkin.openCatalog()));
        }
        // Vanilla derives both drawing and hit areas from these virtual dimensions.
        // Include the left category tabs when centering within our content pane.
        int bookX=contentX+(contentW-147+28-24)/2;
        int bookY=contentY+Math.max(0,(contentH-166)/2);
        getRecipeBookComponent().init(2*bookX+147,2*bookY+166,minecraft,true,getMenu());
        positionInventorySlots();
        positionInventory();
        String initialPage = pageId;
        if (!getMenu().getCarried().isEmpty() || isQuickCrafting) { page = null; pageId = "inventory"; }
        else selectPage(initialPage);
    }
    public void selectPage(String id) {
        // 手持物品时不换页，避免隐藏正在进行的拖拽和合成操作。
        if (!getMenu().getCarried().isEmpty() || isQuickCrafting) return;
        var entry = navigation.stream().filter(e -> e.id().equals(id)).findFirst().orElse(navigation.get(0));
        if (page != null) { pageScroll.put(pageId, scroll); page.removed(); }
        pageId = entry.id(); scroll = pageScroll.getOrDefault(pageId, 0); pointerInPage = draggingScrollbar = false;
        page = entry.factory() == null ? null : pages.computeIfAbsent(pageId, key -> entry.factory().get());
        hoveredSlot = null;
        setFocused(null);
        if (recipeButton != null) { recipeButton.visible = inventory(); recipeButton.active = inventory(); }
        if (!inventory() && getRecipeBookComponent().isVisible()) getRecipeBookComponent().toggleVisibility();
        if (page != null) {
            page.attach(this);
            page.init(minecraft, pageWidth(), page.contentHeight(pageWidth(), contentViewportHeight()));
            scroll = Math.max(0, Math.min(scroll, page.height - contentViewportHeight()));
        }
    }
    private void positionInventory() {
        leftPos = layout.inventoryX(); topPos = layout.inventoryY();
        if (accessoriesButton != null) {
            accessoriesButton.visible = inventory() && !getRecipeBookComponent().isVisible();
            accessoriesButton.active = accessoriesButton.visible && getMenu().getCarried().isEmpty() && !isQuickCrafting;
            accessoriesButton.setX(leftPos + (layout.spaciousInventory() ? 42 : 30) + 2);
            accessoriesButton.setY(topPos + 20);
        }
        leftPos = layout.inventoryX(); topPos = layout.inventoryY();
        if (recipeButton != null) {
            recipeButton.active=getMenu().getCarried().isEmpty()&&!isQuickCrafting;
            boolean recipe=getRecipeBookComponent().isVisible();
            recipeButton.setX(recipe?contentX+contentW-22:leftPos+(layout.spaciousInventory()?166:123));
            recipeButton.setY(recipe?contentY+contentH-22:topPos+(layout.spaciousInventory()?98:70));
        }
    }
    private void positionInventorySlots() {
        // Slot indices and containers stay intact, so vanilla transfer/recipe logic still applies.
        for (int i = 0; i < getMenu().slots.size(); i++) {
            Slot slot = getMenu().slots.get(i);
            int x = slot.x, y = slot.y;
            if (layout.spaciousInventory()) {
                if (slot instanceof cn.blockforge.generated.generatedmod.robe.EquipmentSlots.EquipmentSlot) {
                    x = 124; y = slot.getContainerSlot() == cn.blockforge.generated.generatedmod.robe.YujianSlot.YUJIAN_SLOT ? 64 : 21;
                } else if (i == 0) { x = 226; y = 47; }
                else if (i <= 4) { x = 166 + (i - 1) % 2 * 22; y = 36 + (i - 1) / 2 * 22; }
                else if (i <= 8) { x = 12; y = 21 + Math.round((i - 5) * 86F / 3); }
                else if (i <= 35) { x = 29 + (i - 9) % 9 * 22; y = 160 + (i - 9) / 9 * 22; }
                else if (i <= 44) { x = 29 + (i - 36) * 22; y = 232; }
                else if (i == 45) { x = 124; y = 107; }
                else continue;
            } else if (slot instanceof cn.blockforge.generated.generatedmod.robe.EquipmentSlots.EquipmentSlot) {
                x = 90; y = slot.getContainerSlot() == cn.blockforge.generated.generatedmod.robe.YujianSlot.YUJIAN_SLOT ? 42 : 18;
            } else if (i == 0) { x = 184; y = 39; }
            else if (i <= 4) { x = 122 + (i - 1) % 2 * 20; y = 29 + (i - 1) / 2 * 20; }
            else if (i <= 8) { x = 8; y = 18 + (i - 5) * 18; }
            else if (i <= 35) { x = 23 + (i - 9) % 9 * 18; y = 108 + (i - 9) / 9 * 18; }
            else if (i <= 44) { x = 23 + (i - 36) * 18; y = 166; }
            else if (i == 45) { x = 90; y = 66; }
            else continue;
            originalSlotPositions.computeIfAbsent(slot, key -> new int[]{key.x, key.y});
            slot.x = x; slot.y = y;
        }
    }
    private void restoreInventorySlots() {
        originalSlotPositions.forEach((slot, position) -> { slot.x = position[0]; slot.y = position[1]; });
        originalSlotPositions.clear();
    }
    void scrollToTop() { scroll = 0; pageScroll.put(pageId, 0); }
    int contentViewportHeight() { return (int)(contentH / pageScale); }
    /** Reveal page-local controls using this host's actual viewport and scale. */
    void ensurePageRegionVisible(int top,int bottom) {
        if(page==null)return;
        int viewport=contentViewportHeight(),regionTop=Math.max(0,top),regionBottom=Math.max(regionTop,bottom);
        if(regionBottom-regionTop>viewport||regionTop<scroll)scroll=regionTop;
        else if(regionBottom>scroll+viewport)scroll=regionBottom-viewport;
        scroll=Math.max(0,Math.min(scroll,Math.max(0,page.height-viewport)));
        pageScroll.put(pageId,scroll);
    }
    private int pageAreaWidth() {
        // Inner vision's effect sidebar meets the outer background frame without a second inset.
        return pageId.equals("vision")?layout.x()+layout.width()-3-contentX:contentW;
    }
    private int pageWidth() { return (int)(pageAreaWidth() / pageScale) - (pageId.equals("vision")?0:8); }
    CultivationVisionLayout.Box visionEffectsBounds(CultivationVisionLayout.Box box) {
        if(pageWidth()<384)return box;
        int top=layout.contentFrameY()-contentY;
        int bottom=layout.contentFrameY()+layout.contentFrameHeight()-contentY;
        return new CultivationVisionLayout.Box(box.x(),top,box.w(),bottom-top);
    }
    void drawVisionEffectsBackground(GuiGraphics g,CultivationVisionLayout.Box box) {
        if(pageWidth()<384) {
            CultivationUi.background(g,"overview",box.x(),box.y(),box.w(),box.h());
            CultivationBorderAssets.frame(g,"gold",box.x(),box.y(),box.w(),box.h());
            return;
        }
        // The outer window owns the shared right/bottom edges and their chamfer.
        CultivationOuterFrame.clipBackground(g,panelX,panelY,panelW,panelH,()->
                CultivationUi.background(g,"overview",box.x(),box.y(),box.w(),box.h()));
        CultivationBorderAssets.drawVertical(g,box.x(),box.y(),box.h()-3);
    }
    private double pageX(double x) { return (x - contentX) / pageScale; }
    private double pageY(double y) { return (y - contentY) / pageScale + scroll; }
    @Override public void containerTick() {
        super.containerTick();
        if (page != null) {
            page.tick();
            scroll = Math.max(0, Math.min(scroll, page.height - contentViewportHeight()));
        }
    }
    private boolean inPage(double x, double y) {
        int top=pageId.equals("vision")?layout.contentFrameY():contentY;
        int bottom=pageId.equals("vision")?layout.contentFrameY()+layout.contentFrameHeight():contentY+contentH;
        return x >= contentX && x < contentX + pageAreaWidth() && y >= top && y < bottom;
    }
    private void frame(GuiGraphics g, int mx, int my, float delta) {
        int header = layout.headerHeight();
        CultivationOuterFrame.clipBackground(g,panelX,panelY,panelW,panelH,()->{
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF0D1A23);
        CultivationUi.background(g, "header", panelX + 3, panelY + 3, panelW - 6, header - 3);
        CultivationUi.background(g, "navigation", panelX + 3, panelY + header, layout.contentFrameX() - panelX - 3, panelH - header);
        int frameX=layout.contentFrameX(),frameY=layout.contentFrameY();
        int frameW=layout.contentFrameWidth(),frameH=layout.contentFrameHeight();
        if(inventory())frameH=panelY+panelH-frameY;
        CultivationUi.background(g, inventory() || pageId.equals("vision") ? "main" : "overview", frameX, frameY, frameW, frameH);
        if(pageId.equals("vision")&&pageWidth()>=384) {
            int paperRight=contentX+CultivationVisionLayout.of(pageWidth(),contentViewportHeight()).paperWidth();
            g.fill(frameX,frameY,paperRight,frameY+frameH,0x45FFF1D5);
            CultivationUi.background(g,"overview",paperRight,frameY,frameX+frameW-paperRight,frameH);
        } else if(!inventory()&&!pageId.equals("vision")) {
            CultivationUi.panel(g,frameX,frameY,frameW,frameH,false);
        }
        });
        CultivationUi.softDivider(g,panelX+4,panelY+header-1,panelW-9,false);
        int textY = panelY + (header - 8) / 2;
        g.drawString(font, Component.translatable("itemGroup.wuxingxiantu"), panelX + 10, textY, CultivationUi.GOLD, false);
        String name = inventory()&&getRecipeBookComponent().isVisible()
                ?Component.translatable("screen.wuxingxiantu.recipes").getString()
                :navigation.stream().filter(e -> e.id().equals(pageId)).map(CultivationPageRegistry.Entry::title).findFirst().orElse("背包");
        int titleCenter = panelX + panelW / 2;
        g.drawString(font, name, titleCenter - font.width(name) / 2, textY, CultivationUi.TEXT, false);
        var data = GeneratedMod.data(minecraft.player);
        if (data != null) {
            String realm = CultivationLogic.realmStageName(minecraft.player, data);
            int available = Math.min(panelW / 2 - 48, font.width(realm));
            CultivationUi.fit(g, Component.literal(realm), panelX + panelW - 34 - available, textY, available, CultivationUi.GOLD, mx, my);
        }
        for (int i = 0; i < navButtons.size(); i++) {
            var b = navButtons.get(i);
            b.active = getMenu().getCarried().isEmpty() && !isQuickCrafting;
            b.render(g, mx, my, delta);
            NavigationUiAssets.separator(g,b.getX()+4,b.getY()+b.getHeight(),b.getWidth()-8);
        }
        close.render(g, mx, my, delta);
        if (mx >= close.getX() && mx < close.getX() + close.getWidth() && my >= close.getY() && my < close.getY() + close.getHeight())
            CultivationUi.tooltip(Component.literal("关闭"));
    }
    private void drawNavigationDivider(GuiGraphics g) {
        // A shared foreground divider on every page, after backgrounds and
        // page scissor scopes have finished. Its center is the actual seam.
        g.pose().pushPose();g.pose().translate(0,0,100);
        CultivationBorderAssets.drawVertical(g,layout.contentFrameX(),layout.contentFrameY(),
                layout.contentFrameHeight());
        g.pose().popPose();
    }
    void clipPage(GuiGraphics g, int x, int y, int w, int h) {
        g.enableScissor((int)(contentX + x * pageScale), (int)(contentY + (y - scroll) * pageScale),
                (int)Math.ceil(contentX + (x + w) * pageScale), (int)Math.ceil(contentY + (y + h - scroll) * pageScale));
    }
    @Override protected void renderBg(GuiGraphics g, float delta, int mx, int my) {
        frame(g, mx, my, delta);
        if(getRecipeBookComponent().isVisible()) {
            hoveredSlot=null;
            recipeButton.render(g,mx,my,delta);
            drawNavigationDivider(g);
            CultivationUi.ornamentFrame(g,panelX,panelY,panelW,panelH);
            return;
        }
        // Frames share the native item centers; menu indices and transfer rules are unchanged.
        boolean spacious = layout.spaciousInventory();
        int craftingX = spacious ? 166 : 122;
        int inventoryLabelY = spacious ? 146 : 96;
        int dividerX = spacious ? 156 : 113;
        CultivationBorderAssets.draw(g,"rule_gold_vertical",leftPos+dividerX-2,topPos+18,4,spacious?108:72);
        g.drawString(font, "装备", leftPos + 8, topPos + 4, CultivationUi.INK, false);
        g.drawString(font, "合成", leftPos + craftingX, topPos + 12, CultivationUi.INK, false);
        g.drawString(font, "行囊", leftPos + (spacious ? 29 : 23), topPos + inventoryLabelY, CultivationUi.INK, false);
        g.drawString(font, "配方", leftPos + (spacious ? 192 : 148), topPos + (spacious ? 104 : 76), CultivationUi.INK, false);
        CultivationUi.rule(g, leftPos + (spacious ? 55 : 49), topPos + inventoryLabelY + 5, imageWidth - (spacious ? 79 : 73));
        int playerX = spacious ? 42 : 30, playerY = 18, playerW = spacious ? 66 : 54, playerH = spacious ? 108 : 72;
        CultivationUi.background(g, "player", leftPos + playerX+1, topPos + playerY+1, playerW-2, playerH-2);
        CultivationUi.portraitFrame(g, leftPos + playerX, topPos + playerY, playerW, playerH);
        for (int i = 0; i < getMenu().slots.size(); i++) {
            var slot = getMenu().slots.get(i);
            if (!slot.isActive()) continue;
            int size = slotSize(i), inset = (size - 16) / 2;
            CultivationUi.slot(g, leftPos + slot.x - inset, topPos + slot.y - inset, size, size, CultivationUi.BORDER);
        }
        if(!getRecipeBookComponent().isVisible()){
            for(var slot:getMenu().slots){
                if(!(slot instanceof cn.blockforge.generated.generatedmod.robe.EquipmentSlots.EquipmentSlot)||slot.hasItem())continue;
                boolean sword=slot.getContainerSlot()==cn.blockforge.generated.generatedmod.robe.YujianSlot.YUJIAN_SLOT;
                if(mx>=leftPos+slot.x&&mx<leftPos+slot.x+16&&my>=topPos+slot.y&&my<topPos+slot.y+16)
                    CultivationUi.tooltip(Component.literal(sword?"御剑槽":"道袍槽"));
            }
        }
        g.drawString(font, "→", leftPos + (spacious ? 212 : 166), topPos + (spacious ? 51 : 43), CultivationUi.INK, false);
        int centerX = leftPos + playerX + playerW / 2, bottomY = topPos + playerY + playerH - 3;
        g.enableScissor(leftPos + playerX + 2, topPos + playerY + 2, leftPos + playerX + playerW - 2, topPos + playerY + playerH - 2);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, centerX, bottomY, spacious ? 45 : 30,
                centerX - mx, topPos + playerY + playerH / 3 - my, minecraft.player);
        g.disableScissor();
        var data = GeneratedMod.data(minecraft.player);
        int x = leftPos + imageWidth + 8, w = panelX + panelW - x - 10;
        if (data != null && layout.hasOverview() && !getRecipeBookComponent().isVisible()) {
            int y = contentY + 5;
            // The shared outer frame owns the right and bottom edges; the
            // summary keeps its internal left separator only.
            int overviewLeft=x-7,overviewTop=panelY+layout.headerHeight();
            int overviewRight=panelX+panelW,overviewBottom=panelY+panelH;
            CultivationOuterFrame.clipBackground(g,panelX,panelY,panelW,panelH,()->
                    CultivationUi.background(g,"overview",overviewLeft,overviewTop,
                            overviewRight-overviewLeft,overviewBottom-overviewTop));
            CultivationBorderAssets.drawVertical(g,overviewLeft,overviewTop,
                    overviewBottom-overviewTop-3);
            CultivationUi.rule(g, x, y + 13, w);
            g.drawString(font, "修炼概览", x + 3, y, CultivationUi.GOLD, false);
            // Anchor roots near the bottom and share the recovered space across the seven row gaps.
            int summaryExtra = contentH >= 250 ? Math.max(0,contentH-246) : 0;
            summaryRow(g, 0, "境界", Component.literal(CultivationLogic.realmStageName(minecraft.player, data)), x, y + 18, w, CultivationUi.GOLD, mx, my);
            CultivationVisionAssets.draw(g, "cultivation_gold", x + 1, y + 42 + summaryGap(summaryExtra,1), 18, 18);
            CultivationUi.bar(g, "修为", x + 22, y + 40 + summaryGap(summaryExtra,1), w - 22, data.wuxingxiantu$getCultivation(), CultivationLogic.breakthroughRequirement(data), CultivationUi.GOLD, mx, my);
            CultivationVisionAssets.draw(g, "mana_gold", x + 1, y + 65 + summaryGap(summaryExtra,2), 18, 18);
            CultivationUi.bar(g, "灵力", x + 22, y + 63 + summaryGap(summaryExtra,2), w - 22, data.wuxingxiantu$getCurrentMana(), CultivationLogic.maxAura(data), CultivationUi.CYAN, mx, my);
            var physique = Component.translatable("physique.wuxingxiantu." + data.wuxingxiantu$getPhysique().id());
            CultivationVisionAssets.draw(g, "health_gold", x + 1, y + 88 + summaryGap(summaryExtra,3), 18, 18);
            CultivationUi.bar(g, "生命", x + 22, y + 86 + summaryGap(summaryExtra,3), w - 22, minecraft.player.getHealth(), minecraft.player.getMaxHealth(), CultivationUi.RED, mx, my);
            summaryRow(g, 3, "体质", physique, x, y + 108 + summaryGap(summaryExtra,4), w, CultivationUi.GREEN, mx, my);
            String heart = data.wuxingxiantu$getEquippedHeartMethod();
            Component method = heart == null || heart.isBlank() ? Component.literal("无")
                    : cn.blockforge.generated.generatedmod.HeartMethodCatalog.shortName(heart);
            summaryRow(g, 4, "当前心法", method, x, y + 132 + summaryGap(summaryExtra,5), w, CultivationUi.CYAN, mx, my);
            String[] domains = {"金光", CultivationLogic.isThunderForm(data) ? "万雷" : "木灵", "潮汐",
                    CultivationLogic.isExoticForm(data) ? "异火" : "业火", "重力"};
            boolean[] enabled = {data.wuxingxiantu$isGoldDomainEnabled(), data.wuxingxiantu$isWoodDomainEnabled(),
                    data.wuxingxiantu$isWaterDomainEnabled(), data.wuxingxiantu$isFireDomainEnabled(), data.wuxingxiantu$isEarthDomainEnabled()};
            List<String> active = new ArrayList<>();
            for (int i = 0; i < domains.length; i++) if (enabled[i]) active.add(domains[i]);
            summaryRow(g, 5, "当前领域",
                    Component.literal(active.isEmpty() ? "未激活" : String.join("、", active)), x, y + 156 + summaryGap(summaryExtra,6), w, CultivationUi.GOLD, mx, my);
            if (contentH >= 250) {
                int fire = data.wuxingxiantu$getEquippedFire();
                summaryRow(g, 6, "当前异火", Component.literal(fire > 0 ? cn.blockforge.generated.generatedmod.items.FireSeedItem.getFireName(fire) : "无"),
                        x, y + 180 + summaryGap(summaryExtra,7), w, CultivationUi.CYAN, mx, my);
                int rootsY=contentY+contentH-16;
                CultivationUi.rule(g, x, rootsY-20, w);
                g.drawString(font, "觉醒灵根", x + 3, rootsY-14, CultivationUi.MUTED, false);
                CultivationUi.fit(g, awakenedRoots(data), x + 3, rootsY, w - 6, CultivationUi.TEXT, mx, my);
            }
        }
        // Composite after the overview background so it cannot hide any corner.
        drawNavigationDivider(g);
        CultivationUi.ornamentFrame(g,panelX,panelY,panelW,panelH);
    }
    private static int summaryGap(int extra,int gap){return (int)Math.round(extra*gap/7.0);}
    private Component awakenedRoots(cn.blockforge.generated.generatedmod.CultivationData data) {
        var text = Component.empty();
        String[] glyphs = {"金", "木", "水", "火", "土", "雷", "异", "混"};
        int[] colors = {0xE8C88B,0x96BEA2,0x70BCE5,0xEF8B76,0xC8AA78,0xBFACF2,0x70DCE5,0xDCAAE0};
        for (int i=0;i<glyphs.length;i++) {
            boolean present = i<5 ? (data.wuxingxiantu$getRootMask()&(1<<i))!=0
                    : i==5 ? data.wuxingxiantu$hasThunderRoot() : i==6 ? data.wuxingxiantu$hasExoticRoot() : data.wuxingxiantu$hasChaosRoot();
            if (!present) continue;
            if (!text.getString().isEmpty()) text.append(" ");
            final int color=colors[i];
            text.append(Component.literal(glyphs[i]).withStyle(style->style.withColor(color)));
        }
        return text.getString().isEmpty()?Component.literal("无"):text;
    }
    private void summaryRow(GuiGraphics g, int icon, String label, Component value,
                            int x, int y, int w, int color, int mx, int my) {
        CultivationUi.summaryIcon(g, icon, x + 1, y + 3);
        g.drawString(font, label, x + 23, y, CultivationUi.MUTED, false);
        CultivationUi.fit(g, value, x + 23, y + 11, w - 23, color, mx, my);
        if(mx>=x&&mx<x+w&&my>=y&&my<y+24){
            var data=cn.blockforge.generated.generatedmod.GeneratedMod.data(minecraft.player);Component detail=null;
            if(data!=null){
                if(icon==3)detail=CultivationVisionPage.physiqueHint(data);
                else if(icon==4)detail=CultivationVisionPage.heartHint(data);
                else if(icon==5){var text=Component.literal("当前领域");boolean[] active={data.wuxingxiantu$isGoldDomainEnabled(),data.wuxingxiantu$isWoodDomainEnabled(),data.wuxingxiantu$isWaterDomainEnabled(),data.wuxingxiantu$isFireDomainEnabled(),data.wuxingxiantu$isEarthDomainEnabled()};boolean found=false;for(int i=0;i<5;i++)if(active[i]){found=true;text.append("\n").append(DomainTooltips.describeCurrent(minecraft.player,data,i));}detail=found?text:text.append("\n未激活领域\n在内视页选择领域，可查看各级效果");}
                else if(icon==6)detail=FireDetailTooltips.describe(minecraft.player,data);
            }
            if(detail!=null)CultivationUi.tooltip(detail);
        }
    }
    private String equipmentPattern(int index) {
        var slot = getMenu().slots.get(index);
        if (slot instanceof cn.blockforge.generated.generatedmod.robe.EquipmentSlots.EquipmentSlot)
            return slot.getContainerSlot() == cn.blockforge.generated.generatedmod.robe.YujianSlot.YUJIAN_SLOT ? "flying_sword" : "dao_robe";
        return switch (index) { case 5 -> "helmet"; case 6 -> "chestplate"; case 7 -> "leggings"; case 8 -> "boots"; case 45 -> "vanilla_shield"; default -> null; };
    }
    private int slotSize(int index) { return layout.spaciousInventory() ? equipmentPattern(index) != null ? 22 : 20 : 18; }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        if (getRecipeBookComponent().isVisible()) return;
        // Foreground replaces vanilla empty silhouettes only; native items and tooltips stay intact.
        g.pose().pushPose(); g.pose().translate(0, 0, 200);
        for (int i = 0; i < getMenu().slots.size(); i++) {
            var slot = getMenu().slots.get(i);
            String pattern = equipmentPattern(i);
            if (pattern == null || slot.hasItem() || !slot.isActive()) continue;
            // Native quick-craft previews must remain above the slot background.
            if (isQuickCrafting && quickCraftSlots.contains(slot) && !getMenu().getCarried().isEmpty()) continue;
            int size = slotSize(i), inset = (size - 16) / 2;
            CultivationUi.equipmentSlot(g, pattern, slot.x - inset, slot.y - inset, size);
            if (slot == hoveredSlot) {
                g.fill(slot.x - inset + 1, slot.y - inset + 1, slot.x + 16 + inset - 1, slot.y + 16 + inset - 1, 0x22FFFFFF);
                String label = switch (pattern) { case "helmet" -> "头盔槽"; case "chestplate" -> "胸甲槽"; case "leggings" -> "护腿槽";
                    case "boots" -> "靴子槽"; case "dao_robe" -> "道袍槽"; case "flying_sword" -> "御剑槽"; default -> "原版副手槽"; };
                CultivationUi.tooltip(Component.literal(label));
            }
        }
        g.pose().popPose();
    }
    @Override protected boolean isHovering(int x, int y, int w, int h, double mx, double my) {
        if (inventory() && !getRecipeBookComponent().isVisible() && w == 16 && h == 16 && layout != null && layout.spaciousInventory()) {
            for (int i = 0; i < getMenu().slots.size(); i++) {
                var slot = getMenu().slots.get(i);
                if (slot.x == x && slot.y == y) {
                    int size = slotSize(i), inset = (size - 16) / 2;
                    return super.isHovering(x - inset, y - inset, size, size, mx, my);
                }
            }
        }
        return super.isHovering(x, y, w, h, mx, my);
    }
    @Override public void renderBackground(GuiGraphics g) {
        if (minecraft.level != null) g.fill(0, 0, width, height, 0x55000000);
        else super.renderBackground(g);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        if (inventory()) { positionInventory(); super.render(g, mx, my, delta); return; }
        renderBackground(g); frame(g, mx, my, delta);
        int clipTop=pageId.equals("vision")?layout.contentFrameY():contentY;
        int clipBottom=pageId.equals("vision")?layout.contentFrameY()+layout.contentFrameHeight():contentY+contentH;
        g.enableScissor(contentX, clipTop, contentX + pageAreaWidth(), clipBottom);
        g.pose().pushPose(); g.pose().translate(contentX, contentY - scroll * pageScale, 0); g.pose().scale(pageScale, pageScale, 1);
        page.render(g, inPage(mx, my) ? (int)pageX(mx) : -1000, inPage(mx, my) ? (int)pageY(my) : -1000, delta);
        g.pose().popPose(); g.disableScissor();
        if (page.height > contentViewportHeight()) {
            int track = contentH-6, thumb = Math.min(track,Math.max(14, track * contentViewportHeight() / page.height));
            int y = contentY+3 + (track - thumb) * scroll / Math.max(1, page.height - contentViewportHeight());
            CultivationUi.scrollbar(g,contentX+contentW-4,contentY,contentH,y,thumb);
        }
        drawNavigationDivider(g);
        CultivationUi.ornamentFrame(g,panelX,panelY,panelW,panelH);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (close.mouseClicked(x, y, button)) return true;
        // 配方书展开时优先处理自身覆盖区域。
        boolean recipe = inventory() && getRecipeBookComponent().isVisible();
        if (!recipe || !getRecipeBookComponent().isMouseOver(x, y)) {
            for (var nav : navButtons) if (nav.mouseClicked(x, y, button)) return true;
        }
        if (inventory()) {
            if(recipe&&recipeButton.mouseClicked(x,y,button))return true;
            if(recipe) {
                getRecipeBookComponent().mouseClicked(x,y,button);
                positionInventory();
                return true;
            }
            boolean handled = super.mouseClicked(x, y, button);
            positionInventory();
            return handled;
        }
        if (button == 0 && page.height > contentViewportHeight() && x >= contentX + contentW - 6 && x < contentX + contentW && inPage(x, y)) {
            int track=contentH-6,thumb = Math.min(track,Math.max(14, track * contentViewportHeight() / page.height));
            int top = contentY+3 + (track - thumb) * scroll / Math.max(1, page.height - contentViewportHeight());
            scrollbarGrab = y >= top && y < top + thumb ? y - top : thumb / 2.0;
            draggingScrollbar = true; moveScrollbar(y); return true;
        }
        if (inPage(x, y)) { pointerInPage = true; return page.mouseClicked(pageX(x), pageY(y), button); }
        return false;
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (recipeButtonClicked) { recipeButtonClicked = false; return true; }
        if (inventory()) return super.mouseReleased(x, y, button);
        if (draggingScrollbar) { draggingScrollbar = false; return true; }
        boolean captured = pointerInPage; pointerInPage = false;
        return captured && page.mouseReleased(pageX(x), pageY(y), button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (draggingScrollbar) { moveScrollbar(y); return true; }
        return inventory() ? super.mouseDragged(x, y, button, dx, dy)
                : pointerInPage && page.mouseDragged(pageX(x), pageY(y), button, dx / pageScale, dy / pageScale);
    }
    private void moveScrollbar(double y) {
        int track=contentH-6,thumb = Math.min(track,Math.max(14, track * contentViewportHeight() / page.height));
        scroll = (int)Math.max(0, Math.min(page.height - contentViewportHeight(),
                (y - contentY - 3 - scrollbarGrab) * (page.height - contentViewportHeight()) / Math.max(1, track - thumb)));
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (inventory()) return super.mouseScrolled(x, y, delta);
        if (!inPage(x, y)) return false;
        if (page.mouseScrolled(pageX(x), pageY(y), delta)) return true;
        scroll = Math.max(0, Math.min(Math.max(0, page.height - contentViewportHeight()), scroll - (int) (delta * 24)));
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key == 258 && (mods & 2) != 0 && getMenu().getCarried().isEmpty() && !isQuickCrafting) {
            int index = 0;
            for (int i = 0; i < navigation.size(); i++) if (navigation.get(i).id().equals(pageId)) index = i;
            int next = Math.floorMod(index + ((mods & 1) != 0 ? -1 : 1), navigation.size());
            selectPage(navigation.get(next).id());
            return true;
        }
        if (inventory()) return super.keyPressed(key, scan, mods);
        if (key == 256 && page.closeTransientUi()) return true;
        if (key == 256 || minecraft.options.keyInventory.matches(key, scan)
                && !(page.getFocused() instanceof net.minecraft.client.gui.components.EditBox)) { onClose(); return true; }
        return page.keyPressed(key, scan, mods);
    }
    @Override public boolean charTyped(char ch, int mods) {
        return inventory() ? super.charTyped(ch, mods) : page.charTyped(ch, mods);
    }
    @Override public boolean keyReleased(int key, int scan, int mods) {
        return inventory() ? super.keyReleased(key, scan, mods) : page.keyReleased(key, scan, mods);
    }
    @Override public void mouseMoved(double x, double y) {
        if (inventory()) super.mouseMoved(x, y);
        else page.mouseMoved(pageX(x), pageY(y));
    }
    @Override public void resize(Minecraft client, int w, int h) {
        int previousScroll = scroll;
        super.resize(client, w, h);
        if (page != null) {
            page.resize(client, pageWidth(), page.contentHeight(pageWidth(), contentViewportHeight()));
            scroll = Math.min(previousScroll, Math.max(0, page.height - contentViewportHeight()));
        }
    }
    @Override protected boolean hasClickedOutside(double x, double y, int left, int top, int button) {
        if (x >= panelX && x < panelX + panelW && y >= panelY && y < panelY + panelH) return false;
        return super.hasClickedOutside(x, y, left, top, button);
    }
    @Override public void removed() {
        try { restoreInventorySlots(); if (page != null) page.removed(); super.removed(); }
        finally { InventoryResolutionFit.restore(minecraft); }
    }
}
