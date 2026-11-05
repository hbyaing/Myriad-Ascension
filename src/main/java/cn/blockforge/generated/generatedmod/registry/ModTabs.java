package cn.blockforge.generated.generatedmod.registry;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.WuXingItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** 五行仙途创造模式标签页注册。 */
public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GeneratedMod.MOD_ID);

    public enum Category { CULTIVATION, ARTS, HERBS, PILLS, ALCHEMY, REFINING, TALISMANS, FORMATIONS, MATERIALS, MISC }

    private static net.minecraft.resources.ResourceLocation previousTab;

    // Preserve the original registry ID for compatibility; it is now the focused cultivation tab.
    public static final RegistryObject<CreativeModeTab> WUXINGXIANTU_TAB = tab("wuxingxiantu", Category.CULTIVATION, () -> new ItemStack(cn.blockforge.generated.generatedmod.WuXingBlocks.MEDITATION_CUSHION_ITEM.get()));
    public static final RegistryObject<CreativeModeTab> ARTS = tab("arts", Category.ARTS, () -> new ItemStack(cn.blockforge.generated.generatedmod.arts.ImportedSwordArts.JIUXIAO_SWORD_RAIN.get()));
    public static final RegistryObject<CreativeModeTab> HERBS = tab("herbs", Category.HERBS, () -> new ItemStack(WuXingItems.SPIRIT_FERTILIZER.get()));
    public static final RegistryObject<CreativeModeTab> PILLS = tab("pills", Category.PILLS, () -> cn.blockforge.generated.generatedmod.alchemy.PillData.create(cn.blockforge.generated.generatedmod.alchemy.PillCatalog.GATHER_QI, cn.blockforge.generated.generatedmod.alchemy.PillData.Grade.PERFECT, cn.blockforge.generated.generatedmod.alchemy.PillData.Element.METAL, 0));
    public static final RegistryObject<CreativeModeTab> ALCHEMY = tab("alchemy", Category.ALCHEMY, () -> new ItemStack(cn.blockforge.generated.generatedmod.WuXingBlocks.ALCHEMY_FURNACE_ITEM.get()));
    public static final RegistryObject<CreativeModeTab> REFINING = tab("refining", Category.REFINING, () -> new ItemStack(cn.blockforge.generated.generatedmod.WuXingBlocks.REFINING_FORGE_CORE_ITEM.get()));
    public static final RegistryObject<CreativeModeTab> TALISMANS = tab("talismans", Category.TALISMANS, () -> new ItemStack(WuXingItems.TALISMAN_PAPER.get()));
    public static final RegistryObject<CreativeModeTab> FORMATIONS = tab("formations", Category.FORMATIONS, () -> new ItemStack(cn.blockforge.generated.generatedmod.formation.FormationBootstrap.ZHUXIAN_SWORD_CHART.get()));
    public static final RegistryObject<CreativeModeTab> MATERIALS = tab("materials", Category.MATERIALS, () -> new ItemStack(WuXingItems.SPIRIT_STONE_EXQUISITE.get()));
    public static final RegistryObject<CreativeModeTab> MISC = tab("misc", Category.MISC, () -> new ItemStack(cn.blockforge.generated.generatedmod.storage.StorageRegistry.BAG_ITEM.get()));

    private static RegistryObject<CreativeModeTab> tab(String id, Category category, java.util.function.Supplier<ItemStack> icon) {
        var preceding=previousTab;
        previousTab=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,id);
        return TABS.register(id, () -> {
            var builder=CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.wuxingxiantu." + category.name().toLowerCase(java.util.Locale.ROOT)))
                .icon(icon)
                .withBackgroundLocation(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,"textures/gui/creative/panel.png"))
                .withLabelColor(0x292A26)
                .withSlotColor(0x33C6A665)
                .displayItems((parameters, output) -> fillAll((stack, visibility) -> {
                    if (category(stack) == category) output.accept(stack, visibility);
                }));
            if(preceding!=null)builder.withTabsBefore(preceding);
            return builder.build();
        });
    }

    /** One authoritative classification per real stack, including NBT-bearing variants. */
    public static Category category(ItemStack stack) {
        var item=stack.getItem();
        String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        if(cn.blockforge.generated.generatedmod.alchemy.PillData.read(stack)!=null) return Category.PILLS;
        if(id.equals("creation_pill")) return Category.PILLS;
        if(item instanceof cn.blockforge.generated.generatedmod.items.ArtManualItem
                ||item instanceof cn.blockforge.generated.generatedmod.items.DivineManualItem
                ||item instanceof cn.blockforge.generated.generatedmod.items.HeartMethodItem
                ||id.equals("guanding_manual")||id.equals("wanfa_guizong")) return Category.ARTS;
        if(cn.blockforge.generated.generatedmod.plants.HerbAge.definition(stack)!=null
                ||(id.startsWith("peach_")&&!(item instanceof net.minecraft.world.item.TieredItem))||java.util.Set.of("spirit_soil","spirit_fertilizer","spirit_liquid_bucket","spirit_liquid_bottle","pill_ash").contains(id)) return Category.HERBS;
        if(item instanceof cn.blockforge.generated.generatedmod.alchemy.CompendiumItem
                ||id.equals("alchemy_formula")||id.startsWith("alchemy_furnace")||id.startsWith("fire_seed_")
                ||id.startsWith("exotic_fire_")||java.util.Set.of("v5_pill","sealed_alchemy_batch","waste_pill","alchemy_residue").contains(id)||id.equals("moon_bottle")||id.equals("empty_fire_seed_bottle")||id.equals("herb_processing_table")||id.equals("alchemy_research_table")
                ||id.equals("waste_pill")||id.startsWith("inner_core_")) return Category.ALCHEMY;
        if(id.equals("formation_carving_core")||id.equals("zhuxian_sword_chart")||id.equals("gathering_array")||id.equals("rune_stone")) return Category.FORMATIONS;
        if(id.startsWith("talisman")||java.util.Set.of("carving_knife","spirit_ink","cinnabar_ink","seal","engraving_platform","rubbing_platform","drawing_platform").contains(id)) return Category.TALISMANS;
        if(id.startsWith("artifact_")||id.startsWith("forge_aux_")||id.startsWith("refining_forge")
                ||java.util.Set.of("ming_ke_tai","jing_zhu_tai","xuanling_anvil","glazed_crystal","refine_seal","quench_essence","inscription_crystal").contains(id)
                ||item instanceof net.minecraft.world.item.TieredItem||item instanceof net.minecraft.world.item.ArmorItem
                ||item instanceof cn.blockforge.generated.generatedmod.formation.ChaosSwordItem
                ||item instanceof cn.blockforge.generated.generatedmod.items.HumanEmperorBannerItem) return Category.REFINING;
        if(java.util.Set.of("meditation_cushion","spirit_cushion","meditation_platform","spirit_platform","spirit_bath","insight","aura_crystal","experience_crystal","hualing_pei","cave_token").contains(id)) return Category.CULTIVATION;
        if(id.startsWith("spirit_stone")||id.startsWith("spirit_vein_ore")||id.startsWith("spirit_coal")
                ||id.startsWith("eternal_spirit_coal")||id.equals("spirit_ore")||id.equals("dark_iron_block")||id.equals("thunderstruck_wood")
                ||java.util.Set.of("raw_dark_iron","dark_iron","azure_steel","tiangang_steel","five_essence","insect_carapace_fragment","spirit_devourer_sac","beast_blood","beast_hide").contains(id)
                ||id.matches("(low|mid|high|perfect)_(gold_marrow|wood_seed|water_pearl|fire_flame|earth_core)")) return Category.MATERIALS;
        return Category.MISC;
    }

    private static void fillAll(CreativeModeTab.Output output) {
                        var groups=new java.util.HashMap<RegistryObject<net.minecraft.world.item.Item>,Integer>();
                        WuXingItems.ITEMS.getEntries().stream()
                                .sorted(java.util.Comparator.comparingInt(entry->groups.computeIfAbsent(entry,ModTabs::displayGroup))).forEach(item -> {
                            if (WuXingItems.BOSS_TEST_EGGS.contains(item)||item==WuXingItems.V5_PILL) return;
                            ItemStack stack = new ItemStack(item.get());
                            if(cn.blockforge.generated.generatedmod.WuXingItemSources.classify(stack)
                                    ==cn.blockforge.generated.generatedmod.WuXingItemSources.Classification.INTERNAL)return;
                            var pill = cn.blockforge.generated.generatedmod.alchemy.PillData.read(stack);
                            if (pill != null) return;
                            if (stack.getItem() instanceof cn.blockforge.generated.generatedmod.items.FireSeedItem fire && fire.tier() > 0) {
                                output.accept(cn.blockforge.generated.generatedmod.fire.FireBottleData.fresh(fire.tier()));
                                return;
                            }
                            if (item != WuXingItems.ALCHEMY_FORMULA && item != WuXingItems.TALISMAN) {
                                var herb=cn.blockforge.generated.generatedmod.plants.HerbAge.definition(stack);
                                if(herb!=null)output.accept(cn.blockforge.generated.generatedmod.plants.HerbAge.creativeTemplate(stack));
                                else output.accept(stack);
                            }
                        });
                        for (var pill : cn.blockforge.generated.generatedmod.alchemy.PillCatalog.values()) {
                            output.accept(cn.blockforge.generated.generatedmod.alchemy.FormulaItem.create(pill));
                            int elements=pill==cn.blockforge.generated.generatedmod.alchemy.PillCatalog.ROOT?5:1;
                            int realms=pill==cn.blockforge.generated.generatedmod.alchemy.PillCatalog.CULTIVATION?9:1;
                            for(int element=0;element<elements;element++)for(int realm=0;realm<realms;realm++)output.accept(cn.blockforge.generated.generatedmod.alchemy.PillData.create(pill,cn.blockforge.generated.generatedmod.alchemy.PillData.Grade.LOW,cn.blockforge.generated.generatedmod.alchemy.PillData.Element.values()[element],realm));
                        }
                        cn.blockforge.generated.generatedmod.arts.ImportedSwordArts.ITEMS.getEntries().forEach(item->output.accept(item.get()));
                        output.accept(cn.blockforge.generated.generatedmod.formation.carving.CarvingRegistry.CORE_ITEM.get());
                        cn.blockforge.generated.generatedmod.formation.FormationBootstrap.items().forEach(item->output.accept(item.get()));
                        cn.blockforge.generated.generatedmod.artifacts.ArtifactBootstrap.items().forEach(item->output.accept(item.get()));
                        output.accept(cn.blockforge.generated.generatedmod.storage.StorageRegistry.BAG_ITEM.get());
                        WuXingItems.BOSS_TEST_EGGS.forEach(item -> output.accept(item.get()));
    }

    /** Stable sorting keeps the existing order within each functional group. */
    private static int displayGroup(RegistryObject<net.minecraft.world.item.Item> entry) {
        var item = entry.get();
        if (entry == WuXingItems.INSIGHT) return 3;
        if (entry == WuXingItems.GUANDING_MANUAL) return 4;
        if (item instanceof cn.blockforge.generated.generatedmod.items.ArtManualItem) return 5;
        if (item instanceof cn.blockforge.generated.generatedmod.items.DivineManualItem) return 6;
        if (item instanceof cn.blockforge.generated.generatedmod.items.HeartMethodItem) return 7;
        if (item instanceof cn.blockforge.generated.generatedmod.alchemy.CompendiumItem) return 9;
        if (item instanceof net.minecraft.world.item.BlockItem) return 1;
        if (item instanceof net.minecraft.world.item.TieredItem
                || item instanceof net.minecraft.world.item.ArmorItem
                || item instanceof cn.blockforge.generated.generatedmod.items.HumanEmperorBannerItem) return 2;
        if (item instanceof cn.blockforge.generated.generatedmod.items.SoulBindingPearlItem
                || item instanceof cn.blockforge.generated.generatedmod.items.SoulCapturePearlItem
                || item instanceof cn.blockforge.generated.generatedmod.items.SoulUtilityPearlItem
                || item instanceof cn.blockforge.generated.generatedmod.items.SoulEnergyPearlItem) return 8;
        String id = entry.getId().getPath();
        if(id.startsWith("spirit_stone_"))return 0;
        for(var materials:java.util.List.of(WuXingItems.GOLD_MATERIALS,WuXingItems.WOOD_MATERIALS,
                WuXingItems.WATER_MATERIALS,WuXingItems.FIRE_MATERIALS,WuXingItems.EARTH_MATERIALS))
            for(var material:materials)if(entry==material)return 0;
        if (java.util.Set.of("pill_ash","spirit_stone", "spirit_coal", "raw_dark_iron", "dark_iron",
                        "azure_steel", "tiangang_steel", "five_essence", "insect_carapace_fragment",
                        "spirit_devourer_sac", "peach_blossom", "peach_petals","soul_pearl","broken_soul_pearl").contains(id)) return 0;
        return 8;
    }

    private ModTabs() {}
}
