package cn.blockforge.generated.generatedmod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;

/**
 * 物品来源分类：明确区分“正常生存可获得”“系统内部”“创造/测试专属”。
 *
 * <p>审计口径（README_五行仙途_源码审计版.md）：</p>
 * <ul>
 *   <li>✅ SURVIVAL —— 正常生存可获得（有配方/战利品/村民等来源）。</li>
 *   <li>🔒 INTERNAL —— 系统内部物品（结构部件、渲染载体等，不作为玩法产出）。</li>
 *   <li>🧪 CREATIVE_TEST —— 创造/测试专属（万法归宗、创造丹、创造魂珠、刷怪蛋等）：
 *       永远不为它们补生存配方，它们的存在不是“断链”。</li>
 *   <li>❌ 本应生存获取但断链 —— 审计发现项，属后续批次修复目标。</li>
 * </ul>
 *
 * <p>CREATIVE_TEST 分类同时以 Forge 物品标签
 * {@code #wuxingxiantu:creative_test} 表达（数据可见），本类提供代码侧的
 * 权威查询入口，供审计、界面提示与后续检查使用。</p>
 */
public final class WuXingItemSources {
    public enum Classification { SURVIVAL, INTERNAL, CREATIVE_TEST, UNREVIEWED }

    /** 创造/测试专属物品 ID（与 data/wuxingxiantu/tags/items/creative_test.json 保持一致）。 */
    private static final Set<String> CREATIVE_TEST = Set.of(
            "dev_test_robe",
            "wanfa_guizong",
            "creation_pill",
            "creative_myriad_soul_pearl",
            "creative_soul_energy_pearl",
            "muyan_spawn_egg", "ermei_spawn_egg", "bichen_spawn_egg", "shetao_spawn_egg", "shenyin_spawn_egg",
            "yihuo_spawn_egg", "pengju_spawn_egg", "pengzhi_spawn_egg", "pengjiao_spawn_egg",
            "heart_demon_spawn_egg", "black_boar_spawn_egg", "black_boar_king_spawn_egg",
            "exotic_fire_red_flow", "exotic_fire_molten_gold", "exotic_fire_cold_bone",
            "exotic_fire_azure_wood", "exotic_fire_cobalt_sea", "exotic_fire_violet_thunder",
            "exotic_fire_sun_saint", "exotic_fire_nether_void", "exotic_fire_nirvana");

    /** 系统内部物品 ID：仅由结构、实体或渲染管线使用。 */
    private static final Set<String> INTERNAL = Set.of(
            "human_emperor_banner_stand",
            "spirit_liquid",
            "alchemy_furnace_part",
            "refining_forge_corner");

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> CREATIVE_TEST_TAG =
            net.minecraft.tags.ItemTags.create(new ResourceLocation(GeneratedMod.MOD_ID, "creative_test"));

    private WuXingItemSources() {}

    public static Classification classify(ItemStack stack) {
        if (stack.isEmpty()) return Classification.INTERNAL;
        return classify(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static Classification classify(ResourceLocation id) {
        if (id == null || !GeneratedMod.MOD_ID.equals(id.getNamespace())) return Classification.SURVIVAL;
        if (CREATIVE_TEST.contains(id.getPath())) return Classification.CREATIVE_TEST;
        if (INTERNAL.contains(id.getPath())) return Classification.INTERNAL;
        return AcquisitionCatalog.sources(id).isEmpty() ? Classification.UNREVIEWED : Classification.SURVIVAL;
    }

    /** 是否创造/测试专属（标签优先，代码集合作为数据驱动加载前的兜底）。 */
    public static boolean isCreativeTest(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(CREATIVE_TEST_TAG) || CREATIVE_TEST.contains(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()));
    }

    /** 只读视图，供审计脚本与文档生成使用。 */
    public static Map<String, Classification> auditTable() {
        var result = new java.util.TreeMap<String, Classification>();
        for (var id : BuiltInRegistries.ITEM.keySet())
            if (GeneratedMod.MOD_ID.equals(id.getNamespace())) result.put(id.toString(), classify(id));
        for (String path : CREATIVE_TEST) {
            result.put(GeneratedMod.MOD_ID + ":" + path, Classification.CREATIVE_TEST);
        }
        for (String path : INTERNAL) {
            result.put(GeneratedMod.MOD_ID + ":" + path, Classification.INTERNAL);
        }
        return java.util.Collections.unmodifiableMap(result);
    }
}
