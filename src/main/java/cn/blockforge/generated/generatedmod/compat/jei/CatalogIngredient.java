package cn.blockforge.generated.generatedmod.compat.jei;

import cn.blockforge.generated.generatedmod.WuXingItems;
import cn.blockforge.generated.generatedmod.alchemy.*;
import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import cn.blockforge.generated.generatedmod.ProgressionGate;
import cn.blockforge.generated.generatedmod.text.ItemTextPalette;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** A JEI catalog entry is deliberately not an inventory ItemStack or an NBT quality. */
public record CatalogIngredient(PillCatalog pill) {
    public boolean core(){return pill==null;}
    public String id(){return core()?"inner_core":pill.id;}
    public ItemStack cheat(){
        if(core())return new ItemStack(WuXingItems.INNER_CORES[5].get());
        var result=PillData.create(pill,PillData.Grade.PERFECT,PillData.Element.METAL,0);
        return MedicineTier.imported(pill)?MedicineTier.set(result,5):result;
    }
    public List<ItemStack> variants(){
        List<ItemStack> result=new ArrayList<>();
        if(core()){for(var core:WuXingItems.INNER_CORES)result.add(new ItemStack(core.get()));return result;}
        for(var grade:PillData.Grade.values()){
            if(pill==PillCatalog.CALMING&&grade!=PillData.Grade.LOW)continue;
            for(int element=0;element<(pill==PillCatalog.ROOT?5:1);element++)
                for(int tier=0;tier<(pill==PillCatalog.CULTIVATION?9:1);tier++)
                    if(MedicineTier.imported(pill)){
                        for(int rank=1;rank<=5;rank++)result.add(MedicineTier.set(PillData.create(pill,grade,PillData.Element.values()[element],tier),rank));
                    }else result.add(PillData.create(pill,grade,PillData.Element.values()[element],tier));
        }
        return result;
    }
    public Component name(){
        return name(clock());
    }
    private static final int[] CORE_COLORS={ItemTextPalette.MORTAL,ItemTextPalette.UNCOMMON,ItemTextPalette.RARE,ItemTextPalette.EPIC,ItemTextPalette.LEGENDARY,ItemTextPalette.UNIQUE};
    private static double clock(){var mc=net.minecraft.client.Minecraft.getInstance();return mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+(double)mc.getFrameTime();}
    public Component name(double ticks){
        return tint(core()?Component.literal("妖兽内丹"):Component.translatable("item.wuxingxiantu."+pill.id),ticks);
    }
    public int color(double ticks){
        if(pill==PillCatalog.CALMING)return 0xC99CEB;
        int[] palette=core()?CORE_COLORS:new int[]{ItemTextPalette.pillGrade(0),ItemTextPalette.pillGrade(1),ItemTextPalette.pillGrade(2),ItemTextPalette.pillGrade(3)};
        double phase=Math.floorMod((long)Math.floor(ticks),40L*palette.length)/40.0+(ticks-Math.floor(ticks))/40.0;
        int stop=(int)phase;double f=phase-stop;f=f*f*(3-2*f);
        int a=palette[stop],b=palette[(stop+1)%palette.length],rgb=0;
        for(int shift:new int[]{16,8,0})rgb|=((int)Math.round(((a>>shift)&255)*(1-f)+((b>>shift)&255)*f))<<shift;
        return rgb;
    }
    private MutableComponent tint(Component text,double ticks){return ItemTextPalette.colored(text,color(ticks));}
    public List<Component> tooltip(){return tooltip(net.minecraft.client.gui.screens.Screen.hasShiftDown(),clock());}
    public List<Component> tooltip(boolean expanded,double ticks){
        if(!core()){
            // Old variants remain internal recipe lookup keys; they are not the active V5 grading axes.
            var lines=new ArrayList<Component>();lines.add(name(ticks));
            lines.add(Component.literal("丹药统一一至九品；实际能力由炼制结果决定。"));
            lines.add(Component.literal("此目录用于查找用途与材料，不保证旧配方产出相同效果。"));
            lines.add(Component.literal("创造取物后由服务端按旧丹导入规则生成有效凭证。"));
            if(expanded){
                var preview=cn.blockforge.generated.generatedmod.alchemy.effects.LegacyPillImport.preview(cheat());
                lines.add(Component.literal("当前取物模板的导入预览："));
                lines.add(cn.blockforge.generated.generatedmod.alchemy.effects.PillSnapshot.name(preview));
                cn.blockforge.generated.generatedmod.alchemy.effects.PillSnapshot.tooltip(preview,lines);
            }else lines.add(Component.literal("按住 Shift 查看取物模板的导入预览").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        List<Component> lines=new ArrayList<>();lines.add(name(ticks));
        lines.add(tint(Component.literal(core()?"品阶：凡品～仙品":pill==PillCatalog.CALMING?"品质：特殊":"品质：下品～极品"),ticks));
        if(!core()&&MedicineTier.imported(pill))lines.add(tint(Component.literal("丹阶：").append(MedicineTier.name(1)).append("～").append(MedicineTier.name(5)).append("；与品质分别计算"),ticks));
        var intro=cn.blockforge.generated.generatedmod.text.ItemTextCatalog.resolve(core()?new ItemStack(WuXingItems.INNER_CORES[0].get()):cheat());
        if(intro!=null){lines.add(tint(intro.text("lore"),ticks));for(int i=0;i<intro.actions();i++)lines.add(tint(intro.text("action."+i),ticks));}
        if(!core()){
            lines.add(tint(pill==PillCatalog.BIGU?Component.literal("练气期间延长辟谷，每品级15分钟"):
                    Component.translatable("pill.wuxingxiantu.effect."+pill.id),ticks));
            var low=PillData.create(pill,PillData.Grade.LOW,PillData.Element.METAL,0);
            String gate="服用境界："+CultivationLogic.realmName(ProgressionGate.requiredRealmForPill(low))+"以上";
            if(ProgressionGate.requiredRealmForPill(low)!=ProgressionGate.requiredRealmForPill(cheat()))gate+="，高品阶要求更高境界";
            lines.add(tint(Component.literal(gate),ticks));
        }
        if(!expanded){if(pill!=PillCatalog.CALMING)lines.add(Component.literal("按住 Shift 查看各品阶详细效果").withStyle(ChatFormatting.GRAY));return lines;}
        if(core()){
            String[] grades={"凡品","下品","中品","上品","极品","仙品"};
            for(int i=0;i<grades.length;i++){
                String detail=i==5?"用于渡劫丹、天阙剑等高阶炼制；不从普通妖兽击杀掉落":
                        "用于"+CultivationLogic.realmName(i)+(i==4?"及以上境界":"")+"引劫；低于当前所需品阶时服用恢复5%最大灵力";
                final int color=CORE_COLORS[i];lines.add(Component.literal(grades[i]+"："+detail).withStyle(s->s.withColor(color)));
            }
            lines.add(Component.literal("掉落受妖兽品阶与玩家境界共同限制，配方仍要求对应品阶。").withStyle(ChatFormatting.GRAY));
        }else if(pill!=PillCatalog.CALMING){
            for(var grade:PillData.Grade.values()){
                final int color=ItemTextPalette.pillGrade(grade.ordinal());
                var stack=PillData.create(pill,grade,PillData.Element.METAL,0);
                lines.add(Component.literal(Component.translatable("quality.wuxingxiantu."+grade.ordinal()).getString()+"："+effect(grade.value())
                        +"（"+CultivationLogic.realmName(ProgressionGate.requiredRealmForPill(stack))+"起）").withStyle(s->s.withColor(color)));
            }
            if(pill==PillCatalog.CULTIVATION)lines.add(Component.literal("服用还需达到实物对应炉阶的境界；以品阶与炉阶中较高要求为准。").withStyle(ChatFormatting.GOLD));
            if(pill==PillCatalog.CULTIVATION){
                for(int tier=0;tier<9;tier++){
                    StringJoiner gains=new StringJoiner(" / ");for(int g=1;g<=4;g++)gains.add(Long.toString(AlchemyBalance.cultivationGain(tier,g)));
                    lines.add(Component.literal(CultivationLogic.realmName(tier)+"修为增量："+gains).withStyle(ChatFormatting.GRAY));
                }
                lines.add(Component.literal("增量按下/中/上/极品排列；实物炉阶仍单独保留。").withStyle(ChatFormatting.GRAY));
            }
        }
        return lines;
    }
    private String effect(int g){return switch(pill){
        case AURA -> "恢复"+(25*g)+"%最大灵力（不超上限）";
        case GOLDEN_WOUND -> "恢复"+(5+15*g)+"%最大生命（不超上限）";
        case BIGU -> "辟谷"+(15*g)+"分钟；仅炼气期可用";
        case CLEAR_HEART -> "清除道心负面状态与反胃，生命恢复I持续"+(5*g)+"秒；不解除道心不稳";
        case CONTINUE_VEIN -> "修复经脉、清除虚弱，生命恢复I持续"+(10*g)+"秒";
        case GATHERING -> "打坐修炼速度增加"+(15*g)+"%，持续"+(60*g)+"秒";
        case CULTIVATION -> "按炉阶增加修为，具体增量见下表；不超过当前突破上限";
        case FOUNDATION -> "随机觉醒或提升一条五行灵根至"+Component.translatable("quality.wuxingxiantu."+(g-1)).getString()+"；只选可提升灵根";
        case ROOT -> "对应五行灵根觉醒/提升至"+Component.translatable("quality.wuxingxiantu."+(g-1)).getString()+"；金木水火土均可炼制";
        case BREAKTHROUGH -> "修为达到上限时突破一个小阶段；不额外扣灵力，巅峰仍须正式渡劫";
        case TRIBULATION -> "渡劫雷击伤害减免"+(10*g)+"%，持续"+(60*g)+"秒；仅渡劫中使用";
        case DIAMOND_BODY -> "抗性提升"+Math.min(3,g)+"级，伤害吸收"+g+"级，持续"+(30*g)+"秒";
        case BERSERK -> "力量"+Math.min(3,g)+"级、速度I，持续"+(22.5*g)+"秒";
        case GALE -> "速度"+Math.min(3,g)+"级、跳跃提升"+Math.min(2,g)+"级，持续"+(30*g)+"秒";
        case PROFOUND_WATER -> "水下呼吸I、夜视I、急迫"+Math.min(2,g)+"级，持续"+(60*g)+"秒";
        case PHYSIQUE -> "随机改换前七种体质之一，生命恢复"+Math.min(2,g)+"级持续"+(10*g)+"秒";
        case GATHER_QI -> "每秒回复"+(.25*g)+"%最大灵力，持续"+(60*g)+"秒（黄阶），高丹阶延长持续时间；同效取强不叠加";
        case ORIGIN_CONDENSING -> "储备当前阶段"+(5*g)+"%上限的药力，仅打坐时缓慢炼化；不跨阶段";
        case MERIDIAN_EXPANSION -> "黄阶永久经脉容量+"+g+"%，高丹阶增益有界提升；每境界最多两次，共享五行25%经脉上限";
        case BARRIER_PREPARATION -> "巅峰时服用，为下一次合规渡劫预备吸收与恢复，不代替渡劫";
        case REJUVENATION -> "每秒恢复"+(.25*g)+"%最大生命，持续"+(30*g)+"秒（黄阶），高丹阶延长持续时间；仅疗养使用";
        case CALMING -> throw new IllegalStateException("Special pill has no quality scaling");
    };}
}
