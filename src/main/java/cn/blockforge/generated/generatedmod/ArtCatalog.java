package cn.blockforge.generated.generatedmod;

import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** 集中维护功法品级、类型、熟练度和界面显示规则。 */
public final class ArtCatalog {
    public enum Type {
        ATTACK("攻击型"), DEFENSE("防御型"), HEALING("治疗型"), CONTROL("控制型"), BUFF("增益型"), SPECIAL("特殊型");
        private final String name;
        Type(String name) { this.name = name; }
        public String typeName() { return name; }
    }

    /** 攻击功法的几何判定，伤害范围由名称语义决定，不再用统一圆形兜底。 */
    public enum AttackShape { NONE, CONE, LINE, CIRCLE, RING, TARGET_AREA }

    public record AttackProfile(AttackShape shape, double range, double width, double angleDegrees) {
        public boolean damaging() { return shape != AttackShape.NONE && range > 0.0D; }
        public String description() {
            return switch (shape) {
                case CONE -> String.format(java.util.Locale.ROOT, "前方 %.1f 格扇形（%.0f°）", range, angleDegrees);
                case LINE -> String.format(java.util.Locale.ROOT, "前方 %.1f 格直线（宽 %.1f 格）", range, width);
                case CIRCLE -> String.format(java.util.Locale.ROOT, "自身周围 %.1f 格圆形", range);
                case RING -> String.format(java.util.Locale.ROOT, "自身周围半径 %.1f 格环形（宽 %.1f 格）", range, width);
                case TARGET_AREA -> String.format(java.util.Locale.ROOT, "准心落点 %.1f 格圆域（施法距离 %.1f 格）", width, range);
                default -> "无直接攻击范围";
            };
        }
    }

    public record Entry(String id, int tier, Type type) {}

    public static final List<Entry> ENTRIES = List.of(
            new Entry("art_sword_aura", 1, Type.ATTACK),
            new Entry("art_renewal", 1, Type.HEALING),
            new Entry("art_spirit_shield", 1, Type.DEFENSE),
            new Entry("art_immobilize", 1, Type.CONTROL),
            new Entry("art_light_body", 1, Type.BUFF),
            new Entry("art_golden_bell", 2, Type.DEFENSE),
            new Entry("art_ice_spike", 2, Type.ATTACK),
            new Entry("art_spirit_gathering", 2, Type.BUFF),
            new Entry("art_armor_break", 2, Type.ATTACK),
            new Entry("art_flame_wave", 3, Type.ATTACK),
            new Entry("art_flash_step", 3, Type.SPECIAL),
            new Entry("art_purification", 3, Type.HEALING),
            new Entry("art_bloodlust", 3, Type.BUFF),
            new Entry("art_thunder", 3, Type.CONTROL),
            new Entry("art_myriad_swords", 4, Type.ATTACK),
            new Entry("art_wanjian_guizong", 4, Type.ATTACK),
            new Entry("art_jiuxiao_sword_rain", 4, Type.ATTACK),
            new Entry("art_immovable_mountain", 4, Type.DEFENSE),
            new Entry("art_human_harmony", 4, Type.BUFF),
            new Entry("art_five_elements", 4, Type.ATTACK),
            new Entry("art_sword_pierce", 1, Type.ATTACK),
            new Entry("art_sword_sweep", 2, Type.ATTACK),
            new Entry("art_sword_guard", 2, Type.DEFENSE),
            new Entry("art_sword_array", 4, Type.ATTACK),
            // 御剑凌霄诀必须作为可学习功法登记：御剑资格判定依赖“已学习”记录，
            // 学习/释放的境界门槛统一由 ProgressionGate.ART_OVERRIDES 给出。
            new Entry("art_sword_flight", 2, Type.SPECIAL));

    private ArtCatalog() {}

    public static Entry entry(String id) {
        return ENTRIES.stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
    }

    public static boolean isValid(String id) { return entry(id) != null; }

    /** 被动槽只接受不带主动攻击判定的功法。 */
    public static boolean isPassiveCompatible(String id) {
        Type type = type(id);
        return isValid(id) && type != Type.ATTACK && type != Type.CONTROL && !"art_flash_step".equals(id)
                && !id.startsWith("art_sword_");
    }

    public static int tier(String id) {
        Entry entry = entry(id);
        return entry == null ? 1 : entry.tier();
    }

    public static Type type(String id) {
        Entry entry = entry(id);
        return entry == null ? Type.ATTACK : entry.type();
    }

    public static AttackProfile attackProfile(String id) {
        return switch (id) {
            case "art_sword_aura" -> new AttackProfile(AttackShape.CONE, 4.5D, 0.0D, 72.0D);
            case "art_ice_spike" -> new AttackProfile(AttackShape.LINE, 8.0D, 1.8D, 0.0D);
            case "art_armor_break" -> new AttackProfile(AttackShape.CONE, 3.8D, 0.0D, 54.0D);
            case "art_flame_wave" -> new AttackProfile(AttackShape.CONE, 6.5D, 0.0D, 108.0D);
            case "art_thunder" -> new AttackProfile(AttackShape.TARGET_AREA, 16.0D, 5.0D, 360.0D);
            case "art_myriad_swords" -> new AttackProfile(AttackShape.TARGET_AREA, 24.0D, 7.0D, 360.0D);
            case "art_wanjian_guizong" -> new AttackProfile(AttackShape.CIRCLE, 6.0D, 0.0D, 360.0D);
            case "art_jiuxiao_sword_rain" -> new AttackProfile(AttackShape.TARGET_AREA, 24.0D, 10.0D, 360.0D);
            case "art_five_elements" -> new AttackProfile(AttackShape.TARGET_AREA, 18.0D, 8.0D, 360.0D);
            case "art_sword_pierce" -> new AttackProfile(AttackShape.LINE, 24.0D, 1.0D, 0.0D);
            case "art_sword_sweep" -> new AttackProfile(AttackShape.CIRCLE, 3.5D, 0.0D, 360.0D);
            case "art_sword_array" -> new AttackProfile(AttackShape.TARGET_AREA, 24.0D, 4.0D, 360.0D);
            default -> new AttackProfile(AttackShape.NONE, 0.0D, 0.0D, 0.0D);
        };
    }

    public static String tierName(int tier) {
        return switch (Math.max(1, Math.min(4, tier))) {
            case 4 -> "天阶";
            case 3 -> "地阶";
            case 2 -> "玄阶";
            default -> "黄阶";
        };
    }

    public static int tierColor(int tier) {
        return switch (Math.max(1, Math.min(4, tier))) {
            case 4 -> 0xFFFFC94A;
            case 3 -> 0xFFE2A7F5;
            case 2 -> 0xFF80D9F2;
            default -> 0xFFF4E8D0;
        };
    }

    public static Component displayName(String id) {
        return Component.translatable("item.wuxingxiantu." + id);
    }

    public static Component tierLabel(String id) {
        return Component.literal("【" + tierName(tier(id)) + "】").withStyle(style -> style.withColor(tierColor(tier(id))));
    }

    /** 界面中的功法名不复用物品名，避免显示“功法秘籍·”前缀。 */
    public static Component artName(String id) {
        String name = displayName(id).getString();
        if (name.startsWith("功法秘籍·")) name = name.substring("功法秘籍·".length());
        if (name.startsWith("Art Manual: ")) name = name.substring("Art Manual: ".length());
        return Component.literal(name);
    }

    public static Component labeledName(String id) {
        return Component.literal(tierLabel(id).getString() + artName(id).getString())
                .withStyle(style -> style.withColor(tierColor(tier(id))));
    }

    public static String masteryName(int uses) {
        int safe = Math.max(0, Math.min(100, uses));
        if (safe >= 100) return "登峰造极";
        if (safe >= 60) return "炉火纯青";
        if (safe >= 30) return "融会贯通";
        if (safe >= 10) return "略有小成";
        return "初窥门径";
    }

    /** 熟练度条按五级使用不同颜色，颜色只表达等级，不额外占用文字空间。 */
    public static int masteryColor(int uses) {
        int safe = Math.max(0, Math.min(100, uses));
        if (safe >= 100) return 0xFFFFC94A;
        if (safe >= 60) return 0xFFE2A7F5;
        if (safe >= 30) return 0xFF80D9F2;
        if (safe >= 10) return 0xFF79C58B;
        return 0xFFB7B0A3;
    }

    public static int effectPercent(int uses) {
        int safe = Math.max(0, Math.min(100, uses));
        if (safe >= 100) return 30;
        if (safe >= 60) return 15;
        if (safe >= 30) return 10;
        if (safe >= 10) return 5;
        return 0;
    }

    public static int masteryLevel(int uses) {
        int safe = Math.max(0, Math.min(100, uses));
        if (safe >= 100) return 5;
        if (safe >= 60) return 4;
        if (safe >= 30) return 3;
        if (safe >= 10) return 2;
        return 1;
    }

    /** 兼容旧调用方的固定灵力部分；最终消耗还会按最大灵力追加百分比部分。 */
    public static int auraCost(String id) {
        return switch (id) {
            case "art_sword_aura" -> 4;
            case "art_renewal" -> 3;
            case "art_spirit_shield" -> 5;
            case "art_immobilize" -> 4;
            case "art_light_body" -> 3;
            case "art_golden_bell" -> 4;
            case "art_ice_spike" -> 4;
            case "art_spirit_gathering" -> 3;
            case "art_armor_break" -> 5;
            case "art_flame_wave" -> 10;
            case "art_flash_step" -> 8;
            case "art_purification" -> 9;
            case "art_bloodlust" -> 10;
            case "art_thunder" -> 12;
            case "art_myriad_swords" -> 18;
            case "art_wanjian_guizong" -> 6;
            case "art_jiuxiao_sword_rain" -> 8;
            case "art_immovable_mountain" -> 16;
            case "art_human_harmony" -> 18;
            case "art_five_elements" -> 24;
            case "art_sword_pierce" -> 5;
            case "art_sword_sweep" -> 4;
            case "art_sword_guard" -> 5;
            case "art_sword_array" -> 18;
            case "art_sword_flight" -> 8;
            default -> 10;
        };
    }

    public static int fixedManaCost(String id) { return auraCost(id); }

    /** 常规施术成本：黄/玄/地/天阶分别2/3/4/6%；御剑召剑独立保留5%。 */
    public static int percentageCost(String id) {
        return switch (id) {
            case "art_sword_aura", "art_renewal", "art_spirit_shield", "art_immobilize", "art_light_body",
                    "art_sword_pierce" -> 2;
            case "art_golden_bell", "art_ice_spike", "art_spirit_gathering", "art_armor_break",
                    "art_sword_sweep", "art_sword_guard" -> 3;
            case "art_sword_flight" -> 5;
            case "art_wanjian_guizong" -> 3;
            case "art_jiuxiao_sword_rain" -> 4;
            case "art_flame_wave", "art_flash_step", "art_purification", "art_bloodlust", "art_thunder" -> 4;
            default -> tier(id) == 4 ? 6 : 2;
        };
    }

    /** One cost breakdown shared by all client surfaces; charging uses finalManaCost. */
    public static net.minecraft.network.chat.Component manaCostDescription(String id,float maxMana) {
        if (cn.blockforge.generated.generatedmod.arts.SustainedSwordArts.supports(id))
            return Component.translatable("gui.wuxingxiantu.sustained_art_cost",fixedManaCost(id),percentageCost(id),
                    String.format(java.util.Locale.ROOT,"%.1f",finalManaCost(id,maxMana)));
        return net.minecraft.network.chat.Component.translatable("gui.wuxingxiantu.art_cost_breakdown",
                fixedManaCost(id),percentageCost(id),String.format(java.util.Locale.ROOT,"%.1f",finalManaCost(id,maxMana)));
    }

    public static float finalManaCost(String id, float maxMana) {
        return Math.max(0.0F, fixedManaCost(id) + Math.max(0.0F, maxMana) * percentageCost(id) / 100.0F);
    }

    /** 功法秘籍和轮盘共用的功能说明。 */
    public static Component function(String id) {
        return Component.translatable("art.wuxingxiantu.function." + id);
    }

    /** 功法冷却，单位为游戏刻。 */
    public static int cooldownTicks(String id) {
        if (id.equals("art_wanjian_guizong")) return 480;
        if (id.equals("art_jiuxiao_sword_rain")) return 600;
        return switch (tier(id)) {
            case 1 -> 100;
            case 2 -> 160;
            case 3 -> 240;
            default -> 400;
        };
    }

    public static int currentCooldownTicks(String id, int uses) {
        int base = cooldownTicks(id);
        return uses >= 60 ? Math.max(1, Math.round(base * 0.90F)) : base;
    }

    public static int shieldDurationTicks(String id, int uses) {
        if (id.equals("art_spirit_shield")) return 400 + effectPercent(uses) * 4;
        return tier(id) == 4 && effectPercent(uses) >= 30 ? 140 : 100;
    }

    public static String cooldownText(String id, int uses) {
        float seconds = currentCooldownTicks(id, uses) / 20.0F;
        return String.format(java.util.Locale.ROOT, "%.1f秒", seconds);
    }

    public static int damageBase(String id) {
        return switch (tier(id)) {
            case 1 -> 8;
            case 2 -> 16;
            case 3 -> 28;
            default -> 50;
        };
    }

    public static float artPowerMultiplier(String id) {
        return switch (id) {
            case "art_sword_aura", "art_renewal", "art_spirit_shield", "art_immobilize", "art_light_body",
                    "art_sword_pierce" -> 0.90F;
            case "art_golden_bell", "art_ice_spike", "art_spirit_gathering", "art_armor_break",
                    "art_sword_sweep", "art_sword_guard", "art_sword_flight" -> 1.15F;
            case "art_flame_wave", "art_flash_step", "art_purification", "art_bloodlust", "art_thunder" -> 1.50F;
            default -> tier(id) == 4 ? 1.90F : 0.90F;
        };
    }

    public static float proficiencyMultiplier(int uses) { return 1.0F + effectPercent(uses) / 100.0F; }

    /** Damage tuning is independent of mana prices; reducing a cost must not also nerf its damage. */
    public static float damageInjectionMultiplier(String id) {
        return switch (id) {
            case "art_sword_aura", "art_renewal", "art_spirit_shield", "art_immobilize", "art_light_body",
                    "art_sword_pierce" -> 1.12F;
            case "art_golden_bell", "art_ice_spike", "art_spirit_gathering", "art_armor_break",
                    "art_sword_sweep", "art_sword_guard", "art_sword_flight" -> 1.30F;
            case "art_flame_wave", "art_flash_step", "art_purification", "art_bloodlust", "art_thunder" -> 1.48F;
            default -> tier(id) == 4 ? 1.84F : 1.12F;
        };
    }

    public static float damageFor(String id, double playerAttack, int uses, CultivationData data) {
        float base = damageFor(id, playerAttack, uses, cn.blockforge.generated.generatedmod.items.CultivationLogic.maxAura(data));
        return data == null ? base : base * data.acupoints().bonuses().multiplier(
                cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog.Stat.SKILL_DAMAGE_PCT);
    }

    public static float damageFor(String id, double playerAttack, int uses, float maxMana) {
        return (float) (playerAttack * artPowerMultiplier(id) + damageBase(id))
                * damageInjectionMultiplier(id) * proficiencyMultiplier(uses);
    }

    public static int currentDamage(String id, int uses) {
        return Math.round(damageBase(id) * artPowerMultiplier(id) * proficiencyMultiplier(uses));
    }

    public static int currentHealing(String id, int uses) {
        int base = tier(id) == 1 ? 8 : 24;
        return base + masteryLevel(uses) * 2;
    }

    public static int shieldBase(String id) {
        return switch (id) { case "art_spirit_shield" -> 20; case "art_golden_bell" -> 40;
            case "art_immovable_mountain" -> 72; default -> 0; };
    }
    public static float shieldHealthFraction(String id) {
        return switch (id) { case "art_spirit_shield" -> .08F; case "art_golden_bell" -> .12F;
            case "art_immovable_mountain" -> .18F; default -> 0.0F; };
    }
    /** Vanilla effects serialize/network the amplifier as one byte; never let it wrap on giant forms. */
    public static int shieldAmplifier(String id, float maximumHealth) {
        float amount=shieldBase(id)+Math.max(0.0F,maximumHealth)*shieldHealthFraction(id);
        return Math.max(0,Math.min(255,(int)Math.ceil(amount/4.0F)-1));
    }

    /** 返回当前熟练度等级实际对应的效果，供设置界面和秘籍 tooltip 使用。 */
    public static String currentEffect(String id, int uses) {
        if (cn.blockforge.generated.generatedmod.arts.SustainedSwordArts.supports(id))
            return cn.blockforge.generated.generatedmod.arts.SustainedSwordArts.description(id,uses);
        if (id.equals("art_sword_flight")) return "御剑召剑消耗" + fixedManaCost(id) + "点灵力 + 最大灵力" + percentageCost(id)
                + "%；飞行各阶段持续消耗灵力，遁虚另有进入和维持消耗；需要可御剑灵剑";
        if (SwordTechniqueManager.isSwordTechnique(id)) return "以当前灵剑配置施展本人已学剑诀，消耗玩家灵力";
        int level = masteryLevel(uses);
        return switch (type(id)) {
            case ATTACK -> "当前效果：" + attackProfile(id).description() + "；攻击×" + String.format(java.util.Locale.ROOT, "%.2f", artPowerMultiplier(id))
                    + " + 基础" + damageBase(id) + "，灵力注入×"
                    + String.format(java.util.Locale.ROOT, "%.2f", damageInjectionMultiplier(id))
                    + "，熟练度最高+30%";
            case HEALING -> "当前效果：恢复" + currentHealing(id, uses) + "点生命（基础"
                    + (tier(id) == 1 ? 8 : 24) + " + 熟练度等级×2）" + (id.equals("art_purification") ? "；清除自身负面状态" : "");
            case DEFENSE -> "当前效果：吸收护盾容量为" + shieldBase(id) + " + 最大生命"
                    + Math.round(shieldHealthFraction(id)*100) + "%（向上取整为4的倍数，上限1024）；持续"
                    + (shieldDurationTicks(id,uses) / 20) + "秒";
            case CONTROL -> id.equals("art_thunder") ? "当前效果：" + attackProfile(id).description() + "；造成强化闪电伤害并麻痹1.7至2.3秒"
                    : "当前效果：自身周围3格范围定身" + (2 + level / 2) + "秒";
            case BUFF -> "当前效果：" + switch (id) {
                case "art_light_body" -> "速度II";
                case "art_spirit_gathering" -> "生命恢复II";
                case "art_human_harmony" -> "力量III";
                default -> "力量II";
            } + "，持续" + (6 + level) + "秒";
            case SPECIAL -> id.startsWith("art_sword_")
                    ? "当前效果：御剑术式，需持有本命剑或处于御剑状态"
                    : "当前效果：向前闪现" + (effectPercent(uses) >= 30 ? 10 : 8) + "格";
        };
    }

    public static String extraEffect(String id, int uses) {
        return (uses>=100?"满熟练：":"满熟练后：") + switch (type(id)) {
            case ATTACK -> "熟练度伤害增幅达到30%";
            case DEFENSE -> "护盾持续" + (id.equals("art_immovable_mountain")?7:5) + "秒";
            case HEALING -> "恢复" + currentHealing(id,100) + "点生命";
            case CONTROL -> id.equals("art_thunder")?"强化集束落雷，麻痹1.7至2.3秒":"近身定身4秒";
            case BUFF -> "增益持续11秒";
            case SPECIAL -> id.equals("art_flash_step")?"移动距离提升至10格":"冷却缩短10%";
        };
    }

    public static String[] sortLearned(String[] learned) {
        return Arrays.stream(learned == null ? new String[0] : learned)
                .filter(ArtCatalog::isValid)
                .sorted(Comparator.comparingInt((String id) -> tier(id)).reversed()
                        .thenComparing(ArtCatalog::displayNameText))
                .toArray(String[]::new);
    }

    private static String displayNameText(String id) {
        return switch (id) {
            case "art_sword_aura" -> "剑气斩";
            case "art_renewal" -> "回春术";
            case "art_spirit_shield" -> "灵力护盾";
            case "art_immobilize" -> "定身咒";
            case "art_light_body" -> "轻身术";
            case "art_golden_bell" -> "金钟罩";
            case "art_ice_spike" -> "冰锥术";
            case "art_spirit_gathering" -> "聚灵术";
            case "art_armor_break" -> "破甲斩";
            case "art_flame_wave" -> "烈焰波";
            case "art_flash_step" -> "闪现";
            case "art_purification" -> "净化术";
            case "art_bloodlust" -> "嗜血术";
            case "art_thunder" -> "雷霆万钧";
            case "art_myriad_swords" -> "万剑诀";
            case "art_wanjian_guizong" -> "万剑归宗";
            case "art_jiuxiao_sword_rain" -> "九霄剑诀";
            case "art_immovable_mountain" -> "不动如山";
            case "art_human_harmony" -> "天人合一";
            case "art_five_elements" -> "五行灭世";
            case "art_sword_pierce" -> "飞星贯日诀";
            case "art_sword_sweep" -> "回风环刃诀";
            case "art_sword_guard" -> "六御金罡诀";
            case "art_sword_array" -> "太虚落剑诀";
            case "art_sword_flight" -> "御剑凌霄诀";
            default -> id;
        };
    }
}
