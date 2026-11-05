package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.blocks.ThunderstruckWoodBlock.RuneAnchor;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import cn.blockforge.generated.generatedmod.registry.ModBlockEntities;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class ThunderstruckWoodRunes implements BlockEntityRenderer<RuneAnchor> {
    private static final ResourceLocation FIELD = new ResourceLocation(GeneratedMod.MOD_ID,
            "textures/effect/thunderstruck_wood_field.png");
    private static final float SURFACE_OFFSET = 0.0005F;
    private static final Direction[] BARK = {Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};
    private static final VertexFormat FORMAT = DefaultVertexFormat.POSITION_COLOR_TEX;
    private static final BufferBuilder BATCH = new BufferBuilder(32768);
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static ShaderInstance shader;
    private static BufferBuilder.RenderedBuffer pending;
    private static float travel;

    private ThunderstruckWoodRunes(BlockEntityRendererProvider.Context context) {}

    @Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void shader(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(GeneratedMod.MOD_ID, "thunderstruck_wood"), FORMAT), value -> shader = value);
        }

        @SubscribeEvent
        public static void renderer(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.THUNDERSTRUCK_WOOD.get(), ThunderstruckWoodRunes::new);
        }
    }

    @Override
    public void render(RuneAnchor anchor, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = anchor.getLevel();
        if (level == null || shader == null || TrailShaderBridge.shadowPass()) return;
        BlockState state = anchor.getBlockState();
        BlockPos pos = anchor.getBlockPos();
        Direction.Axis axis = state.getValue(RotatedPillarBlock.AXIS);
        var rotation = switch (axis) {
            case X -> BlockModelRotation.X90_Y90.getRotation();
            case Y -> BlockModelRotation.X0_Y0.getRotation();
            case Z -> BlockModelRotation.X90_Y0.getRotation();
        };
        Direction top = Direction.rotate(rotation.getMatrix(), Direction.UP);
        int row = rowInPattern(level, state, pos, top);
        int phase = (int) (Mth.getSeed(axis == Direction.Axis.X ? 0 : pos.getX(),
                axis == Direction.Axis.Y ? 0 : pos.getY(), axis == Direction.Axis.Z ? 0 : pos.getZ()) >>> 32) & 255;

        pose.pushPose();
        pose.translate(.5, .5, .5);
        pose.mulPose(rotation.getLeftRotation());
        pose.translate(-.5, -.5, -.5);
        for (int i = 0; i < BARK.length; i++) {
            Direction face = Direction.rotate(rotation.getMatrix(), BARK[i]);
            if (!Block.shouldRenderFace(state, level, pos, face, pos.relative(face))) continue;
            if (!BATCH.building()) BATCH.begin(VertexFormat.Mode.QUADS, FORMAT);
            addFace(pose.last().pose(), i, row, phase);
        }
        pose.popPose();
    }

    private static int rowInPattern(Level level, BlockState state, BlockPos pos, Direction top) {
        // Preserve the workbench's three-block composition for a single short run.
        // Longer connected runs tile in groups of three without scanning an entire structure.
        int above = 0;
        while (above < 3 && level.getBlockState(pos.relative(top, above + 1)) == state) above++;
        int length = above + 1;
        for (int below = 1; length < 4 && level.getBlockState(pos.relative(top.getOpposite(), below)) == state; below++) {
            length++;
        }
        return length < 4 ? above : Math.floorMod(-top.getAxisDirection().getStep() * top.getAxis().choose(
                pos.getX(), pos.getY(), pos.getZ()), 3);
    }

    private static void addFace(Matrix4f pose, int face, int row, int phase) {
        Direction normal = BARK[face];
        // Looking at the exterior of a south face, +X is the workbench's right edge.
        Direction right = normal.getCounterClockWise();
        float x = .5F + normal.getStepX() * (.5F + SURFACE_OFFSET);
        float z = .5F + normal.getStepZ() * (.5F + SURFACE_OFFSET);
        float dx = right.getStepX() * (.5F + SURFACE_OFFSET);
        float dz = right.getStepZ() * (.5F + SURFACE_OFFSET);
        float u0 = face * .25F;
        float u1 = u0 + .25F;
        float v0 = row / 3F;
        float v1 = (row + 1) / 3F;
        BATCH.vertex(pose, x - dx, 0, z - dz).color(phase, 0, 0, 255).uv(u0, v1).endVertex();
        BATCH.vertex(pose, x + dx, 0, z + dz).color(phase, 0, 0, 255).uv(u1, v1).endVertex();
        BATCH.vertex(pose, x + dx, 1, z + dz).color(phase, 0, 0, 255).uv(u1, v0).endVertex();
        BATCH.vertex(pose, x - dx, 1, z - dz).color(phase, 0, 0, 255).uv(u0, v0).endVertex();
    }

    @SubscribeEvent
    public static void stage(RenderLevelStageEvent event) {
        if (!BATCH.building()) return;
        boolean shaderPack = TrailShaderBridge.shaderPackDetected();
        var stage = shaderPack ? RenderLevelStageEvent.Stage.AFTER_PARTICLES
                : RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
        if (event.getStage() != stage || TrailShaderBridge.shadowPass()) return;

        var level = Minecraft.getInstance().level;
        if (level == null) return;
        PROJECTION.set(event.getProjectionMatrix());
        travel = (float) ((((double) level.getGameTime() + event.getPartialTick()) * .075) % 5.78);
        var mesh = BATCH.end();
        if (!shaderPack) {
            draw(mesh, -1);
        } else if (TrailShaderBridge.captureDepth()) {
            pending = mesh;
        } else {
            mesh.release();
        }
    }

    @SubscribeEvent
    public static void beforeHud(RenderGuiEvent.Pre event) {
        if (pending == null) return;
        event.getGuiGraphics().flush();
        drawPending();
    }

    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) drawPending();
        if (BATCH.building()) BATCH.end().release();
        if (pending != null) {
            pending.release();
            pending = null;
        }
    }

    private static void drawPending() {
        if (pending == null) return;
        var mesh = pending;
        pending = null;
        int depth = TrailShaderBridge.capturedDepthTexture();
        if (depth >= 0) draw(mesh, depth);
        else mesh.release();
    }

    private static void draw(BufferBuilder.RenderedBuffer mesh, int depth) {
        var target = Minecraft.getInstance().getMainRenderTarget();
        try (var saved = new MeteorBloomRenderer.State()) {
            if (depth >= 0) {
                target.bindWrite(true);
                RenderSystem.disableDepthTest();
                RenderSystem.disableScissor();
            } else {
                RenderSystem.enableDepthTest();
            }
            RenderSystem.depthMask(false);
            RenderSystem.enableCull();
            RenderSystem.enableBlend();
            RenderSystem.colorMask(true, true, true, false);
            RenderSystem.setShaderTexture(0, FIELD);
            RenderSystem.setShaderTexture(1, depth >= 0 ? depth : RenderSystem.getShaderTexture(0));
            shader.safeGetUniform("WorldProjection").set(PROJECTION);
            shader.safeGetUniform("OutputSize").set((float) target.viewWidth, (float) target.viewHeight);
            shader.safeGetUniform("UseSceneDepth").set(depth >= 0 ? 1F : 0F);
            shader.safeGetUniform("Travel").set(travel);
            RenderSystem.setShader(() -> shader);
            BufferUploader.drawWithShader(mesh);
        }
    }
}
