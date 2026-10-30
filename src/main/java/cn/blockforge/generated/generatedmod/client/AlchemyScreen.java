package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.alchemy.AlchemyGradeOdds;
import cn.blockforge.generated.generatedmod.alchemy.PillData;
import cn.blockforge.generated.generatedmod.alchemy.PillCatalog;
import cn.blockforge.generated.generatedmod.alchemy.FreeAlchemy;
import cn.blockforge.generated.generatedmod.fire.FireBottleData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Alchemy fittings and native slots; the scenic background is a separate final pass. */
public final class AlchemyScreen extends AbstractContainerScreen<AlchemyFurnaceMenu> {
    private static final int GOLD = 0xFFEED08A, TEXT = 0xFFE6E3D8, MUTED = 0xFFA5B1AE;
    private static final String[] TRIGRAMS = {"乾", "兑", "坎", "艮", "坤", "震", "巽", "离"};
    private static final String[] ESSENCES = {"金", "木", "水", "火", "土", "阴", "阳", "魂"};
    private EditBox search;
    private boolean observationOpen;
    private int observationScroll;
    private Button observationToggle;
    private final List<Button> heatControls = new ArrayList<>();
    private Button start, previous, next, formulaMode, experimentMode, inject, upgrade, fillMaterials, clearMaterials;
    private boolean holdingInjection;
    private int injectionTicks;
    private int injectionTooltipTier=-1;
    private final List<Button> rows = new ArrayList<>();
    private int page, pendingTicks;

    public AlchemyScreen(AlchemyFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = AlchemyUiLayout.WIDTH;
        imageHeight = AlchemyUiLayout.HEIGHT;
    }
    @Override protected void init() {
        String query=search==null?"":search.getValue();
        super.init();
        holdingInjection=false;injectionTicks=0;
        rows.clear();heatControls.clear();
        formulaMode = addRenderableWidget(new FlatButton(12,31,54,14,Component.literal("按丹方"),b -> switchMode(false)));
        experimentMode = addRenderableWidget(new FlatButton(70,31,54,14,Component.literal("自由炼制"),b -> switchMode(true)));
        search = addRenderableWidget(new EditBox(font, leftPos + 16, topPos + 51, 102, 10,
                Component.literal("搜索丹方")));
        search.setHint(Component.literal("搜索丹方…"));
        search.setBordered(false);
        search.setTextColor(TEXT);
        search.setMaxLength(60);
        search.setValue(query);
        search.setResponder(value -> { page = 0; updateControls(); });
        for (int i = 0; i < 6; i++) {
            final int row = i;
            FlatButton choice = new FlatButton(12, 64 + i * 22, 112, 21,
                    Component.empty(), button -> choose(row));
            choice.listRow = i;
            rows.add(addRenderableWidget(choice));
        }
        previous = addRenderableWidget(new FlatButton(12, 198, 32, 14, Component.literal("<"),
                button -> { page = Math.max(0, page - 1); updateControls(); }));
        next = addRenderableWidget(new FlatButton(92, 198, 32, 14, Component.literal(">"),
                button -> { page++; updateControls(); }));
        start = addRenderableWidget(new FlatButton(AlchemyUiLayout.BUTTON_X, AlchemyUiLayout.BUTTON_Y,
                AlchemyUiLayout.BUTTON_WIDTH, AlchemyUiLayout.BUTTON_HEIGHT, Component.literal("开始炼丹"), button -> brew()));
        for(int i=0;i<4;i++) {
            final int mode=new int[]{0,1,4,16}[i];
            var heat=addRenderableWidget(new FlatButton(298+i*28,296,26,16,
                    Component.literal(mode==0?"停火":mode+"倍"), b -> {
                        Network.CHANNEL.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyHeatPacket(menu.containerId,mode));
                    }));
            heat.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(mode==0
                    ?"直接停止加热，保留本炉并继续反应；余热不会立即消失。需要重新加热时选择火力。"
                    :"直接设为"+mode+"倍火力；消耗炉内灵力，强火可能导致过热、丹灰或炸炉。")));
            heatControls.add(heat);
        }
        upgrade=addRenderableWidget(new FlatButton(12,274,42,18,Component.literal("升阶"),b -> machineAction(254)));
        inject=addRenderableWidget(new FlatButton(366,253,40,18,Component.literal("长按注灵"),b -> {}));
        inject.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("按住持续注灵，松开停止。燃烧灵力耗尽，本炉炼制失败。")));
        upgrade.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("在上方放入更高阶丹炉核心，再点击升阶；炼制中不能升级。")));
        fillMaterials=addRenderableWidget(new FlatButton(144,196,62,16,Component.literal("一键放入"),b -> machineAction(menu.furnace().v5Batch()&&menu.isBrewing()?248:251)));
        clearMaterials=addRenderableWidget(new FlatButton(212,196,62,16,Component.literal("一键清空"),b -> machineAction(menu.furnace().v5Batch()&&menu.isBrewing()?245:252)));
        fillMaterials.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("从背包补足选中丹方的一炉材料；材料不足时不移动物品。")));
        clearMaterials.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("将八卦槽材料退回背包，保留异火与核心；背包空间不足时不移动物品。")));
        observationToggle=addRenderableWidget(new FlatButton(371,153,34,14,Component.literal(observationOpen?"药力":"记录"),b->{observationOpen=!observationOpen;observationScroll=0;observationToggle.setMessage(Component.literal(observationOpen?"药力":"记录"));}));
        observationToggle.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("切换原料观察 / 异火药力；滚轮查看可扩展参数与标签。自由炼制已接入V5事务。")));
        updateControls();
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){
        if(observationOpen&&isHovering(290,150,122,81,x,y)){
            int count=AlchemyObservationView.rows(menu).size();observationScroll=Math.max(0,Math.min(Math.max(0,count-5),observationScroll+(delta>0?-1:1)));return true;
        }
        return super.mouseScrolled(x,y,delta);
    }
    private List<AlchemyRecipe> filtered() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        java.util.Map<String,AlchemyRecipe> kinds = new java.util.LinkedHashMap<>();
        var candidates = menu.recipes().stream().sorted(java.util.Comparator
                .comparingInt((AlchemyRecipe r) -> r.furnaceTier() > menu.furnaceTier()
                        || GeneratedMod.data(minecraft.player)==null
                        || r.minimumRealm()>GeneratedMod.data(minecraft.player).wuxingxiantu$getRealm()
                        || menu.ignited() && !r.acceptsFire(menu.ignitionTier()) ? 1 : 0)
                .thenComparingInt(r -> r.tryIngredientReservation(menu.container()).isPresent() ? 0 : 1)
                .thenComparingInt(r -> -PillData.read(result(r)).grade().ordinal())
                .thenComparingInt(AlchemyRecipe::furnaceTier).thenComparingInt(AlchemyRecipe::cookTime)).toList();
        for (var recipe : candidates) if (kindName(recipe).toLowerCase(Locale.ROOT).contains(query))
            kinds.putIfAbsent(kindKey(recipe),recipe);
        // Keep the selected investment variant stable when adding/removing
        // ingredients; the row, tooltip and probability panel must agree.
        var selected=menu.selectedRecipe();
        if(!menu.freeMode()&&selected!=null&&kinds.containsKey(kindKey(selected)))kinds.put(kindKey(selected),selected);
        return kinds.values().stream().sorted(java.util.Comparator.comparing(this::kindName)).toList();
    }
    private String kindKey(AlchemyRecipe recipe) {
        var pill = PillData.read(result(recipe));
        return pill == null ? recipe.getId().toString() : pill.pill().id
                + (pill.pill() == PillCatalog.ROOT ? ":" + pill.element() : "")
                + (pill.pill() == PillCatalog.CULTIVATION ? ":" + pill.cultivationTier() : "");
    }
    private String kindName(AlchemyRecipe recipe) {
        var pill = PillData.read(result(recipe));
        if (pill == null) return result(recipe).getHoverName().getString();
        if (pill.pill() == PillCatalog.ROOT) return Component.translatable("pill.wuxingxiantu.root_name",
                Component.translatable("root.wuxingxiantu." + pill.element().ordinal())).getString();
        String name = Component.translatable("item.wuxingxiantu." + pill.pill().id).getString();
        return pill.pill() == PillCatalog.CULTIVATION ? name + " · " + cn.blockforge.generated.generatedmod.items.CultivationLogic.realmName(pill.cultivationTier()) : name;
    }
    private void switchMode(boolean free) {
        if (menu.isBrewing() || minecraft.gameMode == null) return;
        var kinds = filtered();
        int index = kinds.isEmpty() ? 0 : menu.recipes().indexOf(kinds.get(0));
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,free ? 255 : 2 + Math.max(0,index));
    }
    private ItemStack result(AlchemyRecipe recipe) {
        return minecraft == null || minecraft.level == null ? ItemStack.EMPTY
                : recipe.getResultItem(minecraft.level.registryAccess());
    }
    private String recipeAvailability(AlchemyRecipe recipe) {
        if(menu.isBrewing())return menu.selectedRecipe()!=null&&kindKey(menu.selectedRecipe()).equals(kindKey(recipe))?"炼制中":"炉占用";
        if(recipe.furnaceTier()>menu.furnaceTier())return "炉阶不足";
        var data=GeneratedMod.data(minecraft.player);
        if(data==null)return "待同步";
        if(!menu.formulaUnlocked())return "未学炼丹";
        if(recipe.minimumRealm()>data.wuxingxiantu$getRealm())return "境界不足";
        if(!menu.knowsFormula(recipe))return "未学丹方";
        if(recipe.tryIngredientReservation(menu.container()).isEmpty())return "材料不足";
        if(!menu.ignited()||!recipe.acceptsFire(menu.ignitionTier())||!ProgressionGate.canUseFire(minecraft.player,menu.ignitionTier()))return "异火不符";
        if(!ProgressionGate.canUseAlchemyFurnace(minecraft.player,menu.furnaceTier()))return "境界不足";
        if(menu.spiritFuel()<menu.v5RequiredFuel())return "需先注灵";
        return "可炼制";
    }
    private Component stateLabel() {
        if(menu.state()==AlchemyFurnaceMenu.State.NEED_FUEL)return Component.literal("预付需 "+menu.v5RequiredFuel()+" 炉内灵力");
        if(menu.state()==AlchemyFurnaceMenu.State.DAMAGED)return Component.literal("炉体损坏，需4铁锭修炉");
        if(menu.state()==AlchemyFurnaceMenu.State.COMPLETE)return Component.literal("批次已封存，可领取");
        if(menu.furnace().v5Batch()&&menu.state()==AlchemyFurnaceMenu.State.BREWING)return Component.literal(incidentLabel());
        if(menu.state()==AlchemyFurnaceMenu.State.NEED_FUEL)return Component.literal("先注灵（10秒储量）");
        return Component.translatable("gui.wuxingxiantu.alchemy.state_" + menu.state().name().toLowerCase(Locale.ROOT));
    }
    private List<Component> recipeDetails(AlchemyRecipe recipe) {
        var lines=new ArrayList<Component>();lines.add(Component.literal(kindName(recipe)+" · 配伍目标"));lines.add(Component.literal("新丹统一一至九品；药效和品级取决于真实反应，不按旧成色或药阶预定。"));
        lines.add(Component.literal("炼制条件："+recipeAvailability(recipe)).withStyle(net.minecraft.ChatFormatting.GOLD));
        lines.add(Component.literal("炉阶 ≥"+recipe.furnaceTier()+" · 异火 ≥"+recipe.minFireTier()+" · 时间 "+menu.v5Steps()+"秒"));
        lines.add(Component.literal("所需境界："+cn.blockforge.generated.generatedmod.items.CultivationLogic.realmName(
                Math.max(recipe.minimumRealm(),ProgressionGate.requiredRealmForFurnace(menu.furnaceTier())))));
        if(recipe.requiredFire()>0)lines.add(Component.literal("指定异火："+Component.translatable(ExoticFireColorRegistry.nameKey(recipe.requiredFire())).getString()));
        lines.add(Component.literal("炉内预付："+menu.v5RequiredFuel()+"；最终产物按实际反应结算"));
        int[] available=new int[8];
        for(int s=0;s<8;s++) {
            var stack=menu.container().getItem(s);
            available[s]=stack.getItem() instanceof cn.blockforge.generated.generatedmod.items.FireSeedItem?0:stack.getCount();
        }
        for(int i=0;i<recipe.getIngredients().size();i++) {
            var ingredient=recipe.getIngredients().get(i);int need=recipe.counts().get(i),remaining=need;
            for(int s=0;s<8&&remaining>0;s++)if(ingredient.test(menu.container().getItem(s))) {
                int used=Math.min(remaining,available[s]);available[s]-=used;remaining-=used;
            }
            var options=ingredient.getItems();
            String name=options.length==0?"材料":options[0].getHoverName().getString();
            lines.add(Component.literal(name+"：炉内 "+(need-remaining)+"/"+need+(remaining>0?"（缺 "+remaining+"）":""))
                    .withStyle(remaining>0?net.minecraft.ChatFormatting.RED:net.minecraft.ChatFormatting.GREEN));
        }
        lines.add(Component.literal("一键放入会从背包补齐；悬停中心预览可查看说明。").withStyle(net.minecraft.ChatFormatting.GRAY));
        return cn.blockforge.generated.generatedmod.alchemy.AlchemyTooltipOrder.attributionLast(lines);
    }
    private String incidentLabel(){
        int w=menu.v5Warning();if((w&2)!=0)return "炉压急升，宜泄压";if((w&1)!=0)return "焦味渐浓，请停止加热";if((w&8)!=0)return "结构失稳";if((w&16)!=0)return "温养储备不足";if((w&4)!=0)return "月华断供，宜温养";if((w&32)!=0)return "有偿温养中";return menu.v5HeatMode()==0?"已停加热，余热反应中":"当前火力 "+menu.v5HeatMode()+"倍";
    }
    private void updateControls() {
        if (start == null || previous == null) return;
        var choices = filtered();
        search.visible = !menu.freeMode();
        if (menu.freeMode()) search.setFocused(false);
        formulaMode.active = !menu.isBrewing() && menu.freeMode();
        experimentMode.active = !menu.isBrewing() && !menu.freeMode();
        page = Math.max(0, Math.min(page, Math.max(0, (choices.size() - 1) / 6)));
        for (int i = 0; i < rows.size(); i++) {
            int index = page * 6 + i;
            Button button = rows.get(i);
            button.visible = !menu.freeMode() && index < choices.size();
            button.active = !menu.isBrewing();
            if (button.visible) {
                var recipe = choices.get(index);
                boolean selected = menu.selectedRecipe() != null && kindKey(menu.selectedRecipe()).equals(kindKey(recipe));
                button.setMessage(Component.literal(kindName(recipe)));
            }
        }
        previous.visible = next.visible = !menu.freeMode();
        previous.active = page > 0;
        next.active = (page + 1) * 6 < choices.size();
        start.setTooltip(menu.freeMode() ? net.minecraft.client.gui.components.Tooltip.create(Component.literal("自由炼制：材料托管、预付炉内灵力；完成后按真实预算结算灵丹或副产物。升火会额外扣费，火力可直接选择停火/1/4/16倍。未使用的预付炉内灵力会返还。")) : null);
        boolean incident=menu.furnace().v5Batch()&&menu.isBrewing();
        start.visible=!incident;
        for(int i=0;i<heatControls.size();i++){var heat=heatControls.get(i);int mode=new int[]{0,1,4,16}[i];heat.visible=incident;heat.active=incident&&menu.v5HeatMode()!=mode;}
        start.active = incident||menu.state() == AlchemyFurnaceMenu.State.READY && pendingTicks == 0;
        fillMaterials.active=incident||!menu.isBrewing()&&!menu.freeMode()&&menu.formulaUnlocked()&&menu.knowsFormula(menu.selectedRecipe());
        fillMaterials.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                !menu.formulaUnlocked()?"先学习炼丹入门。":!menu.knowsFormula(menu.selectedRecipe())?"先选择并学习对应丹方。":
                "从背包补足选中丹方的一炉材料；材料不足时不移动物品。")));
        clearMaterials.active=incident||!menu.isBrewing()&&java.util.stream.IntStream.range(0,8).anyMatch(i -> !menu.container().getItem(i).isEmpty());
        fillMaterials.setMessage(Component.literal(incident?"中止封存":"一键放入"));clearMaterials.setMessage(Component.literal(incident?"有损泄压":"一键清空"));
        if(incident){fillMaterials.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("终止本炉并封存副产物，不能恢复炼制。仅需降温请使用右下角「停火」。")));clearMaterials.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("下一逻辑步释放部分挥发库存，永久损失质量和药力；不能撤销已提交事故。")));}
        if(inject!=null)inject.active=incident||!menu.furnace().v5Batch()&&menu.ignited() && menu.spiritFuel()<menu.spiritCapacity()
                &&minecraft.player!=null&&ProgressionGate.canUseFire(minecraft.player,menu.ignitionTier());
        if(inject!=null&&injectionTooltipTier!=menu.furnaceTier()){
            injectionTooltipTier=menu.furnaceTier();inject.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("长按持续注灵，松开停止\n容量："+menu.spiritCapacity()+" 灵力\n注灵速度："+cn.blockforge.generated.generatedmod.alchemy.AlchemyBalance.injectionPerSecond(injectionTooltipTier)+" 灵力 / 秒\n上限和注灵速度随炉品质递进")));
        }
        if(inject!=null)inject.setMessage(Component.literal(incident?(menu.v5Warming()?"恢复炼制":"封炉温养"):"长按注灵"));
        if(incident)inject.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("温养暂停工序推进，每秒额外消耗炉内灵力和已托管月华；不免疫热损伤或结构破坏，断供将中止。")));
        if(upgrade!=null)upgrade.active=!menu.isBrewing() && !menu.furnace().hasResult()
                && (!menu.container().getItem(AlchemyFurnaceBlockEntity.CORE_SLOT).isEmpty()||menu.v5DeviceWear()>0);
        if(upgrade!=null){upgrade.setMessage(Component.literal(menu.v5DeviceWear()>0?"修炉":"升阶"));upgrade.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(menu.v5DeviceWear()>0?"核心槽留空，背包消耗4铁锭修复；炉体损伤 "+menu.v5DeviceWear()/10+"%":"放入更高级核心升阶。")));}
        start.setMessage(Component.literal(pendingTicks > 0 ? "等待响应…" : incident ? "火力："+menu.v5HeatMode()+"倍" : menu.isBrewing() ? "炼制中…" : "开始炼丹"));
    }
    private void choose(int row) {
        var choices = filtered();
        int index = page * 6 + row;
        if (index >= choices.size() || menu.isBrewing() || minecraft == null || minecraft.gameMode == null) return;
        int actual = menu.recipes().indexOf(choices.get(index));
        if (actual >= 0 && actual < 249) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2 + actual);
    }
    private void brew() {
        if(menu.furnace().v5Batch()&&menu.isBrewing()){return;}
        AlchemyRecipe recipe = menu.selectedRecipe();
        if ((!menu.freeMode() && recipe == null) || menu.state() != AlchemyFurnaceMenu.State.READY || pendingTicks > 0) return;
        pendingTicks = 20;
        updateControls();
        Network.CHANNEL.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyPacket(menu.blockPos(), menu.freeMode() ? FreeAlchemy.ID : recipe.getId()));
    }
    @Override protected void containerTick() {
        super.containerTick();
        if (menu.state() != AlchemyFurnaceMenu.State.READY) pendingTicks = 0;
        else if (pendingTicks > 0) pendingTicks--;
        updateControls();
        if(holdingInjection&&(!minecraft.isWindowActive()
                ||org.lwjgl.glfw.GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(),0)!=org.lwjgl.glfw.GLFW.GLFW_PRESS))holdingInjection=false;
        if(holdingInjection && inject.active && ++injectionTicks>=4) { injectionTicks=0;machineAction(253); }
    }
    private void machineAction(int id) { if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id); }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0&&isHovering(AlchemyUiLayout.CENTER_X-14,AlchemyUiLayout.CENTER_Y-14,28,28,x,y)){
            if(menu.furnace().hasResult())machineAction(hasShiftDown()?249:250);return true;
        }
        if(button==0&&menu.getCarried().isEmpty()&&isHovering(144,42,128,12,x,y)&&!menu.freeMode()&&menu.selectedRecipe()!=null) {
            var selected=menu.selectedRecipe();
            search.setValue("");
            var choices=filtered();
            for(int i=0;i<choices.size();i++)if(kindKey(choices.get(i)).equals(kindKey(selected))) {page=i/6;break;}
            updateControls();return true;
        }
        if(button==0 && inject.active && inject.isMouseOver(x,y)) {
            if(menu.furnace().v5Batch()&&menu.isBrewing()){machineAction(247);return true;}
            holdingInjection=true;injectionTicks=0;machineAction(253);return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        if(button==0)holdingInjection=false;
        return super.mouseReleased(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(holdingInjection && !inject.isMouseOver(x,y))holdingInjection=false;
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public void removed() { holdingInjection=false;super.removed(); }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float tick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, tick);
        renderTooltip(g, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        if(isHovering(144,42,128,12,mouseX,mouseY)&&!menu.freeMode()&&menu.selectedRecipe()!=null) {
            g.renderComponentTooltip(font,List.of(Component.literal("已选丹方："+kindName(menu.selectedRecipe())),
                    Component.literal("点击定位到它所在的列表页；不会更改选中的丹方。")),mouseX,mouseY);return;
        }
        if(isHovering(290,25,122,68,mouseX,mouseY)) {
            g.renderComponentTooltip(font,List.of(Component.literal("药效与九品"),Component.literal("主药副药先按真实库存和预算编译药效，再评一至九品。"),Component.literal("失败只结算副产物；九品不自动授予职业称号。")),mouseX,mouseY);return;
        }
        if(observationOpen&&isHovering(298,174,106,50,mouseX,mouseY)){
            var observations=AlchemyObservationView.rows(menu);int index=observationScroll+(mouseY-topPos-174)/10;
            if(index>=0&&index<observations.size())g.renderComponentTooltip(font,observations.get(index).detail(),mouseX,mouseY);
            return;
        }
        if(!observationOpen&&isHovering(298,157,70,12,mouseX,mouseY)){
            g.renderComponentTooltip(font,List.of(Component.literal("异火现有药力").withStyle(net.minecraft.ChatFormatting.AQUA),
                    Component.literal("此处仅展示异火已有药力，不预告本批次结果。"),
                    Component.literal("当前批次的材料全部进入反应账本，不额外增加异火药力。")),mouseX,mouseY);return;
        }
        if(isHovering(298,281,108,12,mouseX,mouseY)&&menu.selectedRecipe()!=null) {
            g.renderComponentTooltip(font,recipeDetails(menu.selectedRecipe()),mouseX,mouseY);return;
        }
        if(isHovering(298,AlchemyUiLayout.FUEL_BAR_Y,62,AlchemyUiLayout.FUEL_BAR_HEIGHT,mouseX,mouseY)) {
            int rate=cn.blockforge.generated.generatedmod.alchemy.AlchemyBalance.burnPerSecond(menu.furnaceTier());
            g.renderComponentTooltip(font,List.of(Component.literal("燃烧灵力："+menu.spiritFuel()+" / "+menu.spiritCapacity()),
                    Component.literal("额外余额用于升火/温养，不等于基础续航秒数。"),
                    Component.literal("开炉预付 "+menu.v5RequiredFuel()+"；主药副药共同参与反应，不额外随机加成。")),mouseX,mouseY);
            return;
        }
        var choices = filtered();
        for(int row=0;row<rows.size();row++) {
            var button=rows.get(row);
            int index=page*6+row;
            if(button.visible && index<choices.size() && button.isMouseOver(mouseX,mouseY)) {
                g.renderComponentTooltip(font,recipeDetails(choices.get(index)),mouseX,mouseY);
                return;
            }
        }
        ItemStack display = menu.furnace().hasResult() ? menu.furnace().completedResult()
                : menu.selectedRecipe() == null ? ItemStack.EMPTY : result(menu.selectedRecipe());
        if (!display.isEmpty() && isHovering(AlchemyUiLayout.CENTER_X-14,AlchemyUiLayout.CENTER_Y-14,28,28,mouseX,mouseY)) {
            if (menu.furnace().hasResult()) {
                var lines=new ArrayList<Component>(display.getTooltipLines(minecraft.player,net.minecraft.world.item.TooltipFlag.NORMAL));
                lines.add(Component.literal("点击领取到背包；背包满时整批保留。").withStyle(net.minecraft.ChatFormatting.AQUA));
                lines.add(Component.literal("放不下的成品保留在炉内").withStyle(net.minecraft.ChatFormatting.GRAY));
                g.renderComponentTooltip(font,lines,mouseX,mouseY);
            }
            else {
                var recipe = menu.selectedRecipe();
                List<Component> lines = new ArrayList<>();
                lines.add(display.getHoverName());
                var data = minecraft.player == null ? null : GeneratedMod.data(minecraft.player);
                lines.add(Component.literal("炉内预付："+menu.v5RequiredFuel()+" · 基础时间："+menu.v5Steps()+"秒"));
                lines.add(Component.literal("丹方图标仅为方例；最终药效与品级由实际材料和火候决定。"));
                lines.add(Component.literal("炉品阶：" + recipe.furnaceTier() + " · 异火阶数：" + recipe.minFireTier()));
                for (int i = 0; i < recipe.getIngredients().size(); i++) {
                    var alternatives = recipe.getIngredients().get(i).getItems();
                    if (alternatives.length > 0) lines.add(Component.literal(alternatives[0].getHoverName().getString() + " ×" + recipe.counts().get(i)));
                }
                lines.add(Component.literal("灰色预览：尚未成丹，不能取出").withStyle(net.minecraft.ChatFormatting.GRAY));
                lines.add(Component.literal("预览用于展示丹种，实际品阶按右侧概率生成"));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
    }
    @Override protected void renderBg(GuiGraphics g, float tick, int mouseX, int mouseY) {
        // Scissor coordinates are absolute screen coordinates in 1.20.1.
        // Draw the opaque backing before translating the controls to local space.
        g.setColor(1,1,1,1);
        CultivationOuterFrame.clipBackground(g,leftPos,topPos,imageWidth,imageHeight,() -> {
            g.blit(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wuxingxiantu","textures/gui/alchemy_concept/landscape.png"),
                    leftPos,topPos,imageWidth,imageHeight,0,0,1448,1086,1448,1086);
            g.fill(leftPos,topPos+24,leftPos+imageWidth,topPos+imageHeight,0x44051820);
        });
        g.pose().pushPose();
        g.pose().translate(leftPos, topPos, 0);
        try {
            AlchemyChrome.headerBacking(g);
            panel(g, 8, 25, 120, 190);
            panel(g, 132, 25, 152, 190);
            AlchemyChrome.progressBacking(g,8,218,276,12);
            panel(g, 8, 232, 50, 84);
            panel(g, 62, 232, 222, 84);
            panel(g, 290, 25, 122, 68);
            panel(g, 290, 97, 122, 49);
            panel(g, 290, 150, 122, 81);
            panel(g, 290, 235, 122, 40);
            panel(g, 290, 279, 122, 37);
            if(!menu.freeMode())AlchemyChrome.surface(g,12,48,112,16,0xFF887249,0xFF091A24,0xFF091A24);
            AlchemyChrome.rule(g,144,40,128);
            AlchemyChrome.rule(g,298,42,106);
            AlchemyChrome.rule(g,298,114,106);
            AlchemyChrome.rule(g,298,167,106);
            AlchemyChrome.formation(g,AlchemyUiLayout.CENTER_X,AlchemyUiLayout.CENTER_Y);
            AlchemyChrome.clouds(g);
            CultivationUi.softDivider(g,351,48,25,true,0x548E805E);
            CultivationUi.softDivider(g,351,171,38,true,0x448E805E);
            for (int index=0;index<menu.slots.size();index++) {
                var slot=menu.slots.get(index);
                AlchemyChrome.slot(g,slot.x-1,slot.y-1,index<AlchemyFurnaceBlockEntity.CONTAINER_SIZE);
            }
            // Mark the quickbar boundary using the existing two-pixel gap.
            g.fill(74,AlchemyUiLayout.INVENTORY_ROWS[3]-2,272,AlchemyUiLayout.INVENTORY_ROWS[3]-1,0xFF4E4F3A);
            AlchemyChrome.preview(g,AlchemyUiLayout.CENTER_X-14,AlchemyUiLayout.CENTER_Y-14);
            for (int i = 0; i < 8; i++) {
                var slot = menu.slots.get(i);
                // The trigram belongs to the input slot. Native item rendering
                // later covers this subdued watermark without covering counts.
                String symbol=TRIGRAMS[i];
                // Font advance includes one trailing spacing pixel. Center the
                // visible glyph, rounding the half-pixel instead of truncating.
                text(g,symbol,slot.x+Math.round((17-font.width(symbol))/2F),slot.y+(16-font.lineHeight)/2,
                        slot.hasItem()?0xFF354A4D:0xFF9C8858);
            }
            ItemStack display = menu.furnace().hasResult() ? menu.furnace().completedResult()
                    : menu.selectedRecipe() == null ? ItemStack.EMPTY : result(menu.selectedRecipe());
            if (!display.isEmpty()){
                if(menu.furnace().hasResult())g.renderItem(display,AlchemyUiLayout.CENTER_X-8,AlchemyUiLayout.CENTER_Y-8);
                else AlchemyPreviewRenderer.grey(g,display,AlchemyUiLayout.CENTER_X-8,AlchemyUiLayout.CENTER_Y-8);
            }
            else text(g,"丹",AlchemyUiLayout.CENTER_X-font.width("丹")/2,AlchemyUiLayout.CENTER_Y-4,0xFF657D77);
            AlchemyChrome.bar(g,AlchemyUiLayout.BAR_X,AlchemyUiLayout.BAR_Y,AlchemyUiLayout.BAR_WIDTH,AlchemyUiLayout.BAR_HEIGHT,menu.brewingProgress(),false,false);
            AlchemyChrome.bar(g,298,AlchemyUiLayout.FUEL_BAR_Y,62,AlchemyUiLayout.FUEL_BAR_HEIGHT,
                    menu.spiritFuel()/(float)menu.spiritCapacity(),true,
                    !menu.furnace().v5Batch()&&menu.isBrewing()&&menu.spiritFuel()<15*cn.blockforge.generated.generatedmod.alchemy.AlchemyBalance.burnPerSecond(menu.furnaceTier()));
            CultivationOuterFrame.drawAlchemy(g,0,0,imageWidth,imageHeight);
            AlchemyChrome.headerCorners(g);
        } finally { g.pose().popPose(); }
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Component heading=AlchemyFurnaceBlock.tierName(menu.furnaceTier()).copy().withStyle(style -> style.withFont(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wuxingxiantu","alchemy")));
        g.pose().pushPose();g.pose().translate(210,6,0);g.pose().scale(1.45F,1.45F,1);
        g.drawString(font,heading,-font.width(heading)/2,0,GOLD,false);g.pose().popPose();
        AlchemyChrome.headerFlourishes(g,Math.round(font.width(heading)*1.45F/2));
        centered(g, Component.literal("八卦炼丹"),AlchemyUiLayout.CENTER_X,29,136,GOLD);
        var selected=menu.selectedRecipe();
        centered(g,Component.literal(selected==null?menu.freeMode()?"V5自由配伍：有限药效与九品":"选择丹方后预览产物":"已选："+kindName(selected)),AlchemyUiLayout.CENTER_X,44,126,MUTED);
        if (!menu.freeMode()) centered(g, Component.literal((page + 1) + " / " + Math.max(1, (filtered().size() + 5) / 6)), 68, 201, 40, MUTED);
        else {
            String[] hints={"药材与月华可组合", "投入材料即托管", "炉内灵力整批预付", "先编译药效再评级", "失败产物不可服用", "中止封存有限药渣"};
            for(int i=0;i<hints.length;i++) small(g,hints[i],14,70+i*20,108,i==4?0xFFDB8878:MUTED);
        }
        small(g,menu.isBrewing()?"炼制":"待炼制",16,219,34,GOLD);
        if(menu.isBrewing()) {
            String progress=Math.round(menu.brewingProgress()*100)+"%";
            g.drawString(font,progress,166-font.width(progress)/2,219,TEXT,true);
        }
        centered(g,Component.literal("炉核心"),33,239,42,GOLD);
        small(g,"高阶核心",12,302,42,MUTED);
        centered(g,Component.literal(menu.furnace().hasResult()?menu.furnace().completedResult().is(WuXingItems.PILL_ASH.get())?"领取丹灰":menu.furnace().completedResult().is(WuXingItems.V5_PILL.get())?"领取灵丹×"+menu.furnace().completedResult().getCount():menu.furnace().completedResult().is(WuXingItems.ALCHEMY_RESIDUE.get())?"领取药渣":menu.furnace().completedResult().is(WuXingItems.WASTE_PILL.get())?"领取废丹":menu.furnace().completedResult().is(WuXingItems.SEALED_ALCHEMY_BATCH.get())?"领取药液":"点击取丹":selected==null?menu.freeMode()?"投入灵药":"选择丹方":"预览"),AlchemyUiLayout.CENTER_X,137,72,0xFF8FCFD2);
        text(g,"注灵",298,241,GOLD);
        int seconds=menu.spiritFuel()/cn.blockforge.generated.generatedmod.alchemy.AlchemyBalance.burnPerSecond(menu.furnaceTier());
        small(g,"余 "+menu.spiritFuel()+" 灵",334,241,70,MUTED);
        String spirit=Math.round(100F*menu.spiritFuel()/menu.spiritCapacity())+"%";
        g.drawString(font,spirit,329-font.width(spirit)/2,
                AlchemyUiLayout.FUEL_BAR_Y+Math.round((AlchemyUiLayout.FUEL_BAR_HEIGHT-font.lineHeight)/2F),
                menu.spiritFuel()*5>=menu.spiritCapacity()*4?0xFF092432:TEXT,false);
        text(g, "当前异火", 298, 104, GOLD);
        Component fire = menu.ignited() ? Component.translatable(ExoticFireColorRegistry.nameKey(menu.ignitionTier())) : Component.literal("放入异火瓶");
        centered(g, fire, 363, 125, 78, TEXT);
        text(g,"批次结算",298,32,GOLD);
        var recipe=menu.selectedRecipe();
        var v5Result=cn.blockforge.generated.generatedmod.alchemy.effects.PillSnapshot.view(menu.furnace().completedResult());
        String[] labels={"反应","评级","药效","供灵"};String[] values={"连续",v5Result==null?"待结算":v5Result.grade()+"品",v5Result==null?"待编译":"已编译","预付"};
        for(int i=0;i<4;i++){int x=298+(i%2)*55,y=48+(i/2)*17;text(g,labels[i],x,y,TEXT);right(g,values[i],x+52,y,28,GOLD);}
        text(g,"警兆",298,80,TEXT);right(g,menu.isBrewing()?incidentLabel():"账本统一结算",404,80,90,0xFFE3A287);
        text(g, observationOpen?"原料观察":"异火吸收药力", 298, 157, GOLD);
        if(observationOpen){
            var observations=AlchemyObservationView.rows(menu);observationScroll=Math.max(0,Math.min(observationScroll,Math.max(0,observations.size()-5)));
            for(int i=0;i<5&&observationScroll+i<observations.size();i++){
                var row=observations.get(observationScroll+i);int y=174+i*10;
                small(g,row.label(),298,y,64,TEXT);right(g,row.value(),404,y,40,GOLD);
            }
        }else drawEssence(g, recipe);
        centered(g,stateLabel(),353,283,108,start.active?0xFF80D6B0:MUTED);
    }
    private void drawEssence(GuiGraphics g, AlchemyRecipe recipe) {
        ItemStack bottle = menu.container().getItem(AlchemyFurnaceMenu.IGNITION_SLOT);
        for(int i=0;i<8;i++){double value=FireBottleData.essence(bottle,FireBottleData.Essence.values()[i]);small(g,ESSENCES[i]+" "+number(value),298+i%2*55,172+i/2*11,53,TEXT);}
        small(g,"本批次不增加异火药力",298,217,106,MUTED);
    }

    private String number(double value) {
        return value >= 10000 ? String.format(Locale.ROOT, "%.1f万", value / 10000)
                : value == Math.floor(value) ? Long.toString((long)value) : String.format(Locale.ROOT, "%.1f", value);
    }
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        AlchemyChrome.panel(g,x,y,w,h);
    }
    private void text(GuiGraphics g, String value, int x, int y, int color) { g.drawString(font, value, x, y, color, false); }
    private void small(GuiGraphics g, String value, int x, int y, int max, int color) {
        float scale = Math.min(1F, max / (float)Math.max(1, font.width(value)));
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(scale, scale, 1);
        text(g, value, 0, 0, color); g.pose().popPose();
    }
    private void secondary(GuiGraphics g,String value,int x,int y,int max,int color) {
        float scale=Math.min(.85F,max/(float)Math.max(1,font.width(value)));
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
        text(g,value,0,0,color);g.pose().popPose();
    }
    private void right(GuiGraphics g,String value,int edge,int y,int max,int color) {
        float scale=Math.min(1F,max/(float)Math.max(1,font.width(value)));
        g.pose().pushPose();g.pose().translate(edge,y,0);g.pose().scale(scale,scale,1);
        text(g,value,-font.width(value),0,color);g.pose().popPose();
    }
    private void centered(GuiGraphics g, Component value, int x, int y, int max, int color) {
        String line = value.getString();
        float scale = Math.min(1F, max / (float)Math.max(1, font.width(line)));
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(scale, scale, 1);
        text(g, line, -font.width(line) / 2, 0, color); g.pose().popPose();
    }
    private final class FlatButton extends Button {
        private int listRow = -1;
        FlatButton(int x, int y, int w, int h, Component text, OnPress press) {
            super(Button.builder(text, press).bounds(leftPos + x, topPos + y, w, h));
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float tick) {
            boolean selected=this==formulaMode&&!menu.freeMode()||this==experimentMode&&menu.freeMode();
            if(listRow>=0) {
                var choices=filtered();int index=page*6+listRow;
                selected=index<choices.size()&&menu.selectedRecipe()!=null
                        &&kindKey(menu.selectedRecipe()).equals(kindKey(choices.get(index)));
            }
            boolean pressed=this==inject&&holdingInjection&&active;
            boolean tab=this==formulaMode||this==experimentMode;
            AlchemyChrome.button(g,getX(),getY(),width,height,active,selected,isHoveredOrFocused(),this==start,pressed,tab);
            if (listRow >= 0) {
                var choices = filtered();
                int index = page * 6 + listRow;
                if (index >= choices.size()) return;
                var recipe = choices.get(index);
                String availability=recipeAvailability(recipe);
                int color=availability.equals("可炼制")?0xFF8DD9AF:availability.equals("材料不足")||availability.equals("需先注灵")?MUTED:0xFFD59B7D;
                g.fill(getX()+1,getY()+4,getX()+(selected?3:2),getY()+height-4,selected?GOLD:color);
                g.renderItem(result(recipe), getX() + 3, getY() + 2);
                small(g, getMessage().getString(), getX() + 23, getY() + 2, width - 27, selected||active ? GOLD : MUTED);
                String seconds=menu.v5Steps()+"秒";
                int timeWidth=Math.round(font.width(seconds)*.85F);
                secondary(g,availability,getX()+23,getY()+12,width-31-timeWidth,color);
                g.pose().pushPose();g.pose().translate(getX()+width-4,getY()+12,0);g.pose().scale(.85F,.85F,1);
                text(g,seconds,-font.width(seconds),0,MUTED);g.pose().popPose();
            } else centered(g, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, width - 8,
                    tab&&selected||this==start&&active?0xFF302313:selected||active ? GOLD : MUTED);
        }
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (search != null && search.isFocused() && key != 256) return search.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
