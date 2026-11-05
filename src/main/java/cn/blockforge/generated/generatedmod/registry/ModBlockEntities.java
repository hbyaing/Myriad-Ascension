package cn.blockforge.generated.generatedmod.registry;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.SoulFurnaceBlockEntity;
import cn.blockforge.generated.generatedmod.SoulHealingRoomBlockEntity;
import cn.blockforge.generated.generatedmod.TalismanPlatformBlockEntity;
import cn.blockforge.generated.generatedmod.SpiritPlatformBlockEntity;
import cn.blockforge.generated.generatedmod.ChenghuaPedestalBlockEntity;
import cn.blockforge.generated.generatedmod.RefiningForgeBlockEntity;
import cn.blockforge.generated.generatedmod.HumanEmperorBannerStandBlockEntity;
import cn.blockforge.generated.generatedmod.AlchemyFurnaceBlockEntity;
import cn.blockforge.generated.generatedmod.WuXingBlocks;
import cn.blockforge.generated.generatedmod.blocks.ThunderstruckWoodBlock.RuneAnchor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GeneratedMod.MOD_ID);
    public static final RegistryObject<BlockEntityType<RuneAnchor>> THUNDERSTRUCK_WOOD =
            BLOCK_ENTITIES.register("thunderstruck_wood", () -> BlockEntityType.Builder.of(
                    RuneAnchor::new, WuXingBlocks.THUNDERSTRUCK_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<cn.blockforge.generated.generatedmod.alchemy.research.ResearchTableEntity>> ALCHEMY_RESEARCH_TABLE=BLOCK_ENTITIES.register("alchemy_research_table",()->BlockEntityType.Builder.of(cn.blockforge.generated.generatedmod.alchemy.research.ResearchTableEntity::new,WuXingBlocks.ALCHEMY_RESEARCH_TABLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<cn.blockforge.generated.generatedmod.alchemy.inputs.MoonBottleBlockEntity>> MOON_BOTTLE=
            BLOCK_ENTITIES.register("moon_bottle",()->BlockEntityType.Builder.of(cn.blockforge.generated.generatedmod.alchemy.inputs.MoonBottleBlockEntity::new,WuXingBlocks.MOON_BOTTLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<cn.blockforge.generated.generatedmod.HerbProcessingTableBlockEntity>> HERB_PROCESSING_TABLE=
            BLOCK_ENTITIES.register("herb_processing_table",()->BlockEntityType.Builder.of(cn.blockforge.generated.generatedmod.HerbProcessingTableBlockEntity::new,WuXingBlocks.HERB_PROCESSING_TABLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<cn.blockforge.generated.generatedmod.AlchemyFurnacePartEntity>> ALCHEMY_FURNACE_PART =
            BLOCK_ENTITIES.register("alchemy_furnace_part", () -> BlockEntityType.Builder.of(
                    cn.blockforge.generated.generatedmod.AlchemyFurnacePartEntity::new, WuXingBlocks.ALCHEMY_FURNACE_PART.get()).build(null));
    public static final RegistryObject<BlockEntityType<SoulFurnaceBlockEntity>> SOUL_REFINING_FURNACE =
            BLOCK_ENTITIES.register("soul_refining_furnace", () -> BlockEntityType.Builder.of(
                    ModBlockEntities::createSoulRefining, WuXingBlocks.SOUL_REFINING_FURNACE.get()).build(null));
    public static final RegistryObject<BlockEntityType<SoulFurnaceBlockEntity>> SPIRIT_DISMANTLING_FURNACE =
            BLOCK_ENTITIES.register("spirit_dismantling_furnace", () -> BlockEntityType.Builder.of(
                    ModBlockEntities::createSpiritDismantling, WuXingBlocks.SPIRIT_DISMANTLING_FURNACE.get()).build(null));
    public static final RegistryObject<BlockEntityType<SoulHealingRoomBlockEntity>> SOUL_HEALING_ROOM =
            BLOCK_ENTITIES.register("soul_healing_room", () -> BlockEntityType.Builder.of(
                    ModBlockEntities::createSoulHealingRoom, WuXingBlocks.SOUL_HEALING_ROOM.get()).build(null));
    public static final RegistryObject<BlockEntityType<SpiritPlatformBlockEntity>> SPIRIT_PLATFORM =
            BLOCK_ENTITIES.register("spirit_platform", () -> BlockEntityType.Builder.of(
                    ModBlockEntities::createSpiritPlatform, WuXingBlocks.SPIRIT_PLATFORM.get()).build(null));
    public static final RegistryObject<BlockEntityType<ChenghuaPedestalBlockEntity>> CHENGHUA_PEDESTAL =
            BLOCK_ENTITIES.register("chenghua_pedestal", () -> BlockEntityType.Builder.of(
                    ModBlockEntities::createChenghuaPedestal, WuXingBlocks.CHENGHUA_PEDESTAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<RefiningForgeBlockEntity>> REFINING_FORGE =
            BLOCK_ENTITIES.register("refining_forge", () -> BlockEntityType.Builder.of(
                    ModBlockEntities::createRefiningForge, WuXingBlocks.REFINING_FORGE_CORE.get()).build(null));
    public static final RegistryObject<BlockEntityType<HumanEmperorBannerStandBlockEntity>> HUMAN_EMPEROR_BANNER_STAND =
            BLOCK_ENTITIES.register("human_emperor_banner_stand", () -> BlockEntityType.Builder.of(
                    ModBlockEntities::createHumanEmperorBannerStand, WuXingBlocks.HUMAN_EMPEROR_BANNER_STAND.get()).build(null));
    public static final RegistryObject<BlockEntityType<AlchemyFurnaceBlockEntity>> ALCHEMY_FURNACE =
            BLOCK_ENTITIES.register("alchemy_furnace", () -> BlockEntityType.Builder.of(
                    ModBlockEntities::createAlchemyFurnace,
                    WuXingBlocks.ALCHEMY_FURNACE.get(), WuXingBlocks.ALCHEMY_FURNACE_LOW.get(),
                    WuXingBlocks.ALCHEMY_FURNACE_MID.get(), WuXingBlocks.ALCHEMY_FURNACE_HIGH.get(),
                    WuXingBlocks.ALCHEMY_FURNACE_PERFECT.get(), WuXingBlocks.ALCHEMY_FURNACE_IMMORTAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<TalismanPlatformBlockEntity>> TALISMAN_PLATFORM =
            BLOCK_ENTITIES.register("talisman_platform", () -> BlockEntityType.Builder.of(
                    ModBlockEntities::createTalismanPlatform,
                    WuXingBlocks.ENGRAVING_PLATFORM.get(), WuXingBlocks.RUBBING_PLATFORM.get(),
                    WuXingBlocks.DRAWING_PLATFORM.get()).build(null));

    private static TalismanPlatformBlockEntity createTalismanPlatform(net.minecraft.core.BlockPos pos,
                                                                        net.minecraft.world.level.block.state.BlockState state) {
        TalismanPlatformBlockEntity.Kind kind = TalismanPlatformBlockEntity.Kind.ENGRAVE;
        if (state.getBlock() instanceof cn.blockforge.generated.generatedmod.TalismanPlatformBlock platform) {
            kind = platform.kind();
        }
        return new TalismanPlatformBlockEntity(TALISMAN_PLATFORM.get(), pos, state, kind);
    }

    private static HumanEmperorBannerStandBlockEntity createHumanEmperorBannerStand(net.minecraft.core.BlockPos pos,
                                                                                       net.minecraft.world.level.block.state.BlockState state) {
        return new HumanEmperorBannerStandBlockEntity(HUMAN_EMPEROR_BANNER_STAND.get(), pos, state);
    }

    private static AlchemyFurnaceBlockEntity createAlchemyFurnace(net.minecraft.core.BlockPos pos,
                                                                   net.minecraft.world.level.block.state.BlockState state) {
        return new AlchemyFurnaceBlockEntity(ALCHEMY_FURNACE.get(), pos, state);
    }

    private static RefiningForgeBlockEntity createRefiningForge(net.minecraft.core.BlockPos pos,
                                                                  net.minecraft.world.level.block.state.BlockState state) {
        return new RefiningForgeBlockEntity(REFINING_FORGE.get(), pos, state);
    }

    private static SpiritPlatformBlockEntity createSpiritPlatform(net.minecraft.core.BlockPos pos,
                                                                  net.minecraft.world.level.block.state.BlockState state) {
        return new SpiritPlatformBlockEntity(SPIRIT_PLATFORM.get(), pos, state);
    }

    private static ChenghuaPedestalBlockEntity createChenghuaPedestal(net.minecraft.core.BlockPos pos,
                                                                      net.minecraft.world.level.block.state.BlockState state) {
        return new ChenghuaPedestalBlockEntity(CHENGHUA_PEDESTAL.get(), pos, state);
    }

    private static SoulHealingRoomBlockEntity createSoulHealingRoom(net.minecraft.core.BlockPos pos,
                                                                     net.minecraft.world.level.block.state.BlockState state) {
        return new SoulHealingRoomBlockEntity(SOUL_HEALING_ROOM.get(), pos, state);
    }

    private static SoulFurnaceBlockEntity createSoulRefining(net.minecraft.core.BlockPos pos,
                                                              net.minecraft.world.level.block.state.BlockState state) {
        return new SoulFurnaceBlockEntity(SOUL_REFINING_FURNACE.get(), pos, state,
                SoulFurnaceBlockEntity.Kind.SOUL_REFINING);
    }

    private static SoulFurnaceBlockEntity createSpiritDismantling(net.minecraft.core.BlockPos pos,
                                                                   net.minecraft.world.level.block.state.BlockState state) {
        return new SoulFurnaceBlockEntity(SPIRIT_DISMANTLING_FURNACE.get(), pos, state,
                SoulFurnaceBlockEntity.Kind.SPIRIT_DISMANTLING);
    }
    private ModBlockEntities() {}
}
