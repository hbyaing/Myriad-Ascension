package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.AcupointScreen;
import cn.blockforge.generated.generatedmod.client.CultivationMainScreen;
import cn.blockforge.generated.generatedmod.client.CultivationPage;
import cn.blockforge.generated.generatedmod.client.ForgedSearchBox;
import cn.blockforge.generated.generatedmod.client.InnerVisionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class BaiqiaoCapture {
    private static final boolean ENABLED = Boolean.getBoolean("wuxingxiantu.baiqiaoCapture");
    private static final boolean EXPECT_SHADERS = Boolean.getBoolean("wuxingxiantu.myriadExternalShaders");
    private static final Path OUT = Path.of(System.getProperty("wuxingxiantu.baiqiaoOutput", "baiqiao-capture"));
    private static final int[][] CASES = {{1920, 1080, 3}, {2560, 1440, 4}, {1280, 720, 3}};
    private static final List<String> results = new ArrayList<>();
    private static final long BEGIN = System.nanoTime();
    private static boolean started, finished;
    private static int testCase, step, ticks;
    private static String capture;
    private static Screen origin;
    private static int invested;
    private static double clickX, clickY;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        try {
            GLFW.glfwHideWindow(client.getWindow().getWindow());
            client.options.pauseOnLostFocus = false;
            client.setWindowActive(true);
            check((System.nanoTime() - BEGIN) / 1.0E9 < 420, "Client timeout at step " + step);
            if (!started && client.getOverlay() == null
                    && (client.screen instanceof TitleScreen || client.screen instanceof AccessibilityOnboardingScreen)) {
                started = true;
                Files.createDirectories(OUT);
                client.options.languageCode = "zh_cn";
                client.getLanguageManager().setSelected("zh_cn");
                client.reloadResourcePacks();
                client.options.renderDistance().set(2);
                client.options.simulationDistance().set(5);
                client.options.framerateLimit().set(30);
                client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var rules = new GameRules();
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                client.createWorldOpenFlows().createFreshLevel("baiqiao-qa-" + System.currentTimeMillis(),
                        new LevelSettings("Baiqiao isolated QA", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                                true, rules, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(7215, false, false), WorldPresets::createNormalWorldDimensions);
            }
            if (!started || client.player == null || client.getOverlay() != null || capture != null) return;
            ticks++;
            if (step == 0 && ticks == 8) {
                if (EXPECT_SHADERS) check(shaderPackActive(), "Expected Solas shader pack is not active");
                int[] display = CASES[testCase];
                client.getWindow().setWindowed(display[0], display[1]);
                client.getWindow().setWidth(display[0]);
                client.getWindow().setHeight(display[1]);
                client.options.guiScale().set(display[2]);
                client.resizeDisplay();
                client.mouseHandler.grabMouse();
                PointerSafety.verify();
                origin = testCase == 1 ? new InnerVisionScreen() : new CultivationMainScreen(client.player, "vision");
                client.setScreen(origin);
                // The normal screen-opening event maps the I-key screen to the vision page.
                origin = client.screen;
                next("entry");
            } else if (step == 1 && ticks == 4) {
                clickEntry(client);
                check(client.screen instanceof AcupointScreen, "Entry did not open Baiqiao");
                next("overview");
            } else if (step == 2 && ticks == 2) {
                var screen = (AcupointScreen) client.screen;
                click(screen, widget(screen, testCase == 2 ? "全部位" : "全部部位"));
                check(widget(screen, "躯干").visible, "Region menu did not open");
                capture = name("filters");
            } else if (step == 2 && ticks == 4) {
                var screen = (AcupointScreen) client.screen;
                click(screen, widget(screen, "躯干"));
                var ready = widget(screen, "气海 · CV-6");
                check(ready.visible, "Torso filter hid Qihai");
                click(screen, widget(screen, "石门 · CV-5"));
                check(!widget(screen, "冲击一次").active && !widget(screen, "按住冲穴").active,
                        "Locked point did not disable charge");
                capture = name("locked");
            } else if (step == 2 && ticks == 6) {
                var screen = (AcupointScreen) client.screen;
                click(screen, widget(screen, "气海 · CV-6"));
                click(screen, widget(screen, "躯干"));
                click(screen, widget(screen, "全部部位"));
            } else if (step == 2 && ticks == 8) {
                var screen = (AcupointScreen) client.screen;
                assertBounds(screen);
                var search = screen.children().stream().filter(ForgedSearchBox.class::isInstance)
                        .map(ForgedSearchBox.class::cast).findFirst().orElseThrow();
                screen.mouseClicked(search.getX() + 3, search.getY() + 6, 0);
                check(search.isFocused(), "Search did not focus");
                for (char ch : "CV-6".toCharArray()) screen.charTyped(ch, 0);
                check(search.getValue().equals("CV-6"), "Search keystrokes did not enter");
                check(widget(screen, "气海 · CV-6").visible, "Exact ID search did not show the selected point");
                invested = investment(client);
                next("search");
            } else if (step == 3 && ticks == 1) {
                var screen = (AcupointScreen) client.screen;
                click(screen, widget(screen, "气海 · CV-6"));
                if (testCase == 0) click(screen, widget(screen, "冲击一次"));
            } else if (step == 3 && ticks == 12) {
                if (testCase == 0) check(investment(client) == invested + 10, "Server tap did not sync to client");
                var screen = (AcupointScreen) client.screen;
                if (testCase == 0) {
                    var hold = widget(screen, "按住冲穴");
                    clickX = hold.getX() + hold.getWidth() / 2.0;
                    clickY = hold.getY() + hold.getHeight() / 2.0;
                    check(screen.mouseClicked(clickX, clickY, 0), "Hold widget click missed");
                }
                next(null);
            } else if (step == 4 && ticks == 16) {
                var screen = (AcupointScreen) client.screen;
                if (testCase == 0) {
                    check(investment(client) > invested + 10, "Hold did not sync to client");
                    screen.mouseReleased(clickX, clickY, 0);
                }
                var search = screen.children().stream().filter(ForgedSearchBox.class::isInstance)
                        .map(ForgedSearchBox.class::cast).findFirst().orElseThrow();
                search.setValue("");
                click(screen, widget(screen, "石门 · CV-5"));
                click(screen, widget(screen, "气海 · CV-6"));
                click(screen, widget(screen, "经脉图谱"));
                click(screen, widget(screen, "正面"));
                check(widget(screen, "背面").visible, "Projection did not switch to the back");
                click(screen, widget(screen, "全身"));
                var control = widget(screen, "局部放大");
                click(screen, control);
                assertBounds(screen);
                double x = control.getX() + control.getWidth() / 2.0, y = control.getY() + control.getHeight() + 35;
                check(screen.mouseScrolled(x, y, 1), "Zoom did not consume scroll");
                check(screen.mouseClicked(x, y, 1) && screen.mouseDragged(x + 4, y + 3, 1, 4, 3)
                        && screen.mouseReleased(x + 4, y + 3, 1), "Map pan did not consume drag");
                next("zoom");
            } else if (step == 5 && ticks == 8) {
                var screen = (AcupointScreen) client.screen;
                click(screen, widget(screen, "周天循环"));
                next("cycles");
            } else if (step == 6 && ticks == 8) {
                var screen = (AcupointScreen) client.screen;
                click(screen, widget(screen, "属性总览"));
                next("attributes");
            } else if (step == 7 && ticks == 5) {
                var screen = (AcupointScreen) client.screen;
                click(screen, widget(screen, "返回内视"));
                check(client.screen == origin, "Return did not restore the opening screen");
                results.add("PASS " + name("entry, region dropdown, locked button state, index selection, focused ID search, projection switch, zoom/pan, tabs and return") + (testCase == 0 ? ", server tap/hold" : ""));
                testCase++;
                step = ticks = 0;
                if (testCase == CASES.length) finish(client, null);
            }
        } catch (Throwable failure) {
            finish(client, failure);
        }
    }

    private static boolean shaderPackActive() throws ReflectiveOperationException {
        for (String name : List.of("net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi")) {
            try {
                Class<?> api = Class.forName(name);
                return (boolean) api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null));
            } catch (ClassNotFoundException ignored) {}
        }
        return false;
    }

    private static int investment(Minecraft client) {
        var state = GeneratedMod.data(client.player).acupoints();
        return state.invested(state.catalog().points().get("CV-6"));
    }

    private static void clickEntry(Minecraft client) throws ReflectiveOperationException {
        if (origin instanceof CultivationMainScreen host) {
            var field = CultivationMainScreen.class.getDeclaredField("page");
            field.setAccessible(true);
            var page = (CultivationPage) field.get(host);
            var entry = widget(page, "百窍");
            double x = coordinate(host, "pageX", entry.getX() + entry.getWidth() / 2.0);
            double y = coordinate(host, "pageY", entry.getY() + entry.getHeight() / 2.0);
            check(host.mouseClicked(x, y, 0), "Embedded entry hitbox missed");
        } else {
            click(origin, widget(origin, "百窍"));
        }
        var window = client.getWindow();
        int configured = window.calculateScale(client.options.guiScale().get(), client.isEnforceUnicode());
        check(window.getGuiScale() == configured && client.screen.width == window.getGuiScaledWidth()
                && client.screen.height == window.getGuiScaledHeight(), "Baiqiao used a stale GUI scale: "
                + window.getGuiScale() + " / " + configured);
    }

    private static double coordinate(CultivationMainScreen host, String methodName, double local) throws ReflectiveOperationException {
        var method = CultivationMainScreen.class.getDeclaredMethod(methodName, double.class);
        method.setAccessible(true);
        double atZero = (double) method.invoke(host, 0.0);
        double step = (double) method.invoke(host, 1.0) - atZero;
        return (local - atZero) / step;
    }

    private static AbstractWidget widget(Screen screen, String text) {
        return screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                .filter(widget -> widget.visible && widget.getMessage().getString().equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing widget " + text + " in " + screen.getTitle().getString()));
    }

    private static void click(Screen screen, AbstractWidget widget) {
        double x = widget.getX() + widget.getWidth() / 2.0;
        double y = widget.getY() + widget.getHeight() / 2.0;
        check(screen.mouseClicked(x, y, 0), "Missed " + widget.getMessage().getString());
        screen.mouseReleased(x, y, 0);
    }

    private static void assertBounds(Screen screen) {
        for (var child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.visible)
                check(widget.getX() >= 0 && widget.getY() >= 0 && widget.getX() + widget.getWidth() <= screen.width
                        && widget.getY() + widget.getHeight() <= screen.height, "Clipped widget " + widget.getMessage().getString());
        }
    }

    private static void next(String screenshot) {
        step++;
        ticks = 0;
        if (screenshot != null) capture = name(screenshot);
    }

    private static String name(String suffix) {
        int[] c = CASES[testCase];
        return c[0] + "x" + c[1] + "-gui" + c[2] + "-" + suffix;
    }

    @SubscribeEvent
    public static void rendered(TickEvent.RenderTickEvent event) {
        if (!ENABLED || finished || capture == null || event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            image.writeToFile(OUT.resolve(capture + ".png"));
            capture = null;
        } catch (Throwable failure) {
            finish(client, failure);
        }
    }

    private static void finish(Minecraft client, Throwable failure) {
        finished = true;
        if (failure != null) {
            results.add("FAIL step " + step + ": " + failure);
            com.mojang.logging.LogUtils.getLogger().error("Baiqiao capture failed", failure);
        }
        results.add("External shader pack required " + EXPECT_SHADERS);
        results.add("Pointer safety " + PointerSafety.snapshot());
        try { Files.write(OUT.resolve("verification.txt"), results); }
        catch (Exception error) { throw new RuntimeException(error); }
        finally { client.stop(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private BaiqiaoCapture() {}
}
