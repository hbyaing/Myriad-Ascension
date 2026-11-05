package cn.blockforge.generated.generatedmod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import cn.blockforge.generated.generatedmod.registry.ModFluids;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 五行仙途方块注册。所有方块和对应方块物品都采用延迟构造。 */
public final class WuXingBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, GeneratedMod.MOD_ID);
    public static final RegistryObject<Block> INSCRIPTION_TABLE=BLOCKS.register("ming_ke_tai",()->new cn.blockforge.generated.generatedmod.crafting.EquipmentWorkstationBlock(true));
    public static final RegistryObject<Item> INSCRIPTION_TABLE_ITEM=blockItem("ming_ke_tai",INSCRIPTION_TABLE,64);
    public static final RegistryObject<Block> REFORGE_TABLE=BLOCKS.register("jing_zhu_tai",()->new cn.blockforge.generated.generatedmod.crafting.EquipmentWorkstationBlock(false));
    public static final RegistryObject<Item> REFORGE_TABLE_ITEM=blockItem("jing_zhu_tai",REFORGE_TABLE,64);

    public static final RegistryObject<Block> XUANLING_ANVIL = BLOCKS.register("xuanling_anvil", XuanlingAnvilBlock::new);
    public static final RegistryObject<Item> XUANLING_ANVIL_ITEM = blockItem("xuanling_anvil", XUANLING_ANVIL, 64);
    public static final RegistryObject<Block> MOON_BOTTLE=BLOCKS.register("moon_bottle",cn.blockforge.generated.generatedmod.alchemy.inputs.MoonBottleBlock::new);
    public static final RegistryObject<Item> MOON_BOTTLE_ITEM=blockItem("moon_bottle",MOON_BOTTLE,1);

    public static final RegistryObject<Block> ALCHEMY_RESEARCH_TABLE=BLOCKS.register("alchemy_research_table",cn.blockforge.generated.generatedmod.alchemy.research.ResearchTableBlock::new);
    public static final RegistryObject<Item> ALCHEMY_RESEARCH_TABLE_ITEM=blockItem("alchemy_research_table",ALCHEMY_RESEARCH_TABLE,64);
    public static final RegistryObject<Block> HERB_PROCESSING_TABLE = BLOCKS.register("herb_processing_table",HerbProcessingTableBlock::new);
    public static final RegistryObject<Item> HERB_PROCESSING_TABLE_ITEM = blockItem("herb_processing_table",HERB_PROCESSING_TABLE,64);

    public static final RegistryObject<Block> ALCHEMY_FURNACE_PART = BLOCKS.register("alchemy_furnace_part", AlchemyFurnacePartBlock::new);
    public static final RegistryObject<Block> EXOTIC_FIRE_SOURCE = BLOCKS.register("exotic_fire_source",
            cn.blockforge.generated.generatedmod.fire.ExoticFireSourceBlock::new);

    public static final RegistryObject<Block> SPIRIT_ORE = BLOCKS.register(
            "spirit_ore", () -> new Block(oreProperties(0)));
    public static final RegistryObject<Block> SPIRIT_VEIN_ORE = BLOCKS.register(
            "spirit_vein_ore", () -> new Block(oreProperties(5)));
    public static final RegistryObject<Block> SPIRIT_VEIN_ORE_MIDDLE = BLOCKS.register("spirit_vein_ore_middle", () -> new Block(oreProperties(7)));
    public static final RegistryObject<Item> SPIRIT_VEIN_ORE_MIDDLE_ITEM = blockItem("spirit_vein_ore_middle", SPIRIT_VEIN_ORE_MIDDLE, 64);
    public static final RegistryObject<Block> SPIRIT_VEIN_ORE_SUPERIOR = BLOCKS.register("spirit_vein_ore_superior", () -> new Block(oreProperties(11)));
    public static final RegistryObject<Item> SPIRIT_VEIN_ORE_SUPERIOR_ITEM = blockItem("spirit_vein_ore_superior", SPIRIT_VEIN_ORE_SUPERIOR, 64);
    public static final RegistryObject<Block> SPIRIT_VEIN_ORE_EXQUISITE = BLOCKS.register("spirit_vein_ore_exquisite", () -> new Block(oreProperties(15)));
    public static final RegistryObject<Item> SPIRIT_VEIN_ORE_EXQUISITE_ITEM = blockItem("spirit_vein_ore_exquisite", SPIRIT_VEIN_ORE_EXQUISITE, 64);
    public static final RegistryObject<Block> SPIRIT_COAL_ORE = BLOCKS.register(
            "spirit_coal_ore", () -> new Block(oreProperties(0)));
    public static final RegistryObject<Block> SPIRIT_COAL_BLOCK = BLOCKS.register(
            "spirit_coal_block", () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()));
    /** 永燃灵煤块：使用钻石级镐可挖掘并掉落自身；作为方块展示时不会充当燃料。 */
    public static final RegistryObject<Block> ETERNAL_SPIRIT_COAL_BLOCK = BLOCKS.register(
            "eternal_spirit_coal_block", () -> new EternalSpiritCoalBlock(BlockBehaviour.Properties.of()
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 10)));
    /** 兼容旧代码的原版火焰引用；世界点火仍使用原版 FireBlock。 */
    public static final RegistryObject<Block> ETERNAL_SPIRIT_FIRE = RegistryObject.create(
            new net.minecraft.resources.ResourceLocation("minecraft", "fire"), ForgeRegistries.BLOCKS);

    /** 九阶异火的正式方块注册；外观参考 Spiritfire，实体附魔点燃仍由服务端效果链处理。 */
    public static final RegistryObject<Block> EXOTIC_FIRE_RED_FLOW = exoticFire("exotic_fire_red_flow", 1);
    public static final RegistryObject<Block> EXOTIC_FIRE_MOLTEN_GOLD = exoticFire("exotic_fire_molten_gold", 2);
    public static final RegistryObject<Block> EXOTIC_FIRE_COLD_BONE = exoticFire("exotic_fire_cold_bone", 3);
    public static final RegistryObject<Block> EXOTIC_FIRE_AZURE_WOOD = exoticFire("exotic_fire_azure_wood", 4);
    public static final RegistryObject<Block> EXOTIC_FIRE_COBALT_SEA = exoticFire("exotic_fire_cobalt_sea", 5);
    public static final RegistryObject<Block> EXOTIC_FIRE_VIOLET_THUNDER = exoticFire("exotic_fire_violet_thunder", 6);
    public static final RegistryObject<Block> EXOTIC_FIRE_SUN_SAINT = exoticFire("exotic_fire_sun_saint", 7);
    public static final RegistryObject<Block> EXOTIC_FIRE_NETHER_VOID = exoticFire("exotic_fire_nether_void", 8);
    public static final RegistryObject<Block> EXOTIC_FIRE_NIRVANA = exoticFire("exotic_fire_nirvana", 9);

    @SuppressWarnings("unchecked")
    public static final RegistryObject<Block>[] EXOTIC_FIRES = new RegistryObject[]{
            EXOTIC_FIRE_RED_FLOW, EXOTIC_FIRE_MOLTEN_GOLD, EXOTIC_FIRE_COLD_BONE,
            EXOTIC_FIRE_AZURE_WOOD, EXOTIC_FIRE_COBALT_SEA, EXOTIC_FIRE_VIOLET_THUNDER,
            EXOTIC_FIRE_SUN_SAINT, EXOTIC_FIRE_NETHER_VOID, EXOTIC_FIRE_NIRVANA
    };

    public static final RegistryObject<Block> SPIRIT_STONE_BLOCK = BLOCKS.register(
            "spirit_stone_block", () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DARK_IRON_BLOCK = BLOCKS.register(
            "dark_iron_block", () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()));
    /** 灵液只注册一个可放置方块；流体内部仍保留 source/flowing 两种状态。 */
    public static final RegistryObject<Block> SPIRIT_LIQUID = BLOCKS.register(
            "spirit_liquid", WuXingBlocks::spiritLiquidBlock);
    /** 灵壤：唯一品级的修仙灵植培养土。下品/中品/上品/极品灵壤不存在，也不会再加。 */
    public static final RegistryObject<Block> SPIRIT_SOIL = BLOCKS.register(
            "spirit_soil", () -> new cn.blockforge.generated.generatedmod.blocks.SpiritSoilBlock(
                    BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.DIRT)
                            .strength(0.5F).sound(net.minecraft.world.level.block.SoundType.GRAVEL)
                            .lightLevel(state -> 1)));
    // 龙鳞草与发光蓝蘑菇的注册已迁移到 SpiritPlantRegistry（保留原注册 ID，行为重做）。

    public static final RegistryObject<Block> ALCHEMY_FURNACE_LOW = furnace(
            "alchemy_furnace_low", 1);
    public static final RegistryObject<Block> ALCHEMY_FURNACE_MID = furnace(
            "alchemy_furnace_mid", 2);
    public static final RegistryObject<Block> ALCHEMY_FURNACE_HIGH = furnace(
            "alchemy_furnace_high", 3);
    public static final RegistryObject<Block> ALCHEMY_FURNACE_PERFECT = furnace(
            "alchemy_furnace_perfect", 4);
    public static final RegistryObject<Block> ALCHEMY_FURNACE_IMMORTAL = furnace(
            "alchemy_furnace_immortal", 5);
    /** 唯一的凡品炼丹炉核心。 */
    public static final RegistryObject<Block> ALCHEMY_FURNACE = furnace(
            "alchemy_furnace", 0);

    public static final RegistryObject<Block> SPIRIT_PLATFORM = BLOCKS.register(
            "spirit_platform", () -> new SpiritPlatformBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> 7)));
    public static final RegistryObject<Block> GATHERING_ARRAY = cultivation(
            "gathering_array", CultivationBlock.Kind.ARRAY, 7);
    public static final RegistryObject<Block> CAVE_TOKEN = BLOCKS.register(
            "cave_token", () -> new CaveTokenBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.5F)
                            .sound(net.minecraft.world.level.block.SoundType.STONE)
                            .noCollission()
                            .noOcclusion()
                            .lightLevel(state -> 7)));
    public static final RegistryObject<Block> MEDITATION_CUSHION = cultivation(
            "meditation_cushion", CultivationBlock.Kind.CUSHION, 0);
    public static final RegistryObject<Block> SPIRIT_CUSHION = cultivation(
            "spirit_cushion", CultivationBlock.Kind.SPIRIT_CUSHION, 3);
    public static final RegistryObject<Block> MEDITATION_PLATFORM = cultivation(
            "meditation_platform", CultivationBlock.Kind.PLATFORM, 5);
    public static final RegistryObject<Block> PEACH_WOOD = BLOCKS.register(
            "peach_wood", () -> new RotatedPillarBlock(woodProperties()));
    public static final RegistryObject<Block> THUNDERSTRUCK_WOOD = BLOCKS.register(
            "thunderstruck_wood", () -> new cn.blockforge.generated.generatedmod.blocks.ThunderstruckWoodBlock(BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK)
                    .strength(2.0F)
                    .sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final RegistryObject<Item> THUNDERSTRUCK_WOOD_ITEM =
            blockItem("thunderstruck_wood", THUNDERSTRUCK_WOOD, 64);
    public static final RegistryObject<Block> PEACH_LEAVES = BLOCKS.register(
            "peach_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.of().strength(0.2F)
                    .sound(net.minecraft.world.level.block.SoundType.GRASS).noOcclusion().randomTicks()));
    public static final RegistryObject<Block> PEACH_SAPLING = BLOCKS.register(
            "peach_sapling", () -> new SaplingBlock(new PeachTreeGrower(),
                    BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak()
                            .sound(net.minecraft.world.level.block.SoundType.GRASS)));
    public static final RegistryObject<Block> PEACH_PLANKS = BLOCKS.register(
            "peach_planks", () -> new Block(woodProperties()));
    public static final RegistryObject<Block> PEACH_SLAB = BLOCKS.register(
            "peach_slab", () -> new SlabBlock(woodProperties()));
    public static final RegistryObject<Block> PEACH_STAIRS = BLOCKS.register(
            "peach_stairs", () -> new StairBlock(() -> PEACH_PLANKS.get().defaultBlockState(), woodProperties()));
    public static final RegistryObject<Block> PEACH_FENCE = BLOCKS.register(
            "peach_fence", () -> new FenceBlock(woodProperties()));
    public static final RegistryObject<Block> PEACH_FENCE_GATE = BLOCKS.register(
            "peach_fence_gate", () -> new FenceGateBlock(woodProperties(), WoodType.CHERRY));
    public static final RegistryObject<Block> PEACH_DOOR = BLOCKS.register(
            "peach_door", () -> new DoorBlock(woodProperties().noOcclusion(), BlockSetType.CHERRY));
    public static final RegistryObject<Block> PEACH_TRAPDOOR = BLOCKS.register(
            "peach_trapdoor", () -> new TrapDoorBlock(woodProperties().noOcclusion(), BlockSetType.CHERRY));
    public static final RegistryObject<Block> PEACH_PRESSURE_PLATE = BLOCKS.register(
            "peach_pressure_plate", () -> new PressurePlateBlock(
                    PressurePlateBlock.Sensitivity.EVERYTHING, woodProperties(), BlockSetType.CHERRY));
    public static final RegistryObject<Block> PEACH_BUTTON = BLOCKS.register(
            "peach_button", () -> new ButtonBlock(woodProperties().noOcclusion(), BlockSetType.CHERRY, 30, true));
    /** 唯一的炼器台核心注册，避免旧铸剑台 ID 产生第二个核心。 */
    public static final RegistryObject<Block> REFINING_FORGE_CORE = BLOCKS.register(
            "refining_forge_core", WuXingBlocks::refiningForgeCore);
    public static final RegistryObject<Block> RUNE_STONE = BLOCKS.register(
            "rune_stone", WuXingBlocks::runeStone);
    /** 结构成型后替换四角磨制黑石砖台阶的外观方块；掉落物回归原版台阶。 */
    public static final RegistryObject<Block> REFINING_FORGE_CORNER = BLOCKS.register(
            "refining_forge_corner", () -> new RefiningForgeCornerBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(RefiningForgeCornerBlock.LIT) ? 12 : 5)));
    public static final RegistryObject<Block> SOUL_REFINING_FURNACE = BLOCKS.register(
            "soul_refining_furnace", () -> new SoulFurnaceBlock(SoulFurnaceBlockEntity.Kind.SOUL_REFINING));
    public static final RegistryObject<Block> SPIRIT_DISMANTLING_FURNACE = BLOCKS.register(
            "spirit_dismantling_furnace", () -> new SoulFurnaceBlock(SoulFurnaceBlockEntity.Kind.SPIRIT_DISMANTLING));
    public static final RegistryObject<Block> SOUL_HEALING_ROOM = BLOCKS.register(
            "soul_healing_room", () -> new SoulHealingRoomBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F, 3.5F).requiresCorrectToolForDrops()
                    .sound(net.minecraft.world.level.block.SoundType.SCULK)));
    /** 人皇幡插地展示座：无方块模型，由方块实体复用完整 3D 物品模型渲染。 */
    public static final RegistryObject<Block> HUMAN_EMPEROR_BANNER_STAND = BLOCKS.register(
            "human_emperor_banner_stand", () -> new HumanEmperorBannerStandBlock(BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK)
                    .strength(1.8F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                    .noOcclusion()
                    .noLootTable()));
    /** 人皇幡杆身延伸格：隐形、无碰撞，只为把插地幡的可交互范围从底座延长到整根杆。 */
    public static final RegistryObject<Block> HUMAN_EMPEROR_BANNER_POLE = BLOCKS.register(
            "human_emperor_banner_pole", () -> new HumanEmperorBannerPoleBlock(BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK)
                    .strength(1.8F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()));

    /** 承华台：可承载物品的基座，兼作炼器台的素材入口（外观为灰石托盘+铜足）。 */
    public static final RegistryObject<Block> CHENGHUA_PEDESTAL = BLOCKS.register(
            "chenghua_pedestal", () -> new ChenghuaPedestalBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(net.minecraft.world.level.block.SoundType.STONE)
                    .noOcclusion()));

    // ===== 符箓系统 =====

    /** 镌印台：放入刻刀与金锭，左侧描绘符文，成功产出印章。 */
    public static final RegistryObject<Block> ENGRAVING_PLATFORM = BLOCKS.register(
            "engraving_platform", () -> new TalismanPlatformBlock(
                    TalismanPlatformBlockEntity.Kind.ENGRAVE, platformProperties()));
    /** 拓印台：印章 + 符纸 + 朱砂墨，批量拓印符咒。 */
    public static final RegistryObject<Block> RUBBING_PLATFORM = BLOCKS.register(
            "rubbing_platform", () -> new TalismanPlatformBlock(
                    TalismanPlatformBlockEntity.Kind.RUB, platformProperties()));
    /** 绘制台：直接放入符纸与朱砂墨，一次描红生成一张符咒。 */
    public static final RegistryObject<Block> DRAWING_PLATFORM = BLOCKS.register(
            "drawing_platform", () -> new TalismanPlatformBlock(
                    TalismanPlatformBlockEntity.Kind.DRAW, platformProperties()));

    public static final RegistryObject<Item> ENGRAVING_PLATFORM_ITEM = blockItem(
            "engraving_platform", ENGRAVING_PLATFORM, 64);
    public static final RegistryObject<Item> RUBBING_PLATFORM_ITEM = blockItem(
            "rubbing_platform", RUBBING_PLATFORM, 64);
    public static final RegistryObject<Item> DRAWING_PLATFORM_ITEM = blockItem(
            "drawing_platform", DRAWING_PLATFORM, 64);

    public static final RegistryObject<Item> SPIRIT_ORE_ITEM = blockItem(
            "spirit_ore", SPIRIT_ORE, 64);
    public static final RegistryObject<Item> SPIRIT_VEIN_ORE_ITEM = blockItem(
            "spirit_vein_ore", SPIRIT_VEIN_ORE, 64);
    public static final RegistryObject<Item> SPIRIT_COAL_ORE_ITEM = blockItem(
            "spirit_coal_ore", SPIRIT_COAL_ORE, 64);
    public static final RegistryObject<Item> SPIRIT_COAL_BLOCK_ITEM = blockItem(
            "spirit_coal_block", SPIRIT_COAL_BLOCK, 64);
    public static final RegistryObject<Item> ETERNAL_SPIRIT_COAL_BLOCK_ITEM = WuXingItems.ITEMS.register(
            "eternal_spirit_coal_block", () -> new cn.blockforge.generated.generatedmod.items
                    .EternalSpiritCoalBlockItem(ETERNAL_SPIRIT_COAL_BLOCK.get(), new Item.Properties().rarity(
                    net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> EXOTIC_FIRE_RED_FLOW_ITEM = blockItem(
            "exotic_fire_red_flow", EXOTIC_FIRE_RED_FLOW, 64);
    public static final RegistryObject<Item> EXOTIC_FIRE_MOLTEN_GOLD_ITEM = blockItem(
            "exotic_fire_molten_gold", EXOTIC_FIRE_MOLTEN_GOLD, 64);
    public static final RegistryObject<Item> EXOTIC_FIRE_COLD_BONE_ITEM = blockItem(
            "exotic_fire_cold_bone", EXOTIC_FIRE_COLD_BONE, 64);
    public static final RegistryObject<Item> EXOTIC_FIRE_AZURE_WOOD_ITEM = blockItem(
            "exotic_fire_azure_wood", EXOTIC_FIRE_AZURE_WOOD, 64);
    public static final RegistryObject<Item> EXOTIC_FIRE_COBALT_SEA_ITEM = blockItem(
            "exotic_fire_cobalt_sea", EXOTIC_FIRE_COBALT_SEA, 64);
    public static final RegistryObject<Item> EXOTIC_FIRE_VIOLET_THUNDER_ITEM = blockItem(
            "exotic_fire_violet_thunder", EXOTIC_FIRE_VIOLET_THUNDER, 64);
    public static final RegistryObject<Item> EXOTIC_FIRE_SUN_SAINT_ITEM = blockItem(
            "exotic_fire_sun_saint", EXOTIC_FIRE_SUN_SAINT, 64);
    public static final RegistryObject<Item> EXOTIC_FIRE_NETHER_VOID_ITEM = blockItem(
            "exotic_fire_nether_void", EXOTIC_FIRE_NETHER_VOID, 64);
    public static final RegistryObject<Item> EXOTIC_FIRE_NIRVANA_ITEM = blockItem(
            "exotic_fire_nirvana", EXOTIC_FIRE_NIRVANA, 64);

    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] EXOTIC_FIRE_ITEMS = new RegistryObject[]{
            EXOTIC_FIRE_RED_FLOW_ITEM, EXOTIC_FIRE_MOLTEN_GOLD_ITEM, EXOTIC_FIRE_COLD_BONE_ITEM,
            EXOTIC_FIRE_AZURE_WOOD_ITEM, EXOTIC_FIRE_COBALT_SEA_ITEM, EXOTIC_FIRE_VIOLET_THUNDER_ITEM,
            EXOTIC_FIRE_SUN_SAINT_ITEM, EXOTIC_FIRE_NETHER_VOID_ITEM, EXOTIC_FIRE_NIRVANA_ITEM
    };

    public static final RegistryObject<Item> SPIRIT_STONE_BLOCK_ITEM = blockItem(
            "spirit_stone_block", SPIRIT_STONE_BLOCK, 64);
    public static final RegistryObject<Item> DARK_IRON_BLOCK_ITEM = blockItem(
            "dark_iron_block", DARK_IRON_BLOCK, 64);
    public static final RegistryObject<Item> SPIRIT_LIQUID_ITEM = blockItem(
            "spirit_liquid", SPIRIT_LIQUID, 64);
    public static final RegistryObject<Item> SPIRIT_SOIL_ITEM = blockItem(
            "spirit_soil", SPIRIT_SOIL, 64);
    public static final RegistryObject<Item> ALCHEMY_FURNACE_LOW_ITEM = blockItem(
            "alchemy_furnace_low", ALCHEMY_FURNACE_LOW, 64);
    public static final RegistryObject<Item> ALCHEMY_FURNACE_MID_ITEM = blockItem(
            "alchemy_furnace_mid", ALCHEMY_FURNACE_MID, 64);
    public static final RegistryObject<Item> ALCHEMY_FURNACE_HIGH_ITEM = blockItem(
            "alchemy_furnace_high", ALCHEMY_FURNACE_HIGH, 64);
    public static final RegistryObject<Item> ALCHEMY_FURNACE_PERFECT_ITEM = blockItem(
            "alchemy_furnace_perfect", ALCHEMY_FURNACE_PERFECT, 64);
    public static final RegistryObject<Item> ALCHEMY_FURNACE_IMMORTAL_ITEM = blockItem(
            "alchemy_furnace_immortal", ALCHEMY_FURNACE_IMMORTAL, 64);
    public static final RegistryObject<Item> ALCHEMY_FURNACE_ITEM = blockItem(
            "alchemy_furnace", ALCHEMY_FURNACE, 64);
    public static final RegistryObject<Item> SPIRIT_PLATFORM_ITEM = blockItem(
            "spirit_platform", SPIRIT_PLATFORM, 64);
    public static final RegistryObject<Item> CHENGHUA_PEDESTAL_ITEM = blockItem(
            "chenghua_pedestal", CHENGHUA_PEDESTAL, 64);
    public static final RegistryObject<Item> GATHERING_ARRAY_ITEM = blockItem(
            "gathering_array", GATHERING_ARRAY, 64);
    public static final RegistryObject<Item> CAVE_TOKEN_ITEM = blockItem(
            "cave_token", CAVE_TOKEN, 1);
    public static final RegistryObject<Item> MEDITATION_CUSHION_ITEM = blockItem(
            "meditation_cushion", MEDITATION_CUSHION, 1);
    public static final RegistryObject<Item> SPIRIT_CUSHION_ITEM = blockItem(
            "spirit_cushion", SPIRIT_CUSHION, 1);
    public static final RegistryObject<Item> MEDITATION_PLATFORM_ITEM = blockItem(
            "meditation_platform", MEDITATION_PLATFORM, 1);
    public static final RegistryObject<Item> PEACH_WOOD_ITEM = blockItem(
            "peach_wood", PEACH_WOOD, 64);
    public static final RegistryObject<Item> PEACH_LEAVES_ITEM = blockItem(
            "peach_leaves", PEACH_LEAVES, 64);
    public static final RegistryObject<Item> PEACH_SAPLING_ITEM = blockItem(
            "peach_sapling", PEACH_SAPLING, 64);
    public static final RegistryObject<Item> PEACH_PLANKS_ITEM = blockItem(
            "peach_planks", PEACH_PLANKS, 64);
    public static final RegistryObject<Item> PEACH_SLAB_ITEM = blockItem(
            "peach_slab", PEACH_SLAB, 64);
    public static final RegistryObject<Item> PEACH_STAIRS_ITEM = blockItem(
            "peach_stairs", PEACH_STAIRS, 64);
    public static final RegistryObject<Item> PEACH_FENCE_ITEM = blockItem(
            "peach_fence", PEACH_FENCE, 64);
    public static final RegistryObject<Item> PEACH_FENCE_GATE_ITEM = blockItem(
            "peach_fence_gate", PEACH_FENCE_GATE, 64);
    public static final RegistryObject<Item> PEACH_DOOR_ITEM = WuXingItems.ITEMS.register(
            "peach_door", () -> new DoubleHighBlockItem(PEACH_DOOR.get(), new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> PEACH_TRAPDOOR_ITEM = blockItem(
            "peach_trapdoor", PEACH_TRAPDOOR, 64);
    public static final RegistryObject<Item> PEACH_PRESSURE_PLATE_ITEM = blockItem(
            "peach_pressure_plate", PEACH_PRESSURE_PLATE, 64);
    public static final RegistryObject<Item> PEACH_BUTTON_ITEM = blockItem(
            "peach_button", PEACH_BUTTON, 64);
    public static final RegistryObject<Item> REFINING_FORGE_CORE_ITEM = blockItem(
            "refining_forge_core", REFINING_FORGE_CORE, 1);
    public static final RegistryObject<Item> RUNE_STONE_ITEM = blockItem(
            "rune_stone", RUNE_STONE, 64);
    public static final RegistryObject<Item> SOUL_REFINING_FURNACE_ITEM = blockItem(
            "soul_refining_furnace", SOUL_REFINING_FURNACE, 1);
    public static final RegistryObject<Item> SPIRIT_DISMANTLING_FURNACE_ITEM = blockItem(
            "spirit_dismantling_furnace", SPIRIT_DISMANTLING_FURNACE, 1);
    public static final RegistryObject<Item> SOUL_HEALING_ROOM_ITEM = blockItem(
            "soul_healing_room", SOUL_HEALING_ROOM, 1);
    /** 仅用于调试/创造栏取出空座；正常玩法由人皇幡右键放置并写入完整幡数据。 */
    public static final RegistryObject<Item> HUMAN_EMPEROR_BANNER_STAND_ITEM = blockItem(
            "human_emperor_banner_stand", HUMAN_EMPEROR_BANNER_STAND, 1);

    private static RegistryObject<Block> furnace(String id, int tier) {
        return BLOCKS.register(id, () -> new AlchemyFurnaceBlock(tier));
    }

    private static RegistryObject<Block> exoticFire(String id, int tier) {
        return BLOCKS.register(id, () -> new ExoticFireBlock(tier,
                BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.FIRE)
                        .noCollission()
                        .noOcclusion()
                        .noLootTable()
                        .lightLevel(state -> ExoticFireColorRegistry.lightLevel(tier))));
    }

    private static Block refiningForgeCore() {
        return new RefiningForgeBlock(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                // 成型后核心是空心井壁切片，必须关闭遮剔，否则会把相邻
                // 结构方块的内侧面剔掉，井口四周露出大洞。
                .noOcclusion()
                .lightLevel(state -> state.getValue(RefiningForgeBlock.LIT) ? 15
                        : state.getValue(RefiningForgeBlock.FORGED) ? 5 : 0));
    }

    private static Block runeStone() {
        return new RuneStoneBlock(BlockBehaviour.Properties.of()
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .lightLevel(state -> state.getValue(RuneStoneBlock.LIT) ? 10
                        : state.getValue(RuneStoneBlock.FORGED) ? 4 : 0));
    }

    private static RegistryObject<Block> cultivation(
            String id, CultivationBlock.Kind kind, int light) {
        return BLOCKS.register(id, () -> new CultivationBlock(
                kind,
                BlockBehaviour.Properties.of()
                        .strength(2.0F)
                        .noOcclusion()
                        .lightLevel(state -> light)));
    }

    private static RegistryObject<Item> blockItem(
            String id, RegistryObject<Block> block, int count) {
        return WuXingItems.ITEMS.register(
                id,
                () -> new BlockItem(
                        block.get(),
                        new Item.Properties().stacksTo(count)));
    }

    private static LiquidBlock spiritLiquidBlock() {
        return new SpiritLiquidBlock(ModFluids.SPIRIT_LIQUID_SOURCE, BlockBehaviour.Properties.of()
                .liquid()
                .noCollission()
                .replaceable()
                .noLootTable()
                .lightLevel(state -> 6));
    }

    private static BlockBehaviour.Properties woodProperties() {
        return BlockBehaviour.Properties.of()
                .strength(2.0F)
                .sound(net.minecraft.world.level.block.SoundType.WOOD)
                .ignitedByLava();
    }

    private static BlockBehaviour.Properties platformProperties() {
        return BlockBehaviour.Properties.of()
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(net.minecraft.world.level.block.SoundType.WOOD)
                .noOcclusion();
    }

    private static BlockBehaviour.Properties oreProperties(int light) {
        return BlockBehaviour.Properties.of()
                .strength(3.0F, 3.0F)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> light);
    }

    private WuXingBlocks() {}
}
