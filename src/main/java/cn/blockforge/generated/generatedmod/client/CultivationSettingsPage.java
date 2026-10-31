package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig;
import cn.blockforge.generated.generatedmod.HudConfig;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.realmflight.RealmFlightSelection;
import cn.blockforge.generated.generatedmod.client.realmflight.RealmFlightVisuals;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

/** Client-only display and control preferences inside the existing inventory settings. */
public final class CultivationSettingsPage extends CultivationPage {
    private int category, bodyX, bodyW;
    private boolean huahongSkinsExpanded;
    private CultivationUi.Action huahongSkinSelector;
    private static final int HUAHONG_SELECTOR_Y=664,HUAHONG_CHOICES_Y=696,HUAHONG_CHOICE_STEP=28;
    private static final int LAYOUT=0,SWORD=1,FLIGHT=2,COMBAT=3,MANA=4;
    private static final String[] CATEGORIES = {"界面布局", "御剑飞行", "遁光", "战斗特效", "灵力损耗"};
    public CultivationSettingsPage() { super("设置"); }
    @Override public int contentHeight(int w, int h) {
        return Math.max(h, switch(category){
            case SWORD -> huahongContentHeight(w);
            case FLIGHT -> 332 + 28 * RealmFlightSelection.unlockedRealms(actualRealm()).size();
            case MANA -> tracking()?600:64;
            case COMBAT -> 274;
            default -> 328;
        });
    }
    private static int huahongColumns(int pageWidth) {
        int tabsWidth=pageWidth>=280?65:49;
        return pageWidth-tabsWidth-18>=192?2:1;
    }
    private int huahongContentHeight(int pageWidth) {
        if(!huahongSkinsExpanded)return HUAHONG_CHOICES_Y+20;
        int columns=huahongColumns(pageWidth),count=cn.blockforge.generated.generatedmod.yujian.HuahongSkin.values().length;
        return HUAHONG_CHOICES_Y+((count+columns-1)/columns)*HUAHONG_CHOICE_STEP+20;
    }
    private void rebuildHuahongChoices(boolean expanded) {
        huahongSkinsExpanded=expanded;
        host.selectPage("settings");
        if(expanded)host.ensurePageRegionVisible(HUAHONG_SELECTOR_Y-28,huahongContentHeight(width));
        setFocused(huahongSkinSelector);
    }
    @Override public boolean closeTransientUi() {
        if(category!=SWORD||!huahongSkinsExpanded)return false;
        rebuildHuahongChoices(false);
        return true;
    }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button) {
        boolean handled=super.mouseClicked(mouseX,mouseY,button);
        // A selector/choice press rebuilds this page during the callback. Vanilla
        // focuses the clicked widget after that callback, so reconcile it with
        // the new children before keyboard input can reach the removed widget.
        if(getFocused()!=null&&!children().contains(getFocused())) {
            setFocused(category==SWORD?huahongSkinSelector:null);
            setDragging(false);
        }
        return handled;
    }
    private void huahongSkinChoices() {
        var selected=FlyingSwordConfig.HUAHONG_SKIN.get();
        // Reuse the domain selector's complete 20-high reference-atlas control.
        huahongSkinSelector=new CultivationUi.Action(bodyX,HUAHONG_SELECTOR_Y,bodyW,20,
                Component.literal(selected.label+"  ▾"),b->rebuildHuahongChoices(!huahongSkinsExpanded));
        huahongSkinSelector.chrome=1;
        huahongSkinSelector.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(selected.description+"\n点击"+(huahongSkinsExpanded?"收起":"展开选择")+"；仅改变外观，速度和消耗不变。")));
        addRenderableWidget(huahongSkinSelector);
        if(!huahongSkinsExpanded)return;
        int columns=huahongColumns(width),gap=6,choiceW=(bodyW-gap*(columns-1))/columns,index=0;
        for(var skin:cn.blockforge.generated.generatedmod.yujian.HuahongSkin.values()) {
            var choice=new CultivationUi.Action(bodyX+(index%columns)*(choiceW+gap),HUAHONG_CHOICES_Y+(index/columns)*HUAHONG_CHOICE_STEP,choiceW,22,
                    Component.literal(skin.label),b->{HuahongSkinPreferences.select(skin);rebuildHuahongChoices(false);},()->FlyingSwordConfig.HUAHONG_SKIN.get()==skin);
            choice.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(skin.description+"\n仅改变化虹外观，速度和消耗不变；其他玩家可见。")));
            addRenderableWidget(choice);index++;
        }
    }
    private int actualRealm() {
        var player = Minecraft.getInstance().player;
        var data = player == null ? null : GeneratedMod.data(player);
        return data == null ? 0 : data.wuxingxiantu$getRealm();
    }
    private boolean selectedVisual(int realm) {
        return RealmFlightSelection.sanitizePreference(actualRealm(), HudConfig.REALM_FLIGHT_VISUAL_REALM.get()) == realm;
    }
    private boolean tracking(){return !(GeneratedMod.data(minecraft.player) instanceof cn.blockforge.generated.generatedmod.CultivationDataImpl d)||d.manaLossEnabled();}
    private boolean fireHurtsNonHostile(){return GeneratedMod.data(minecraft.player) instanceof cn.blockforge.generated.generatedmod.CultivationDataImpl d&&d.exoticFireHurtsNonHostile();}
    private void visualChoice(int y, int realm) {
        addRenderableWidget(new CultivationUi.Action(bodyX, y, bodyW, 22,
                Component.literal(RealmFlightSelection.label(realm)), b -> {
                    HudConfig.REALM_FLIGHT_VISUAL_REALM.set(RealmFlightSelection.sanitizePreference(actualRealm(), realm));
                    HudConfig.REALM_FLIGHT_VISUAL_REALM.save();
                    RealmFlightVisuals.sendMode();
                    host.selectPage("settings");
                }, () -> selectedVisual(realm)));
    }
    private void action(int y, Supplier<String> label, Runnable action) {
        addRenderableWidget(new CultivationUi.Action(bodyX, y, bodyW, 22, Component.literal(label.get()), b -> {
            action.run(); b.setMessage(Component.literal(label.get()));
        }));
    }
    private void toggle(int y, String name, ForgeConfigSpec.BooleanValue value) {
        var toggle=new CultivationUi.Toggle(bodyX, y, bodyW, Component.literal(name), value);
        if(value==HudConfig.EXOTIC_FIRE_FIRST_PERSON_VISUAL)toggle.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("默认关闭：第一人称不渲染异火缠绕，包括防具、手持武器和粒子回退。\n第三人称仍显示；手动开启后才使用下方的第一人称优化。")));
        if(value==HudConfig.EXOTIC_FIRE_FIRST_PERSON_OPTIMIZATION)toggle.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("开启：第一人称优先帧率，细丝更柔，极细火舌可能损失。最多192步、9万积分像素。\n关闭：恢复原生精度，负载更高。第三人称不受影响。")));
        if(value==HudConfig.EXOTIC_FIRE_THIRD_PERSON_OPTIMIZATION)toggle.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("开启：降低第三人称贴身近景的积分分辨率，每次绘制最多9万积分像素，保留512步。\n细丝和焰尖会更柔；远处小范围特效保持原生分辨率。关闭可恢复原生精度。")));
        addRenderableWidget(toggle);
    }
    private void cameraChoice(int y,String name,ForgeConfigSpec.IntValue value){
        action(y,()->name+"："+FlightCameraPreferences.label(value.get()),()->{value.set((value.get()+1)%3);value.save();});
    }
    private void number(int y, String name, ForgeConfigSpec.DoubleValue value, double min, double max, double step) {
        addRenderableWidget(new CultivationUi.Slider(bodyX, y, bodyW, name, value, min, max, step));
    }
    private void bossPosition(int y,String name,ForgeConfigSpec.DoubleValue value) {
        addRenderableWidget(new CultivationUi.Slider(bodyX,y,bodyW,name,value,0,100,1) {
            @Override protected void applyValue(){super.applyValue();HudConfig.BOSS_AUTO.set(false);}
            @Override public void onRelease(double x,double yy){super.onRelease(x,yy);HudConfig.BOSS_AUTO.save();}
            @Override public boolean keyPressed(int key,int scan,int mods){boolean handled=super.keyPressed(key,scan,mods);if(handled)HudConfig.BOSS_AUTO.save();return handled;}
        });
    }
    @Override protected void init() {
        int tabsW = width >= 280 ? 65 : 49;
        bodyX = tabsW + 12; bodyW = width - bodyX - 6;
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int selected = i;
            addRenderableWidget(new CultivationUi.Tab(0, 5 + i * 28, tabsW, 23, Component.literal(CATEGORIES[i]), b -> {
                category = selected; huahongSkinsExpanded=false; host.selectPage("settings"); host.scrollToTop();
            }, () -> category == selected));
        }
        if (category == COMBAT) {
            toggle(28,"灵力御体特效",HudConfig.SPIRIT_GUARD_VISUAL);
            toggle(78,"异火缠绕特效",HudConfig.EXOTIC_FIRE_WRAP_VISUAL);
            toggle(106,"第一人称显示异火缠绕",HudConfig.EXOTIC_FIRE_FIRST_PERSON_VISUAL);
            toggle(134,"异火缠绕第一人称优化",HudConfig.EXOTIC_FIRE_FIRST_PERSON_OPTIMIZATION);
            toggle(162,"异火缠绕第三人称近景优化",HudConfig.EXOTIC_FIRE_THIRD_PERSON_OPTIMIZATION);
            var targeting=new CultivationUi.Action(bodyX,216,bodyW,22,Component.literal(fireTargetingLabel()),b->{
                boolean enabled=!fireHurtsNonHostile();
                cn.blockforge.generated.generatedmod.Network.CHANNEL.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.cultivation.ExoticFireTargetingPacket(enabled));
                if(GeneratedMod.data(minecraft.player) instanceof cn.blockforge.generated.generatedmod.CultivationDataImpl d)d.setExoticFireHurtsNonHostile(enabled);
                b.setMessage(Component.literal(fireTargetingLabel()));
            });
            targeting.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("默认关闭：异火缠绕不会灼伤普通动物或未攻击你的中立生物。\n怪物和正在攻击你的生物仍受影响。只控制缠绕的额外伤害、点燃和附加效果，普通攻击照常。\n按角色保存，与特效显示开关独立；切换前已施加的灼烧会自然结束。")));
            addRenderableWidget(targeting);
        } else if (category == SWORD) {
            toggle(46, "御剑飞行特效", FlyingSwordConfig.TRAILS);
            huahongSkinChoices();
            toggle(74, "第一人称隐藏手持飞剑", FlyingSwordConfig.HIDE_HELD);
            cameraChoice(102,"御剑默认视角",HudConfig.SWORD_FLIGHT_DEFAULT_CAMERA);
            toggle(564, "御剑调试信息", FlyingSwordConfig.DEBUG_OVERLAY);
            action(592, () -> "剑迹渲染方式：" + switch (FlyingSwordConfig.TRAIL_PATH.get()) {
                case AUTO -> "自动"; case NORMAL_TRAIL_RENDER_PATH -> "常规";
                case SHADER_COMPAT_TRAIL_RENDER_PATH -> "光影兼容"; case POST_SHADER_TRAIL_RENDER_PATH -> "光影后处理";
            }, () -> {
                var values = FlyingSwordConfig.TrailPath.values();
                FlyingSwordConfig.TRAIL_PATH.set(values[(FlyingSwordConfig.TRAIL_PATH.get().ordinal() + 1) % values.length]); FlyingSwordConfig.TRAIL_PATH.save();
            });
            action(152, () -> "视角跟随方式：" + switch (FlyingSwordConfig.CAMERA_MODE.get()) {
                case STABLE -> "稳定"; case FOLLOW_ROLL -> "跟随翻滚"; case FULL_LOCKED -> "完全锁定";
            }, () -> {
                var values = FlyingSwordConfig.CameraMode.values();
                FlyingSwordConfig.CAMERA_MODE.set(values[(FlyingSwordConfig.CAMERA_MODE.get().ordinal() + 1) % values.length]); FlyingSwordConfig.CAMERA_MODE.save();
            });
            number(184, "稳定视角倾斜", FlyingSwordConfig.CAMERA_TILT, 0, 20, 1);
            number(222, "普通视距", FlyingSwordConfig.CAMERA_NORMAL, 2, 14, .5);
            number(260, "冲刺视距", FlyingSwordConfig.CAMERA_SPRINT, 2, 14, .5);
            number(298, "螺旋视距", FlyingSwordConfig.CAMERA_SPIRAL, 2, 16, .5);
            number(336, "视角前瞻", FlyingSwordConfig.CAMERA_AHEAD, 0, 3, .1);
            number(374, "速度视野增量", FlyingSwordConfig.FOV_BOOST, 0, 10, .5);
            number(412, "螺旋视野增量", FlyingSwordConfig.SPIRAL_FOV, 0, 15, .5);
            action(472, () -> "松开横滚：" + (FlyingSwordConfig.ROLL_RELEASE_MODE.get() == cn.blockforge.generated.generatedmod.yujian.ManualRoll.ReleaseMode.HOLD_ANGLE ? "保持角度" : "自动回正"), () -> {
                var values = cn.blockforge.generated.generatedmod.yujian.ManualRoll.ReleaseMode.values();
                FlyingSwordConfig.ROLL_RELEASE_MODE.set(values[(FlyingSwordConfig.ROLL_RELEASE_MODE.get().ordinal() + 1) % values.length]); FlyingSwordConfig.ROLL_RELEASE_MODE.save();
            });
            number(504, "横滚灵敏度", FlyingSwordConfig.ROLL_SENSITIVITY, .35, 2, .1);
        } else if(category==FLIGHT) {
            action(28,()->"飞行模式："+(HudConfig.REALM_FLIGHT_ANIMATION.get()?"遁光":"原版飞行"),()->{
                HudConfig.REALM_FLIGHT_ANIMATION.set(!HudConfig.REALM_FLIGHT_ANIMATION.get());
                HudConfig.REALM_FLIGHT_ANIMATION.save();
                cn.blockforge.generated.generatedmod.client.realmflight.RealmFlightVisuals.sendMode();
            });
            toggle(60,"遁光特效",HudConfig.REALM_FLIGHT_EFFECTS);
            toggle(88,"遁光第一人称特效",HudConfig.REALM_FLIGHT_FIRST_PERSON_EFFECTS);
            cameraChoice(116,"遁光默认视角",HudConfig.REALM_FLIGHT_DEFAULT_CAMERA);
            toggle(144,"高速后掠姿态",HudConfig.REALM_FLIGHT_HORIZONTAL);
            toggle(172,"转向镜头侧倾",HudConfig.REALM_FLIGHT_CAMERA);
            number(200,"镜头侧倾幅度",HudConfig.REALM_FLIGHT_CAMERA_SCALE,0,1,.05);
            visualChoice(264, RealmFlightSelection.FOLLOW_CURRENT);
            int y = 296;
            for (int realm : RealmFlightSelection.unlockedRealms(actualRealm())) {
                visualChoice(y, realm); y += 28;
            }
        } else if(category==MANA) {
            action(28,()->"灵力损耗记录："+(tracking()?"开启":"关闭"),()->{
                boolean enabled=!tracking();cn.blockforge.generated.generatedmod.Network.CHANNEL.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.cultivation.ManaLossTogglePacket(enabled));
                if(GeneratedMod.data(minecraft.player) instanceof cn.blockforge.generated.generatedmod.CultivationDataImpl d)d.setManaLossEnabled(enabled);
                host.selectPage("settings");host.scrollToTop();
            });
        } else {
            addRenderableWidget(new CultivationUi.Slider(bodyX,28,bodyW,"人物 UI 整体大小",HudConfig.SCALE,.5,2,.05) {
                @Override protected void updateMessage(){setMessage(Component.literal("人物 UI 整体大小："+Math.round(HudConfig.SCALE.get()*100)+"%"));}
            });
            number(66,"人物横向位置 %",HudConfig.PLAYER_X,0,100,1);
            number(104,"人物纵向位置 %",HudConfig.PLAYER_Y,0,100,1);
            toggle(142,"Boss 自动避让",HudConfig.BOSS_AUTO);
            bossPosition(174,"Boss 横向位置 %",HudConfig.BOSS_X);
            bossPosition(212,"Boss 纵向位置 %",HudConfig.BOSS_Y);
            number(250,"Boss 名称上下微调",HudConfig.BOSS_NAME_OFFSET_Y,-4,4,.5);
            action(290,()->"恢复默认布局",()->{HudConfig.resetLayout();host.selectPage("settings");});
        }
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        CultivationUi.panel(g, 0, 0, width, height, false);
        g.drawString(font, CATEGORIES[category], bodyX, 8, CultivationUi.GOLD, false);
        CultivationUi.rule(g, bodyX, 21, bodyW);
        if(category==MANA&&tracking()){
            var d=GeneratedMod.data(minecraft.player);
            g.drawString(font,"实际扣除记录（最近32次）",bodyX,57,CultivationUi.TEXT,false);
            int y=75;
            for(var row:cn.blockforge.generated.generatedmod.balance.ManaLossLedger.read(d)){
                long seconds=Math.max(0,(System.currentTimeMillis()-row.getLong("At"))/1000);
                String source=row.getString("Source");
                if("境界御空".equals(source))source="遁光";
                CultivationUi.fit(g,Component.literal(seconds+"秒前 · "+source+"  -"+CultivationUi.number(row.getFloat("Amount"))),bodyX,y,bodyW,0x80D9F2,mx,my);y+=16;
            }
        }
        if(category==SWORD){
            section(g,28,"基础显示");section(g,134,"视角与镜头");
            section(g,454,"横滚操控");section(g,546,"调试与兼容");section(g,642,"化虹皮肤");
            int hintY=huahongSkinsExpanded?huahongContentHeight(width)-16:HUAHONG_CHOICES_Y;
            CultivationUi.fit(g,Component.literal("仅改变外观，速度和消耗不变"),bodyX,hintY,bodyW,CultivationUi.MUTED,mx,my);
        }
        if(category==COMBAT){section(g,60,"异火缠绕 · 显示");section(g,198,"异火缠绕 · 伤害");}
        if(category==FLIGHT) {
            section(g,242,"遁光外观（已解锁）");
            int y = 296 + 28 * RealmFlightSelection.unlockedRealms(actualRealm()).size();
            g.drawString(font, actualRealm()<3 ? "元婴境界解锁特效选择" : "仅改变外观，速度和消耗不变", bodyX, y, CultivationUi.MUTED, false);
        }
        super.render(g, mx, my, delta);
    }
    private void section(GuiGraphics g,int y,String label){g.drawString(font,label,bodyX,y,CultivationUi.GOLD,false);}
    private String fireTargetingLabel(){return "伤害非敌对生物："+(fireHurtsNonHostile()?"开启":"关闭");}
}
