package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import cn.blockforge.generated.generatedmod.items.FireSeedItem;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import cn.blockforge.generated.generatedmod.client.CultivationVisionLayout.Box;

/** Reference layout, actual player effects and existing server-authoritative operations. */
public final class CultivationVisionPage extends CultivationPage {
    private record Binding(CultivationUi.Action button, Supplier<Component> label, BooleanSupplier enabled) {}
    private static final String[] ROOT_NAMES={"gold","wood","water","fire","earth"};
    private static final int[] ROOT_WIDTHS={72,74,74,76,73};
    private final List<Binding> bindings=new ArrayList<>();
    private final List<CultivationUi.Action> options=new ArrayList<>();
    private CultivationVisionLayout layout;
    private Box popup;
    private int popupMode,optionFocus,effectScroll;
    private boolean draggingEffects;
    private double effectScrollGrab;
    private static final int EFFECT_ROW=32;
    private List<MobEffectInstance> visibleEffects=List.of();
    public CultivationVisionPage(){super("内视");}
    private CultivationData data(){return GeneratedMod.data(minecraft.player);}
    @Override public int contentHeight(int w,int h){return CultivationVisionLayout.of(w,h).height();}
    private void button(int x,int y,int w,int h,Supplier<Component> label,Runnable action,BooleanSupplier enabled){
        var b=addRenderableWidget(new CultivationUi.Action(x,y,w,h,label.get(),ignored->action.run()));
        b.chrome=h==20?1:2;
        bindings.add(new Binding(b,label,enabled));
    }
    @Override protected void init(){
        layout=CultivationVisionLayout.of(width,height);bindings.clear();closePopup();
        Box domain=layout.domain(),fire=layout.fire(),a=layout.actions(),r=layout.roots();
        button(domain.x()+6,domain.y()+18,domain.w()-12,20,this::selectedDomain,()->openPopup(1),()->data()!=null);
        button(fire.x()+6,fire.y()+18,fire.w()-12,20,this::selectedFire,()->openPopup(2),()->data()!=null);
        for(int root=0;root<5;root++){
            final int index=root;
            var switchButton=addRenderableWidget(new CultivationUi.Action(rootX(r,root)+(rootWidth(r,root)-28)/2,r.y()+18,28,28,
                    Component.literal(ROOT_NAMES[root]),ignored->{if(index==1||index==3)Network.CHANNEL.sendToServer(new RootFormPacket(index==1?0:1));}) {
                @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta) {
                    boolean owned=data()!=null&&(data().wuxingxiantu$getRootMask()&(1<<index))!=0;
                    var texture=ResourceLocation.fromNamespaceAndPath("wuxingxiantu","textures/gui/wuxia/vision/roots/"+ROOT_NAMES[index]+".png");
                    com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();g.setColor(1,1,1,owned?1:.35F);
                    boolean thunder=data()!=null&&index==1&&CultivationLogic.isThunderForm(data());
                    boolean exotic=data()!=null&&index==3&&CultivationLogic.isExoticForm(data());
                    if(thunder||exotic)CultivationVisionAssets.drawRootForm(g,getX(),getY(),28,thunder);
                    else g.blit(texture,getX(),getY(),28,28,0,0,256,256,256,256);
                    g.setColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                    // Passive roots are deliberately inactive buttons. AbstractWidget
                    // isMouseOver requires active=true, but their descriptions must
                    // remain available without granting a click action or using focus.
                    if(visible&&mx>=getX()&&mx<getX()+getWidth()&&my>=getY()&&my<getY()+getHeight()){
                        CultivationUi.tooltip(rootHint(index));
                    }
                }
            });
            bindings.add(new Binding(switchButton,()->Component.literal(ROOT_NAMES[index]),
                    ()->data()!=null&&(index==1||index==3)&&(data().wuxingxiantu$getRootMask()&(1<<index))!=0));
        }
        Box foundation = layout.foundation();
        int meridianY = foundation.y() + 22 + Math.max(18, (foundation.h() - 38) / 3);
        Component acupoints = Component.translatable("gui.wuxingxiantu.baiqiao.entry");
        var entry = addRenderableWidget(new AcupointScreen.BaiqiaoButton(foundation.x() + foundation.w() - 41,
                meridianY - 3, 36, 15, acupoints, button -> minecraft.setScreen(new AcupointScreen(host)), () -> false));
        bindings.add(new Binding(entry, () -> acupoints, () -> data() != null));
        int actionWidth=(a.w()-14)/2;
        button(a.x()+5,a.y()+18,actionWidth,22,()->Component.literal("渡劫"),()->Network.CHANNEL.sendToServer(new TribulationPacket()),
                ()->data()!=null&&TribulationManager.canStart(minecraft.player,data()));
        button(a.x()+9+actionWidth,a.y()+18,a.w()-14-actionWidth,22,()->Component.literal("凝晶"),()->Network.CHANNEL.sendToServer(new CrystallizePacket(net.minecraft.client.gui.screens.Screen.hasControlDown())),
                ()->data()!=null&&data().wuxingxiantu$getAura()>=CultivationLogic.CRYSTALLIZE_AURA_COST);
        tick();
    }
    private Component formHint(int form){
        var d=data();
        var hint=Component.literal(form==0?"木 / 天雷灵根切换":"火 / 异火灵根切换");
        if(d==null)return hint;
        boolean ready=form==0?CultivationLogic.thunderRootAvailable(d):CultivationLogic.exoticRootAvailable(d);
        if(ready)return hint.append("\n点击灵根图标切换形态");
        if(form==0){
            hint.append("\n天雷淬体："+Math.min(d.wuxingxiantu$getThunderStrikes(),CultivationLogic.THUNDER_STRIKES_REQUIRED)+" / "+CultivationLogic.THUNDER_STRIKES_REQUIRED);
            hint.append("\n显化要求：").append(Component.literal(CultivationLogic.realmName(minecraft.player,ProgressionGate.THUNDER_ROOT_REALM)));
            hint.append("，木灵根").append(Component.translatable("quality.wuxingxiantu."+(ProgressionGate.THUNDER_ROOT_QUALITY-1))).append("及以上");
        }else hint.append("\n需解锁异火灵根，并满足境界、火灵根品质及已掌握异火品阶条件");
        return hint;
    }
    @Override public void tick(){
        visibleEffects=minecraft.player==null?List.of():minecraft.player.getActiveEffects().stream()
                .filter(MobEffectInstance::showIcon)
                .sorted(Comparator.comparing((MobEffectInstance e)->!e.getEffect().isBeneficial()).thenComparing(e->e.getEffect().getDisplayName().getString())).toList();
        for(var b:bindings){b.button().setMessage(b.label().get());b.button().active=b.enabled().getAsBoolean();}
        for(int i=0;i<options.size();i++){
            options.get(i).active=popupMode==1?ownsDomain(i):i==9?data()!=null:data()!=null&&data().wuxingxiantu$hasLearnedFire(i+1);
            if(popupMode==1)options.get(i).setMessage(domainLabel(i));
        }
        effectScroll=Math.max(0,Math.min(effectScroll,maxEffectScroll()));
    }
    private Component selectedDomain(){
        if(data()!=null)for(int i=0;i<5;i++)if(domainActive(i))return domainName(i).append("  ▾");
        return Component.literal("未开启领域  ▾");
    }
    private Component selectedFire(){int tier=data()==null?0:data().wuxingxiantu$getEquippedFire();return Component.literal((tier>0?FireSeedItem.getFireName(tier):"未装备异火")+"  ▾");}
    private void openPopup(int mode){
        popupMode=mode;options.clear();optionFocus=0;Box s=mode==1?layout.domain():layout.fire();int h=mode==1?116:112,anchor=s.y()+18;
        popup=new Box(s.x()+5,Math.max(0,anchor-h-3),s.w()-10,h);
        if(mode==1)for(int i=0;i<5;i++){
            final int root=i;
            options.add(new CultivationUi.Action(popup.x()+4,popup.y()+14+i*20,popup.w()-8,19,domainLabel(i),b->{toggleDomain(root);closePopup();},()->domainActive(root)){
                @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
                    super.renderWidget(g,mx,my,delta);
                    if(isMouseOver(mx,my))CultivationUi.tooltip(DomainTooltips.describe(minecraft.player,data(),root));
                }
            });
        }else{
            int start=popup.x()+(popup.w()-78)/2;
            for(int i=1;i<=9;i++){
                final int tier=i;
                options.add(new CultivationUi.Icon(start+(i-1)%3*26,popup.y()+14+(i-1)/3*26,24,"",()->new ItemStack(WuXingBlocks.EXOTIC_FIRE_ITEMS[tier-1].get()),
                        ()->cn.blockforge.generated.generatedmod.text.ExoticFireDescriptions.tooltip(tier,minecraft.level.getGameTime(),
                                Component.literal(FireSeedItem.getFireName(tier)+" · "+(data().wuxingxiantu$getEquippedFire()==tier?"已装备":data().wuxingxiantu$hasLearnedFire(tier)?"已获得":"未获得"))),
                        b->{Network.CHANNEL.sendToServer(new FireSelectPacket(data().wuxingxiantu$getEquippedFire()==tier?0:tier));closePopup();},()->data()!=null&&data().wuxingxiantu$getEquippedFire()==tier));
            }
            options.add(new CultivationUi.Action(popup.x()+4,popup.y()+94,popup.w()-8,16,Component.literal("卸下异火"),b->{Network.CHANNEL.sendToServer(new FireSelectPacket(0));closePopup();}));
        }
        tick();
    }
    private void closePopup(){popupMode=0;popup=null;options.clear();}
    @Override public boolean closeTransientUi(){if(popupMode==0)return false;closePopup();return true;}
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(popupMode==0)return super.keyPressed(key,scan,mods);
        if(key==256){closePopup();return true;}
        if(key==265||key==264||key==258){optionFocus=Math.floorMod(optionFocus+(key==265||(key==258&&(mods&1)!=0)?-1:1),options.size());return true;}
        if(key==257||key==335||key==32){var b=options.get(optionFocus);if(b.active)b.onPress();return true;}
        return true;
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(popupMode!=0){if(!popup.contains(x,y)){closePopup();return true;}for(var b:List.copyOf(options))if(b.mouseClicked(x,y,button))return true;return true;}
        Box e=effectsBox();
        if(button==0&&maxEffectScroll()>0&&x>=e.x()+e.w()-10&&x<e.x()+e.w()&&y>=e.y()+22&&y<e.y()+22+effectsViewHeight()){
            int view=effectsViewHeight(),track=view-6,thumb=Math.min(track,Math.max(16,track*view/Math.max(1,effects().size()*EFFECT_ROW)));
            int top=e.y()+25+effectScroll*(track-thumb)/maxEffectScroll();
            effectScrollGrab=y>=top&&y<top+thumb?y-top:thumb/2.0;
            draggingEffects=true;moveEffectScroll(y);return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseReleased(double x,double y,int button){if(draggingEffects){draggingEffects=false;return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(draggingEffects){moveEffectScroll(y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    private void moveEffectScroll(double y){
        Box e=effectsBox();int view=effectsViewHeight(),track=view-6,total=effects().size()*EFFECT_ROW,thumb=Math.min(track,Math.max(16,track*view/Math.max(1,total)));
        effectScroll=(int)Math.round(Math.max(0,Math.min(1,(y-e.y()-25-effectScrollGrab)/Math.max(1,track-thumb)))*maxEffectScroll());
    }
    @Override public boolean mouseScrolled(double x,double y,double amount){
        if(popupMode!=0)return true;
        if(effectsBox().contains(x,y)&&maxEffectScroll()>0){effectScroll=(int)Math.max(0,Math.min(maxEffectScroll(),effectScroll-amount*EFFECT_ROW));return true;}
        return super.mouseScrolled(x,y,amount);
    }
    private Box effectsBox(){return host.visionEffectsBounds(layout.effects());}
    private List<MobEffectInstance> effects(){
        return visibleEffects;
    }
    private int effectsViewHeight(){return Math.max(1,effectsBox().h()-36);}
    private int maxEffectScroll(){return layout==null?0:Math.max(0,effects().size()*EFFECT_ROW-effectsViewHeight());}
    private void card(GuiGraphics g,Box b,String title){
        // Use the screen's continuous paper: the card texture itself contained inset margins.
        if(b.y()>0)CultivationUi.rule(g,b.x(),b.y(),b.w());
        g.drawString(font,title,b.x()+6,b.y()+5,CultivationUi.INK,false);
        int start=b.x()+Math.min(b.w()-8,font.width(title)+12);
        CultivationBorderAssets.draw(g,"rule_ink",start,b.y()+6,b.x()+b.w()-5-start,4);
    }
    private void line(GuiGraphics g,Box b,Component value,int y,int color,int mx,int my){CultivationUi.fit(g,value,b.x()+6,b.y()+y,b.w()-12,color,mx,my);}
    private void attributeLine(GuiGraphics g,Box b,String icon,Component value,int y,int color,int mx,int my){
        CultivationVisionAssets.draw(g,icon,b.x()+5,b.y()+y+(font.lineHeight-18)/2,18,18);
        CultivationUi.fit(g,value,b.x()+28,b.y()+y,b.w()-34,color,mx,my);
    }
    private void labelledAttribute(GuiGraphics g,Box b,String icon,String label,Component value,int y,int color,int mx,int my){
        CultivationVisionAssets.draw(g,icon,b.x()+5,b.y()+y+(font.lineHeight-18)/2,18,18);
        int x=b.x()+28,labelWidth=font.width(label+"：");
        g.drawString(font,label+"：",x,b.y()+y,CultivationUi.INK,false);
        CultivationUi.fit(g,value,x+labelWidth,b.y()+y,Math.max(0,b.w()-34-labelWidth),color,mx,my);
    }
    private void compactAttribute(GuiGraphics g,Box b,String icon,Component value,int y,int color){
        int inset=27;
        int available=Math.max(1,b.w()-inset-3);
        float scale=Math.max(.8F,Math.min(1F,available/(float)Math.max(1,font.width(value))));
        Component visible=value;
        if(font.width(value)*scale>available)visible=Component.literal(font.plainSubstrByWidth(value.getString(),Math.max(0,(int)(available/scale)-font.width("…")))+"…");
        if(icon!=null)CultivationVisionAssets.draw(g,icon,b.x()+4,b.y()+y+Math.round((font.lineHeight*scale-18)/2F),18,18);
        g.pose().pushPose();g.pose().translate(b.x()+inset,b.y()+y,0);g.pose().scale(scale,scale,1);
        g.drawString(font,visible,0,0,color,false);g.pose().popPose();
    }
    private void resource(GuiGraphics g,Box b,String label,int y,double value,double max,int color,int mx,int my){
        String icon=label.equals("生命")?"health":label.equals("灵力")?"mana":"cultivation";
        // Center the icon across the text row and its bar (y through y+19).
        CultivationVisionAssets.draw(g,icon,b.x()+5,b.y()+y+(19-18)/2,18,18);
        CultivationUi.fit(g,Component.literal(label+"："+CultivationUi.number(value)+" / "+CultivationUi.number(max)),
                b.x()+28,b.y()+y,b.w()-34,CultivationUi.INK,mx,my);
        int x=b.x()+28,w=b.w()-34;
        String asset=label.equals("生命")?"bar_health":label.equals("灵力")?"bar_mana":"bar_cultivation";
        CultivationVisionAssets.draw(g,asset+"_track",x,b.y()+y+12,w,7);
        CultivationVisionAssets.drawProgress(g,asset,x,b.y()+y+12,w,7,value/Math.max(1,max));
        if(label.equals("灵力")&&mx>=b.x()&&mx<b.x()+b.w()&&my>=b.y()+y&&my<b.y()+y+21){
            CultivationUi.tooltip(CultivationUi.manaTooltip(data(),value,max));
        }
    }
    private void renderStatus(GuiGraphics g,int mx,int my){
        var d=data();Box b=layout.status();card(g,b,"修炼状态");
        attributeLine(g,b,"realm",Component.literal("境界："+CultivationLogic.realmStageName(minecraft.player,d)),22,CultivationUi.INK,mx,my);
        resource(g,b,"修为",42,d.wuxingxiantu$getCultivation(),CultivationLogic.breakthroughRequirement(d),0xFFD6AF5C,mx,my);
        resource(g,b,"灵力",66,d.wuxingxiantu$getCurrentMana(),CultivationLogic.maxAura(d),0xFF4BAFC4,mx,my);
        resource(g,b,"生命",90,minecraft.player.getHealth(),minecraft.player.getMaxHealth(),0xFFCE7666,mx,my);
        CultivationUi.softDivider(g,b.x()+6,b.y()+116,b.w()-12,false,0x888E7B58);
        compactAttribute(g,b,"attack",Component.literal("攻击："+CultivationUi.number(minecraft.player.getAttributeValue(Attributes.ATTACK_DAMAGE))),124,CultivationUi.INK);
        String heart=d.wuxingxiantu$getEquippedHeartMethod();
        Component method=Component.literal("心法：").append(heart==null||heart.isBlank()?Component.literal("无"):HeartMethodCatalog.shortName(heart));
        Component merit=Component.literal("功德："+(d.wuxingxiantu$getMerit()==Long.MAX_VALUE?"∞":CultivationUi.number(d.wuxingxiantu$getMerit())));
        // Give the height reclaimed from the chaos-root footer to both attribute gaps.
        int rowGap=(b.h()-14-124)/3;
        int armorY=124+rowGap,meritY=124+rowGap*2,heartY=b.h()-14;
        compactAttribute(g,b,"armor",Component.literal("护甲："+CultivationUi.number(minecraft.player.getAttributeValue(Attributes.ARMOR))),armorY,CultivationUi.INK);
        compactAttribute(g,b,"merit",merit,meritY,d.wuxingxiantu$getMerit()<0?0xFFAB302C:CultivationUi.INK);
        compactAttribute(g,b,"heart_method",method,heartY,CultivationUi.INK);
        if(b.contains(mx,my)&&my>=b.y()+119&&my<b.y()+135)CultivationUi.tooltip(Component.literal("攻击："+CultivationUi.number(minecraft.player.getAttributeValue(Attributes.ATTACK_DAMAGE))));
        if(b.contains(mx,my)&&mx>=b.x()+4&&mx<b.x()+b.w()-4) {
            if(my>=b.y()+heartY-5&&my<b.y()+heartY+11)
                CultivationUi.tooltip(heartHint(d));
            else if(my>=b.y()+meritY-5&&my<b.y()+meritY+11)CultivationUi.tooltip(merit);
            else if(my>=b.y()+armorY-5&&my<b.y()+armorY+11)CultivationUi.tooltip(Component.literal("护甲："+CultivationUi.number(minecraft.player.getAttributeValue(Attributes.ARMOR))));
        }
    }
    private void renderFoundation(GuiGraphics g,int mx,int my){
        var d=data();Box b=layout.foundation();card(g,b,"修行根基");int dao=Math.max(0,Math.min(8,d.wuxingxiantu$getDaoXin()));
        int step=Math.max(18,(b.h()-38)/3);
        labelledAttribute(g,b,"dao","道心",Component.translatable("dao.wuxingxiantu."+dao),22,dao==4?0xFFAB302C:CultivationUi.INK,mx,my);
        int status=d.wuxingxiantu$getMeridianStatus();
        labelledAttribute(g,new Box(b.x(), b.y(), b.w()-40, b.h()),"meridian","经脉",Component.translatable("gui.wuxingxiantu.inner_vision.meridian_"+(status>=2?"shattered":status==1?"damaged":"intact")),22+step,status>0?0xFFAB302C:0xFF30694F,mx,my);
        labelledAttribute(g,b,"physique","体质",Component.translatable("physique.wuxingxiantu."+d.wuxingxiantu$getPhysique().id()),22+step*2,0xFF266779,mx,my);
        labelledAttribute(g,b,"purity","阳神纯度",Component.literal(d.wuxingxiantu$getYinShenPurity()+"%"),22+step*3,0xFF266779,mx,my);
        if(b.contains(mx,my)) {
            int row=my-b.y();
            if(row>=17&&row<33)CultivationUi.tooltip(Component.translatable("dao.wuxingxiantu."+dao).append("\n").append(Component.translatable("dao.effect.wuxingxiantu."+dao)));
            else if(row>=17+step&&row<33+step&&mx<b.x()+b.w()-41)CultivationUi.tooltip(status==0?Component.literal("经脉完整\n经脉无损伤减益"):
                    Component.translatable("effect.wuxingxiantu.meridian_"+(status>=2?"shattered":"damaged")+".description"));
            else if(row>=17+step*2&&row<33+step*2)CultivationUi.tooltip(physiqueHint(d));
        }
    }
    static Component physiqueHint(CultivationData d){
        boolean advanced=d.wuxingxiantu$getRealm()>=3;
        String effect=switch(d.wuxingxiantu$getPhysique()){
            case TAICHU->"无额外体质效果";
            case QINGLIAN->advanced?"灵力上限 +10%，灵力恢复 +25%":"灵力恢复 +15%";
            case JIUQIAO->advanced?"灵力上限 +20%，灵力恢复 +10%":"灵力上限 +10%";
            case XUANHUANG->advanced?"生命上限 +20%，护甲 +4":"生命上限 +10%，护甲 +2";
            case ZHANWANG->advanced?"攻击伤害 +15%":"攻击伤害 +8%";
            case TAIYIN->"冰冻伤害 +15%，受到火焰伤害 -20%";
            case TAIYANG->"火焰伤害 +15%，受到冰冻伤害 -20%";
            case WANLING->"生命上限 +25%，攻击伤害 +15%，抗性提升 I\n灵力上限、灵力恢复 "+(advanced?"+25%":"+15%");
        };
        return Component.translatable("physique.wuxingxiantu."+d.wuxingxiantu$getPhysique().id()).append("\n"+effect);
    }
    static Component heartHint(CultivationData d){
        String id=d.wuxingxiantu$getEquippedHeartMethod();
        return Component.literal("当前心法：").append(HeartMethodCatalog.isValid(id)
                ?HeartMethodCatalog.shortName(id).copy().append("\n").append(HeartMethodCatalog.description(id))
                :Component.literal("无\n当前未运转心法，不产生心法加成"));
    }
    private void renderRoots(GuiGraphics g,int mx,int my){
        var d=data();Box b=layout.roots();card(g,b,"灵根");
        for(int i=0;i<5;i++){
            int cell=rootWidth(b,i),x=rootX(b,i);boolean owned=(d.wuxingxiantu$getRootMask()&(1<<i))!=0;
            Component grade=!owned?Component.literal("未有"):Component.translatable("quality.wuxingxiantu."+Math.max(0,Math.min(3,d.wuxingxiantu$getRootQuality(i)-1)));
            CultivationUi.fit(g,grade,x+Math.max(0,(cell-font.width(grade))/2),b.y()+48,cell,CultivationUi.INK,mx,my);
            if(mx>=x&&mx<x+cell&&my>=b.y()+46&&my<b.y()+59)CultivationUi.tooltip(rootHint(i));
        }
    }
    private static int rootWidth(Box b,int root){return (b.w()-10)/5;}
    private static int rootX(Box b,int root){int x=b.x()+5;for(int i=0;i<root;i++)x+=rootWidth(b,i);return x;}
    private void renderSelections(GuiGraphics g){card(g,layout.domain(),"领域");card(g,layout.fire(),"异火");}
    private void renderActions(GuiGraphics g,int mx,int my){
        var d=data();Box b=layout.actions();
        card(g,b,"修炼操作");
        if(b.contains(mx,my)&&my>=b.y()+18&&my<b.y()+40){
            if(mx<b.x()+b.w()/2)CultivationUi.tooltip(Component.literal((TribulationManager.canStart(minecraft.player,d)?"已满足渡劫条件":"渡劫条件未满足")+"\n渡劫条件：大境界巅峰、足够修为、七成灵力、引劫材料，并离开洞天。"));
            else CultivationUi.tooltip(Component.literal("点击：消耗 "+CultivationUi.number(CultivationLogic.CRYSTALLIZE_AURA_COST)+" 灵力，凝成 1 枚灵力结晶\nCtrl + 点击：消耗 "+CultivationUi.number((long)CultivationLogic.CRYSTALLIZE_AURA_COST*WuXingItems.AURA_CRYSTAL.get().getMaxStackSize())+" 灵力，凝成一组\n灵力不足一组时不会扣除灵力"));
        }
    }
    private void renderEffects(GuiGraphics g,int mx,int my){
        Box b=effectsBox();host.drawVisionEffectsBackground(g,b);var list=effects();
        g.drawString(font,"药水状态 · "+list.size(),b.x()+6,b.y()+6,CultivationUi.GOLD,false);CultivationUi.softDivider(g,b.x()+5,b.y()+18,b.w()-10,false);
        int view=effectsViewHeight();effectScroll=Math.min(effectScroll,Math.max(0,list.size()*EFFECT_ROW-view));host.clipPage(g,b.x()+4,b.y()+22,b.w()-13,view);
        if(list.isEmpty())g.drawString(font,"暂无药水效果",b.x()+7,b.y()+28,CultivationUi.MUTED,false);
        for(int i=0;i<list.size();i++){
            int y=b.y()+22+i*EFFECT_ROW-effectScroll;if(y+EFFECT_ROW-1<b.y()+22||y>=b.y()+22+view)continue;
            var instance=list.get(i);var effect=instance.getEffect();
            g.blit(b.x()+6,y+5,0,18,18,minecraft.getMobEffectTextures().get(effect));
            String level=effect==ModEffects.REALM_FLIGHT.get()
                    ?cn.blockforge.generated.generatedmod.realmflight.RealmFlightLevel.labelForAmplifier(instance.getAmplifier())
                    :"Lv."+(instance.getAmplifier()+1);
            Component name=Component.translatable(effect.getDescriptionId()).append(" "+level);
            int nameWidth=Math.max(1,b.w()-40);
            CultivationUi.fit(g,name,b.x()+28,y+3,nameWidth,CultivationUi.TEXT,mx,my);
            var duration=MobEffectUtil.formatDuration(instance,1.0F);CultivationUi.fit(g,duration,b.x()+28,y+17,Math.max(12,b.w()-40),CultivationUi.GOLD,mx,my);
            CultivationUi.rule(g,b.x()+5,y+EFFECT_ROW-2,b.w()-17);
            if(b.contains(mx,my)&&mx<b.x()+b.w()-9&&my>=Math.max(y,b.y()+22)&&my<Math.min(y+EFFECT_ROW-1,b.y()+22+view))CultivationUi.tooltip(name.copy().append("\n").append(duration));
        }
        g.disableScissor();
        if(maxEffectScroll()>0){int total=list.size()*EFFECT_ROW,track=view-6,thumb=Math.min(track,Math.max(16,track*view/total)),y=b.y()+25+effectScroll*(track-thumb)/maxEffectScroll();CultivationUi.scrollbar(g,b.x()+b.w()-6,b.y()+22,view,y,thumb);}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        if(data()==null){g.drawString(font,"正在等待修炼数据同步",6,6,CultivationUi.MUTED,false);return;}
        // All paper sections share the screen background, separated by the original UI rules.
        renderStatus(g,mx,my);renderFoundation(g,mx,my);renderRoots(g,mx,my);renderSelections(g);renderActions(g,mx,my);renderEffects(g,mx,my);super.render(g,mx,my,delta);
        if(layout.foundation().x()>0)CultivationBorderAssets.draw(g,"rule_gold_vertical",
                layout.foundation().x()-2,0,4,layout.height());
        if(popupMode!=0){
            g.pose().pushPose();g.pose().translate(0,0,100);
            CultivationUi.pickerPanel(g,popup.x(),popup.y(),popup.w(),popup.h(),popupMode==1?"选择领域 · 再点关闭":"选择异火");
            for(int i=0;i<options.size();i++){var b=options.get(i);b.render(g,mx,my,delta);if(i==optionFocus)g.renderOutline(b.getX(),b.getY(),b.getWidth(),b.getHeight(),CultivationUi.CYAN);}g.pose().popPose();
        }
    }
    private Component rootHint(int root){
        var d=data();String key=root==5?"chaos":root==1&&CultivationLogic.isThunderForm(d)?"thunder":root==3&&CultivationLogic.isExoticForm(d)?"exotic_fire":Integer.toString(root);
        boolean owned=root==5?d.wuxingxiantu$hasChaosRoot():(d.wuxingxiantu$getRootMask()&1<<root)!=0;
        var hint=Component.translatable("root.wuxingxiantu."+key).withStyle(net.minecraft.ChatFormatting.GOLD).append("\n").append(Component.translatable(owned?"tooltip.wuxingxiantu.root_owned":"tooltip.wuxingxiantu.root_missing").withStyle(owned?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.RED));
        if(root<5&&owned)hint.append(" · ").append(cn.blockforge.generated.generatedmod.text.ItemTextPalette.colored(Component.translatable("quality.wuxingxiantu."+Math.max(0,Math.min(3,d.wuxingxiantu$getRootQuality(root)-1))),cn.blockforge.generated.generatedmod.text.ItemTextPalette.pillGrade(Math.max(0,Math.min(3,d.wuxingxiantu$getRootQuality(root)-1)))));
        hint.append("\n").append(Component.translatable("tooltip.wuxingxiantu.root_"+key).withStyle(net.minecraft.ChatFormatting.GRAY));
        if(root==1||root==3)hint.append("\n").append(formHint(root==1?0:1).copy().withStyle(net.minecraft.ChatFormatting.AQUA));
        if(root<5){
            String domain=root==1&&CultivationLogic.isThunderForm(d)?"thunder":root==3&&CultivationLogic.isExoticForm(d)?"exotic":ROOT_NAMES[root];
            hint.append("\n").append(Component.translatable(!ownsDomain(root)?"tooltip.wuxingxiantu.domain_locked":domainActive(root)?"tooltip.wuxingxiantu.domain_active":"tooltip.wuxingxiantu.domain_inactive"));
        }
        if(root<5)hint.append("\n").append(DomainTooltips.describeCurrent(minecraft.player,d,root));
        hint.append(Component.literal(String.format(java.util.Locale.ROOT,
                "\n灵根总数：%d · 修炼效率：%.1f%%\n灵力容量：%.0f%% · 灵根恢复倍率：%.1f%%\n恢复以境界基础值计算；直接修为奖励不受灵根修炼倍率影响",
                cn.blockforge.generated.generatedmod.balance.RootBalance.count(d),cn.blockforge.generated.generatedmod.balance.RootBalance.training(d)*100,
                cn.blockforge.generated.generatedmod.balance.RootBalance.capacity(d)*100,cn.blockforge.generated.generatedmod.balance.RootBalance.recovery(d)*100)));
        return hint;
    }
    private boolean ownsDomain(int root){return data()!=null&&(data().wuxingxiantu$getRootMask()&(1<<root))!=0&&data().wuxingxiantu$getRootQuality(root)>=2;}
    private boolean domainActive(int root){var d=data();if(d==null)return false;return switch(root){case 0->d.wuxingxiantu$isGoldDomainEnabled();case 1->d.wuxingxiantu$isWoodDomainEnabled();case 2->d.wuxingxiantu$isWaterDomainEnabled();case 3->d.wuxingxiantu$isFireDomainEnabled();default->d.wuxingxiantu$isEarthDomainEnabled();};}
    private net.minecraft.network.chat.MutableComponent domainName(int root){String name=switch(root){case 0->"gold";case 1->CultivationLogic.isThunderForm(data())?"thunder":"wood";case 2->"water";case 3->CultivationLogic.isExoticForm(data())?"exotic":"fire";default->"earth";};return Component.translatable("gui.wuxingxiantu.inner_vision."+name+"_domain_name");}
    private Component domainLabel(int root){return domainName(root);}
    private void toggleDomain(int root){boolean enable=!domainActive(root);Object packet=switch(root){case 0->new GoldDomainPacket(enable);case 1->new WoodDomainPacket(enable);case 2->new WaterDomainPacket(enable);case 3->new FireDomainPacket(enable);default->new EarthDomainPacket(enable);};Network.CHANNEL.sendToServer(packet);}
}
