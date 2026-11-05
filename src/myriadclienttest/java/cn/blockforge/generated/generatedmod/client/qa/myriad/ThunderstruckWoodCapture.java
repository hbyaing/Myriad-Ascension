package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.WuXingBlocks;
import cn.blockforge.generated.generatedmod.blocks.ThunderstruckWoodBlock.RuneAnchor;
import cn.blockforge.generated.generatedmod.client.TrailShaderBridge;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Isolated, hidden client only. The normal game paths place, synchronize and render the blocks. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class ThunderstruckWoodCapture {
    private static final boolean ENABLED = Boolean.getBoolean("wuxingxiantu.thunderstruckWoodCapture");
    private static final boolean SOLAS = Boolean.getBoolean("wuxingxiantu.myriadExternalShaders")
            && !"off".equals(System.getProperty("wuxingxiantu.myriadExpectedShaderPack"));
    private static final BlockPos BASE = new BlockPos(8, 110, 8);
    private static final BlockPos HORIZONTAL = BASE.offset(-6, 1, 0);
    private static final Path OUT = Path.of(System.getProperty("wuxingxiantu.thunderstruckWoodOutput", "captures"));
    private static final double[] PHASES = {.68, 1.52, 2.40, 3.05};
    private static final long START = System.nanoTime();
    private static boolean opened, setup, finished;
    private static int scene, settled, frame;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            GLFW.glfwHideWindow(mc.getWindow().getWindow());
            mc.setWindowActive(true);
            mc.options.pauseOnLostFocus = false;
            if ((System.nanoTime() - START) / 1e9 > 210) throw new AssertionError("Capture timed out at scene " + scene);
            if (!opened && mc.getOverlay() == null
                    && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
                opened = true;
                Files.createDirectories(OUT);
                mc.getWindow().setWindowed(1280, 720);
                mc.resizeDisplay();
                mc.options.renderDistance().set(5);
                mc.options.simulationDistance().set(5);
                mc.options.framerateLimit().set(45);
                mc.options.hideGui = true;
                mc.options.bobView().set(false);
                mc.getTutorial().setStep(TutorialSteps.NONE);
                var rules = new GameRules();
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                mc.createWorldOpenFlows().createFreshLevel("thunderstruck-runes-" + System.currentTimeMillis(),
                        new LevelSettings("Thunderstruck wood capture", GameType.CREATIVE, false,
                                Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(7215, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (!opened || mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
            if (mc.getOverlay() != null || mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen) return;
            if (!setup) {
                setup = true;
                if (mc.screen != null) mc.setScreen(null);
                mc.mouseHandler.grabMouse();
                PointerSafety.verify();
                onServer(mc, ThunderstruckWoodCapture::prepare);
            }
            BlockPos anchor = scene == 1 ? HORIZONTAL : BASE;
            if (!(mc.level.getBlockEntity(anchor) instanceof RuneAnchor)) return;
            if (scene == 2 && !mc.level.getBlockState(BASE.offset(1, 1, 2)).is(Blocks.BLACKSTONE)) return;
            settled++;
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static void prepare(ServerPlayer player) {
        var level = player.serverLevel();
        level.setDayTime(13100);
        level.setWeatherParameters(12000, 0, false, false);
        for (int x = -11; x <= 8; x++) {
            for (int z = -5; z <= 10; z++) {
                level.setBlockAndUpdate(BASE.offset(x, -1, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
        var wood = WuXingBlocks.THUNDERSTRUCK_WOOD.get().defaultBlockState();
        for (int i = 0; i < 3; i++) {
            level.setBlockAndUpdate(BASE.above(i), wood);
            level.setBlockAndUpdate(HORIZONTAL.east(i), wood.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        }
        look(player, Vec3.atLowerCornerOf(BASE).add(.5, 1.45, .5), new Vec3(2.4, .45, 4.4));
    }

    private static void look(ServerPlayer player, Vec3 target, Vec3 offset) {
        Vec3 eye = target.add(offset);
        Vec3 dir = target.subtract(eye);
        float yaw = (float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z)));
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(player.serverLevel(), eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void frame(TickEvent.RenderTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END || settled < 40) return;
        var mc = Minecraft.getInstance();
        try {
            boolean pack = TrailShaderBridge.shaderPackDetected();
            if (pack != SOLAS) throw new AssertionError("Unexpected shader-pack status: " + pack);
            if (SOLAS) {
                var iris = Class.forName("net.irisshaders.iris.Iris");
                if (!"Solas Shader V3.7b.zip".equals(iris.getMethod("getCurrentPackName").invoke(null))) {
                    throw new AssertionError("Solas was not selected");
                }
            }
            BlockPos pos = scene == 1 ? HORIZONTAL : BASE;
            long seed = scene == 1 ? Mth.getSeed(0, pos.getY(), pos.getZ()) : Mth.getSeed(pos.getX(), 0, pos.getZ());
            double offset = ((seed >>> 32) & 255) / 255.0 * 5.78;
            double head = (((double) mc.level.getGameTime() + event.renderTickTime) * .075 + offset) % 5.78 - .18;
            double wanted = scene == 0 ? PHASES[frame] : 1.52;
            if (Math.abs(head - wanted) > .11) return;

            int gl = GL11.glGetError();
            if (gl != GL11.GL_NO_ERROR) throw new AssertionError("GL error " + gl);
            String name = scene == 0 ? "vertical-" + frame : scene == 1 ? "horizontal" : "behind-wall";
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(OUT.resolve(name + ".png"));
            }
            if (scene == 0 && ++frame < PHASES.length) return;
            scene++;
            settled = 0;
            if (scene == 1) {
                onServer(mc, player -> look(player, Vec3.atCenterOf(HORIZONTAL.east()), new Vec3(2.1, 1.5, 4.7)));
            } else if (scene == 2) {
                onServer(mc, player -> {
                    for (int x = 0; x < 4; x++) {
                        for (int y = 0; y < 4; y++) {
                            player.serverLevel().setBlockAndUpdate(BASE.offset(x, y, 2), Blocks.BLACKSTONE.defaultBlockState());
                        }
                    }
                    look(player, Vec3.atLowerCornerOf(BASE).add(.5, 1.45, .5), new Vec3(2.4, .45, 4.4));
                });
            } else {
                finish(null);
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static void onServer(Minecraft mc, java.util.function.Consumer<ServerPlayer> task) {
        var server = mc.getSingleplayerServer();
        var id = mc.player.getUUID();
        server.execute(() -> task.accept(server.getPlayerList().getPlayer(id)));
    }

    private static void finish(Throwable error) {
        finished = true;
        if (error != null) com.mojang.logging.LogUtils.getLogger().error("THUNDERSTRUCK_WOOD_CAPTURE", error);
        try {
            Files.createDirectories(OUT);
            Files.writeString(OUT.resolve("result.json"), new Gson().toJson(Map.of(
                    "passed", error == null, "error", error == null ? "" : error.toString(),
                    "shaderPack", SOLAS ? "Solas Shader V3.7b.zip" : "off", "pointer", PointerSafety.snapshot())));
        } catch (Exception writeError) {
            com.mojang.logging.LogUtils.getLogger().error("Cannot record rune capture", writeError);
        }
        Minecraft.getInstance().stop();
    }
}
