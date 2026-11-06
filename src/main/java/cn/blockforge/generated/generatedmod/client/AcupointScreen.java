package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.CultivationData;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog.*;
import cn.blockforge.generated.generatedmod.acupoint.AcupointPackets;
import cn.blockforge.generated.generatedmod.acupoint.AcupointProgress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.BooleanSupplier;

import static cn.blockforge.generated.generatedmod.acupoint.AcupointPackets.Action.*;
import static cn.blockforge.generated.generatedmod.client.AcupointBody.*;

public final class AcupointScreen extends Screen {
    private enum Tab { OVERVIEW, ROUTES, CYCLES, STATS }
    private enum View { BODY, DETAIL, MERIDIAN }
    private enum Filter { ALL, READY, OPEN, LOCKED }
    private enum Menu { NONE, STATE, REGION }

    private static final String[] ZONES = {"", "head", "neck", "torso", "arm", "hand", "leg", "foot"};
    private static final int RAIL = 0x807D6948;
    private static final int CONTROL_HEIGHT = 16;
    private static final int ROW_TOP = 74;
    private static final int LIST_BOTTOM = 7;
    private static final String KEY = "gui.wuxingxiantu.baiqiao.";

    private final Screen parent;
    private final List<CultivationUi.Action> rows = new ArrayList<>();
    private final List<CultivationUi.Action> states = new ArrayList<>(), regions = new ArrayList<>();
    private Menu menu = Menu.NONE;
    private Tab tab = Tab.OVERVIEW;
    private View view = View.BODY;
    private Filter filter = Filter.ALL;
    private String meridian = "", selected = "", active = "", holdPoint = "", query = "";
    private ResultView result = new ResultView(AcupointProgress.Result.IDLE, 0);
    private Set<String> overlap = Set.of();
    private List<Point> visiblePoints = List.of();
    private Set<String> mapTargets = Set.of();
    private Dot selectedDot;
    private Point detailPoint;
    private Component pointTitle, prerequisites, realm, routeTitle;
    private Reward pointReward;
    private List<Reward> routeRewards = List.of();
    private int rowHeight, rewardOffset, rewardRows;
    private Topology projected = Topology.EMPTY;
    private List<Projector> graphViews = List.of();
    private Data shownCatalog;
    private boolean back, dragging, draggingList, initialized;
    private boolean holdMouse;
    private int zoneIndex, offset, graphRevision, renderedRevision = -1, age, holdKey = -1, shownStage = -1;
    private double zoom = 2, panX, panY;
    private Box frame, left, graph, right;
    private CultivationUi.Action lineFilter, stateFilter, zoneFilter, side, tap, hold, auto;
    private ForgedSearchBox search;

    private record Reward(Component name, String value) {}
    static class BaiqiaoButton extends CultivationUi.Action {
        BaiqiaoButton(int x, int y, int w, int h, Component label, OnPress action, BooleanSupplier chosen) {
            super(x, y, w, h, label, action, chosen);
        }

        @Override protected void surface(GuiGraphics g, boolean chosen) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean focused = isHoveredOrFocused();
            boolean down = pressed();
            AlchemyChrome.button(g, x, y, w, h, active, active && chosen, focused, false, down, false);
            if (active && chosen) {
                int top = down ? 0xC03A2B17 : 0xD6342719;
                int bottom = down ? 0xCD6E4F26 : focused ? 0xD68E6A36 : 0xC87B592E;
                g.fillGradient(x + 3, y + 3, x + w - 3, y + h - 3, top, bottom);
            }
        }
    }

    private record ResultView(AcupointProgress.Result value, int until) {}

    public AcupointScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
    }

    private static Component text(String key, Object... args) { return Component.translatable(KEY + key, args); }
    private CultivationData data() { return minecraft.player == null ? null : GeneratedMod.data(minecraft.player); }
    private AcupointProgress progress() { return data().acupoints(); }
    private Data catalog() { return progress().catalog(); }
    private Point point() { return catalog().points().get(selected); }
    private int stage() { return AcupointProgress.stage(data()); }
    private boolean graphTab() { return tab == Tab.OVERVIEW || tab == Tab.ROUTES; }

    @Override
    protected void init() {
        if (data() == null) return;
        shownCatalog = catalog();
        if (selected.isEmpty()) {
            selected = catalog().points().containsKey(progress().selected()) ? progress().selected() : "CV-6";
            if (!catalog().points().containsKey(selected)) selected = catalog().points().keySet().iterator().next();
            meridian = point().meridian();
            back = catalog().markers().get(selected).get(0).back();
        }
        layout();
        if (!initialized) {
            initialized = true;
            request(SNAPSHOT, "");
        }
    }

    private void layout() {
        clearWidgets();
        setFocused(null);
        rows.clear();
        states.clear();
        regions.clear();
        menu = Menu.NONE;
        projected = Topology.EMPTY;
        renderedRevision = -1;
        int w = Math.min(900, width - 12), h = Math.min(520, height - 12);
        frame = new Box((width - w) / 2, (height - h) / 2, w, h);
        int x = frame.x() + 6, top = frame.y() + 6;
        int titleWidth = Mth.ceil(font.width(title) * 1.12F) + 13, backWidth = font.width(text("back")) + 12;
        int minimumTab = Arrays.stream(Tab.values())
                .mapToInt(value -> font.width(text("tab." + id(value))) + 10).max().orElseThrow();
        int headerRoom = w - 12 - 4 * (minimumTab + 3);
        boolean compactHeader = titleWidth + backWidth > headerRoom;
        if (!compactHeader) {
            titleWidth = Math.min(headerRoom - backWidth, Math.max(titleWidth, w * 23 / 100));
            backWidth = Math.min(headerRoom - titleWidth, Math.max(backWidth, w / 10));
        }
        int y = top + (compactHeader ? 41 : 23), bottom = frame.bottom() - 22;
        int l = Math.max(80, w * 23 / 100), r = Math.max(102, w * 29 / 100);
        left = new Box(x, y, l, bottom - y);
        graph = new Box(x + l + 4, y, w - l - r - 20, bottom - y);
        right = new Box(graph.right() + 4, y, r, bottom - y);
        action(frame.right() - backWidth - 6, top, backWidth, CONTROL_HEIGHT, text("back"), this::onClose, () -> false);
        int tabX = compactHeader ? x : x + titleWidth;
        int tabY = compactHeader ? top + 19 : top;
        int tabEnd = frame.right() - 6 - (compactHeader ? 0 : backWidth + 5);
        int tabWidth = (tabEnd - tabX + 3) / Tab.values().length;
        for (Tab value : Tab.values()) {
            action(tabX + value.ordinal() * tabWidth, tabY, tabWidth - 3, CONTROL_HEIGHT,
                    text("tab." + id(value)), () -> changeTab(value), () -> tab == value);
        }
        if (!graphTab()) return;

        int inset = left.x() + 4, available = left.width() - 8;
        search = addRenderableWidget(new ForgedSearchBox(font, inset + 15, y + 20, available - 18, 16, text("search")));
        search.setMaxLength(32);
        search.setHint(text("search"));
        search.setTextColor(CultivationUi.TEXT);
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            resetList();
        });

        int arrow = 15;
        routeArrow(inset, y + 39, arrow, -1);
        lineFilter = action(inset + arrow + 2, y + 39, available - (arrow + 2) * 2, 15,
                text("all_routes"), () -> {
                    if (meridian.isEmpty()) meridian = point().meridian();
                    else meridian = "";
                    resetList();
                    revealSelected();
                    updateControls();
                }, () -> !meridian.isEmpty());
        lineFilter.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text("route_filter_hint")));
        routeArrow(inset + available - arrow, y + 39, arrow, 1);
        int half = (available - 3) / 2;
        stateFilter = filterButton(inset, y + 56, half, text("filter.all"), Menu.STATE);
        zoneFilter = filterButton(inset + half + 3, y + 56, available - half - 3, text("zone.all"), Menu.REGION);
        for (Filter value : Filter.values()) {
            filterOption(states, value.ordinal(), Filter.values().length, text("filter." + id(value)),
                    () -> filter = value, () -> filter == value);
        }
        for (int i = 0; i < ZONES.length; i++) {
            int zone = i;
            filterOption(regions, i, ZONES.length, text("zone." + (i == 0 ? "all" : ZONES[i])),
                    () -> zoneIndex = zone, () -> zoneIndex == zone);
        }
        rowHeight = Mth.clamp(left.height() / 15, 13, 19);
        for (int row = 0; row < Math.max(1, (left.height() - ROW_TOP - LIST_BOTTOM) / rowHeight); row++) {
            final int index = row;
            var item = addRenderableWidget(new CultivationUi.Action(inset, y + ROW_TOP + row * rowHeight,
                    available - 6, rowHeight - 2, Component.empty(),
                    button -> select(visiblePoints.get(offset + index).id()),
                    () -> selected.equals(visiblePoints.get(offset + index).id())) {
                @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
                    Point point = visiblePoints.get(offset + index);
                    boolean chosen = selected.getAsBoolean();
                    int x = getX(), y = getY(), w = getWidth(), h = getHeight();
                    g.fill(x, y, x + w, y + h, chosen ? 0xBA463724 : isHoveredOrFocused() ? 0xD3314350 : 0x8230404D);
                    g.renderOutline(x, y, w, h, chosen ? GOLD : isHoveredOrFocused() ? 0xCFBBA16C : 0x604A606F);
                    int textY = y + (h - 8) / 2;
                    int codeWidth = font.width(point.id());
                    label(g, Component.literal(point.name()), x + 4, textY, w - codeWidth - 11, chosen ? GOLD : CultivationUi.TEXT);
                    g.drawString(font, point.id(), x + w - codeWidth - 4, textY, chosen ? GOLD : CultivationUi.MUTED, false);
                }
            });
            rows.add(item);
        }

        int modeWidth = (graph.width() - 9) / View.values().length;
        for (View mode : View.values()) {
            action(graph.x() + 4 + mode.ordinal() * modeWidth, graph.y() + 5, modeWidth - 2, CONTROL_HEIGHT,
                    text("view." + id(mode)), () -> changeView(mode), () -> view == mode);
        }
        side = action(graph.x() + 4, graph.bottom() - 33, graph.width() - 8, 13,
                text(back ? "back_view" : "front_view"), this::flipSide, () -> back);
        side.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text("side_hint")));

        int actionWidth = right.width() - 10;
        int buttonWidth = (actionWidth - 4) / 2;
        int buttonY = right.bottom() - 37;
        tap = action(right.x() + 5, buttonY, buttonWidth, CONTROL_HEIGHT, text("tap"),
                () -> request(TAP, selected), () -> true);
        hold = addRenderableWidget(new BaiqiaoButton(right.x() + 9 + buttonWidth, buttonY, actionWidth - buttonWidth - 4, CONTROL_HEIGHT,
                text("hold"), button -> {}, () -> selected.equals(holdPoint) || selected.equals(active)) {
            @Override public boolean mouseClicked(double mx, double my, int button) {
                if (button != 0 || !active || !isMouseOver(mx, my)) return false;
                playDownSound(minecraft.getSoundManager());
                beginHold(true, -1);
                return true;
            }
            @Override public boolean keyPressed(int key, int scan, int mods) {
                if (active && (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
                    beginHold(false, key);
                    return true;
                }
                return super.keyPressed(key, scan, mods);
            }
        });
        auto = addRenderableWidget(new BaiqiaoButton(right.x() + 5, right.bottom() - 19, actionWidth,
                CONTROL_HEIGHT, text("auto_off"), button -> request(autoHere() ? AUTO_OFF : AUTO_ON, autoMeridian()), this::autoHere) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
                int x = getX(), y = getY(), w = getWidth(), h = getHeight();
                boolean on = autoHere();
                surface(g, false);
                int toggleX = x + w - SettingsUiAssets.RIGHT_INSET - SettingsUiAssets.TOGGLE_WIDTH;
                SettingsUiAssets.toggle(g, toggleX, y + (h - SettingsUiAssets.TOGGLE_HEIGHT) / 2, on, active);
                Component state = text(on ? "switch_on" : "switch_off");
                int textY = y + (h - 8) / 2;
                int stateX = toggleX - 4 - font.width(state);
                detailLabel(g, text("auto_label"), x + 5, textY, stateX - x - 8,
                        active ? CultivationUi.TEXT : CultivationUi.MUTED, mx, my);
                g.drawString(font, state, stateX, textY, on ? GOLD : CultivationUi.MUTED, false);
                if (isMouseOver(mx, my)) CultivationUi.tooltip(getMessage());
            }
        });
        refreshList();
        for (int i = 0; i < visiblePoints.size(); i++) {
            if (!selected.equals(visiblePoints.get(i).id())) continue;
            scrollList(i - 2 - offset);
            break;
        }
        updateControls();
    }

    private CultivationUi.Action action(int x, int y, int w, int h, Component label, Runnable run, BooleanSupplier chosen) {
        return addRenderableWidget(new BaiqiaoButton(x, y, w, h, label, button -> run.run(), chosen));
    }

    private CultivationUi.Action filterButton(int x, int y, int w, Component label, Menu target) {
        return addRenderableWidget(new BaiqiaoButton(x, y, w, 15, label,
                button -> setMenu(menu == target ? Menu.NONE : target), () -> menu == target) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
                surface(g, selected.getAsBoolean());
                int textWidth = getWidth() - 13;
                int textX = getX() + Math.max(3, (textWidth - font.width(getMessage())) / 2);
                detailLabel(g, getMessage(), textX, getY() + 3, textWidth,
                        selected.getAsBoolean() ? GOLD : CultivationUi.TEXT, mx, my);
                ForgedUi.scrollArrow(g, getX() + getWidth() - 10, getY() + 4, menu != target);
            }
        });
    }

    private void filterOption(List<CultivationUi.Action> options, int index, int total,
                              Component label, Runnable apply, BooleanSupplier chosen) {
        int height = 12, available = left.width() - 8;
        int columns = (left.height() - ROW_TOP - LIST_BOTTOM) / height < total ? 2 : 1;
        int cell = available / columns;
        var option = action(left.x() + 4 + index % columns * cell, left.y() + ROW_TOP + index / columns * height,
                cell - 2, height - 1, label, () -> {
                    apply.run();
                    setMenu(Menu.NONE);
                    resetList();
                    revealSelected();
                    updateControls();
                }, chosen);
        option.visible = option.active = false;
        options.add(option);
    }

    private void setMenu(Menu target) {
        menu = target;
        for (var button : states) button.active = button.visible = target == Menu.STATE;
        for (var button : regions) button.active = button.visible = target == Menu.REGION;
        scrollList(0);
    }

    private void routeArrow(int x, int y, int size, int step) {
        String key = step < 0 ? "previous_route" : "next_route";
        var button = addRenderableWidget(new BaiqiaoButton(x, y, size, size, text(key),
                widget -> cycleRoute(step), () -> false) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
                surface(g, false);
                HeavenlyFurnaceArt.icon(g, step < 0 ? "previous" : "next", getX() + 3, getY() + 3, size - 6);
            }
        });
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text(key)));
    }

    private void cycleRoute(int step) {
        List<String> keys = new ArrayList<>();
        keys.add("");
        keys.addAll(catalog().meridians().keySet());
        meridian = keys.get(Math.floorMod(keys.indexOf(meridian) + step, keys.size()));
        if (!meridian.isEmpty() && !meridian.equals(point().meridian())) {
            select(catalog().meridians().get(meridian).root());
        }
        resetList();
        revealSelected();
        updateControls();
    }

    private void changeView(View mode) {
        if (view == mode) return;
        view = mode;
        if (mode == View.MERIDIAN && meridian.isEmpty()) meridian = point().meridian();
        if (mode != View.BODY) back = catalog().markers().get(selected).get(0).back();
        panX = panY = 0;
        overlap = Set.of();
        refreshList();
        revealSelected();
        updateControls();
    }

    private void flipSide() {
        back = !back;
        panX = panY = 0;
        graphRevision++;
        updateControls();
    }

    private void changeTab(Tab value) {
        if (tab == value) return;
        endHold();
        tab = value;
        view = tab == Tab.ROUTES ? View.MERIDIAN : View.BODY;
        if (tab == Tab.ROUTES && point() != null) {
            if (meridian.isEmpty()) meridian = point().meridian();
            back = catalog().markers().get(point().id()).get(0).back();
        }
        overlap = Set.of();
        panX = panY = 0;
        offset = 0;
        layout();
    }

    private void resetList() {
        offset = 0;
        overlap = Set.of();
        refreshList();
    }

    private boolean matches(Point point) {
        if (!meridian.isEmpty() && !meridian.equals(point.meridian())) return false;
        if (zoneIndex != 0 && catalog().markers().get(point.id()).stream().noneMatch(marker -> marker.zone().equals(ZONES[zoneIndex])))
            return false;
        if (!query.isBlank() && !point.name().contains(query.strip())
                && !point.id().contains(query.strip().toUpperCase(Locale.ROOT))) return false;
        var allowed = progress().eligibility(point, stage());
        return switch (filter) {
            case ALL -> true;
            case OPEN -> allowed == AcupointProgress.Result.ALREADY_OPEN;
            case READY -> allowed == AcupointProgress.Result.IDLE;
            case LOCKED -> allowed == AcupointProgress.Result.PREREQUISITE || allowed == AcupointProgress.Result.REALM_LOCKED;
        };
    }

    private void refreshList() {
        if (!graphTab() || data() == null) return;
        var route = catalog().routes().get(meridian);
        Collection<Point> candidates = route == null ? catalog().points().values() : route;
        List<Point> matches = candidates.stream().filter(this::matches).toList();
        Set<String> targets = new HashSet<>();
        matches.forEach(point -> targets.add(point.id()));
        mapTargets = Set.copyOf(targets);
        visiblePoints = matches.stream().filter(point -> overlap.isEmpty() || overlap.contains(point.id()))
                .sorted(Comparator.comparing(Point::meridian).thenComparingInt(Point::index)).toList();
        graphRevision++;
        scrollList(0);
    }

    private void scrollList(int delta) {
        offset = Mth.clamp(offset + delta, 0, Math.max(0, visiblePoints.size() - rows.size()));
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            row.visible = row.active = menu == Menu.NONE && offset + i < visiblePoints.size();
            if (row.visible) {
                var point = visiblePoints.get(offset + i);
                row.setMessage(Component.literal(point.name() + " · " + point.id()));
                row.setTooltip(net.minecraft.client.gui.components.Tooltip.create(status(point)));
            }
        }
    }

    private void revealSelected() {
        for (int i = 0; i < visiblePoints.size(); i++) {
            if (!selected.equals(visiblePoints.get(i).id())) continue;
            if (i < offset || i >= offset + rows.size()) scrollList(i - offset - 2);
            return;
        }
    }

    private void select(String id) {
        if (id.equals(selected)) return;
        endHold();
        selected = id;
        if (!dualBody()) {
            back = catalog().markers().get(id).get(0).back();
            panX = panY = 0;
            graphRevision++;
        }
        selectedDot = selectedDot();
        revealSelected();
        request(SELECT, id);
        result = new ResultView(AcupointProgress.Result.IDLE, 0);
        updateControls();
    }

    private boolean autoHere() {
        return progress().auto() && progress().autoMeridian().equals(autoMeridian());
    }

    private String autoMeridian() {
        return !meridian.isEmpty() ? meridian : point() == null ? "" : point().meridian();
    }

    private void updateControls() {
        if (!graphTab() || tap == null) return;
        side.visible = side.active = !dualBody();
        side.setMessage(text(back ? "back_view" : "front_view"));
        Meridian line = catalog().meridians().get(meridian);
        lineFilter.setMessage(line == null ? text("all_routes") : Component.literal(line.name()));
        var state = text("filter." + id(filter));
        var region = text("zone." + (zoneIndex == 0 ? "all" : ZONES[zoneIndex]));
        stateFilter.setMessage(filter == Filter.ALL && font.width(state) > stateFilter.getWidth() - 13
                ? text("filter.any_short") : state);
        zoneFilter.setMessage(zoneIndex == 0 && font.width(region) > zoneFilter.getWidth() - 13
                ? text("zone.any_short") : region);
        boolean allowed = progress().eligibility(point(), stage()) == AcupointProgress.Result.IDLE;
        tap.active = hold.active = allowed && (active.isEmpty() || active.equals(holdPoint));
        auto.active = !autoMeridian().isEmpty();
        Meridian automatic = catalog().meridians().get(progress().autoMeridian());
        boolean running = progress().auto() && automatic != null;
        Component automaticState = running
                ? text(data().wuxingxiantu$isMeditating() || !catalog().charge().autoOnlyDuringMeditation() ? "auto_running" : "auto_waiting",
                automatic.name()) : text("auto_off");
        if (!automaticState.equals(auto.getMessage())) {
            auto.setMessage(automaticState);
            auto.setTooltip(running ? net.minecraft.client.gui.components.Tooltip.create(automaticState) : null);
        }
    }

    private void beginHold(boolean mouse, int key) {
        if (!holdPoint.isEmpty()) return;
        holdPoint = selected;
        holdMouse = mouse;
        holdKey = key;
        request(START, holdPoint);
    }

    private void endHold() {
        if (holdPoint.isEmpty()) return;
        holdPoint = "";
        holdKey = -1;
        request(STOP, "");
    }

    private void request(AcupointPackets.Action action, String id) {
        if (data() != null) AcupointClient.send(action, id, catalog().version());
    }

    void accept(AcupointPackets.State message) {
        active = message.activePoint();
        if (!message.result().equals(AcupointProgress.Result.IDLE)) result = new ResultView(message.result(), age + 55);
        if (shownCatalog != catalog()) {
            shownCatalog = catalog();
            detailPoint = null;
            if (point() == null) selected = catalog().points().keySet().iterator().next();
            if (!catalog().meridians().containsKey(meridian)) meridian = "";
            if (frame != null) layout();
        }
        if (progress().opened(holdPoint)) endHold();
        refreshList();
        updateControls();
    }

    @Override public void tick() {
        if (data() == null || frame == null) return;
        age++;
        if (shownStage != stage()) {
            shownStage = stage();
            refreshList();
        }
        if (!holdPoint.isEmpty() && age % 10 == 0) request(RENEW, holdPoint);
        updateControls();
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void removed() { endHold(); }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        if (frame == null || data() == null) return;
        CultivationUi.background(g, "overview", frame.x(), frame.y(), frame.width(), frame.height());
        g.fill(frame.x() + 1, frame.y() + 1, frame.right() - 1, frame.bottom() - 1, 0x40101B2C);
        CultivationUi.ornamentFrame(g, frame.x(), frame.y(), frame.width(), frame.height());
        heading(g, title, frame.x() + 9, frame.y() + 9, 1.12F);
        rule(g, frame.x() + 6, left.y() - 4, frame.width() - 12);
        if (graphTab()) {
            panel(g, left);
            panel(g, graph);
            panel(g, right);
            drawIndex(g);
            drawGraph(g, mx, my, partial);
            drawDetail(g, mx, my);
        } else {
            drawSummary(g, mx, my);
        }
        drawFooter(g);
        super.render(g, mx, my, partial);
    }

    private void panel(GuiGraphics g, Box box) {
        AlchemyChrome.surface(g, box.x(), box.y(), box.width(), box.height(), 0xAD9A7D4E, 0xAF0C1E2B, 0x800A1A25);
    }

    private void drawIndex(GuiGraphics g) {
        g.drawCenteredString(font, text(overlap.isEmpty() ? "index" : "overlap"), left.x() + left.width() / 2,
                left.y() + 7, GOLD);
        rule(g, left.x() + 4, left.y() + 17, left.width() - 8);
        Box input = new Box(left.x() + 4, left.y() + 19, left.width() - 8, 17);
        g.fill(input.x(), input.y(), input.right(), input.bottom(), 0xC10B1720);
        g.renderOutline(input.x(), input.y(), input.width(), input.height(), search.isFocused() ? GOLD : RAIL);
        HeavenlyFurnaceArt.icon(g, "search_icon", input.x() + 3, input.y() + 4, 9);
        if (menu != Menu.NONE) return;
        if (visiblePoints.isEmpty()) {
            label(g, text("empty"), left.x() + 6, left.y() + ROW_TOP + 2, left.width() - 12, CultivationUi.MUTED);
        }
        int count = visiblePoints.size();
        if (count <= rows.size()) return;
        Box rail = listRail();
        int thumb = listThumbHeight(rail);
        int start = rail.y() + (rail.height() - thumb) * offset / (count - rows.size());
        g.fill(rail.x() + 2, rail.y(), rail.right() - 1, rail.bottom(), 0x70576974);
        ForgedUi.scrollThumb(g, rail.x() + 1, start, rail.width() - 1, thumb, true);
    }

    private Box listRail() {
        return new Box(left.right() - 9, left.y() + ROW_TOP, 6, rows.size() * rowHeight - 2);
    }

    private int listThumbHeight(Box rail) {
        return Math.max(7, rail.height() * rows.size() / visiblePoints.size());
    }

    private void dragList(double y) {
        Box rail = listRail();
        int thumb = listThumbHeight(rail);
        double proportion = (y - rail.y() - thumb / 2.0) / (rail.height() - thumb);
        int target = (int) Math.round(Mth.clamp(proportion, 0, 1) * (visiblePoints.size() - rows.size()));
        scrollList(target - offset);
    }

    private void drawGraph(GuiGraphics g, int mx, int my, float partial) {
        var area = mapArea();
        String highlight = !meridian.isEmpty() ? meridian : point().meridian();
        if (renderedRevision != graphRevision) {
            graphViews = projectors(area);
            boolean contextual = view != View.MERIDIAN && query.isBlank() && filter == Filter.ALL && zoneIndex == 0;
            projected = project(catalog(), graphViews, point -> contextual || mapTargets.contains(point.id())
                    && (view != View.MERIDIAN || highlight.equals(point.meridian())));
            selectedDot = selectedDot();
            renderedRevision = graphRevision;
        }
        rule(g, area.x(), area.y() - 2, area.width());
        g.enableScissor(area.x(), area.y(), area.right(), area.bottom());
        backdrop(g, area, graphViews);
        for (var projector : graphViews) skin(g, minecraft.player, catalog(), projector);
        graph(g, projected, progress(), stage(), selected, active, highlight, true, (age + partial) / 20.0);
        if (selectedDot != null && area.contains(selectedDot.x(), selectedDot.y()) && mapTargets.contains(selected)) {
            drawCallout(g, area, selectedDot);
        }
        g.disableScissor();
        if (dualBody()) {
            for (var projector : graphViews) {
                g.drawCenteredString(font, text(projector.back() ? "back_view" : "front_view"), (int) projector.x(8),
                        graph.bottom() - 31, CultivationUi.TEXT);
            }
        }
        drawLegend(g);
        if (area.contains(mx, my) && !dragging) {
            Dot hovered = nearestDot(mx, my);
            if (hovered != null) {
                Point point = hovered.point();
                setTooltipForNextRenderPass(font.split(Component.literal(point.name() + " · " + point.id() + "  ").append(status(point)), 190));
            }
        }
    }

    private Dot selectedDot() {
        for (Dot dot : projected.dots()) {
            if (dot.point().id().equals(selected)) return dot;
        }
        return null;
    }

    private void drawCallout(GuiGraphics g, Box area, Dot dot) {
        int x = (int) dot.x(), y = (int) dot.y();
        String name = dot.point().name();
        int w = font.width(name) + 10;
        int labelX = x + 15 + w < area.right() ? x + 15 : x - w - 15;
        labelX = Mth.clamp(labelX, area.x(), Math.max(area.x(), area.right() - w));
        int labelY = Mth.clamp(y - 6, area.y(), area.bottom() - 13);
        int connectorStart = labelX > x ? x + 4 : labelX + w;
        int connectorEnd = labelX > x ? labelX : x - 4;
        if (connectorEnd > connectorStart) g.fill(connectorStart, y, connectorEnd, y + 1, GOLD);
        AlchemyChrome.surface(g, labelX, labelY, w, 13, GOLD, 0xF527221A, 0xF5433523);
        g.drawString(font, name, labelX + 5, labelY + 3, GOLD, false);
    }

    private void drawLegend(GuiGraphics g) {
        int x = graph.x() + 4, y = graph.bottom() - 14, w = (graph.width() - 8) / 3;
        legendItem(g, text("legend.ready"), x, y, w, BLUE, true);
        legendItem(g, text("legend.open"), x + w, y, w, GOLD, false);
        legendItem(g, text("legend.locked"), x + 2 * w, y, w, GRAY, false);
    }

    private void legendItem(GuiGraphics g, Component text, int x, int y, int w, int tone, boolean hollow) {
        x += Math.max(0, (w - font.width(text) - 8) / 2);
        if (hollow) g.renderOutline(x, y + 2, 5, 5, tone);
        else g.fill(x, y + 2, x + 5, y + 7, tone);
        label(g, text, x + 8, y, w - 8, CultivationUi.TEXT);
    }

    private Box mapArea() {
        return new Box(graph.x() + 4, graph.y() + 25, graph.width() - 8, graph.height() - 61);
    }

    private boolean dualBody() {
        return view == View.BODY && mapArea().width() >= 145;
    }

    private List<Projector> projectors(Box area) {
        double centerY = area.y() + area.height() / 2.0;
        if (dualBody()) {
            double unit = Math.min((area.width() / 2.0 - 7) / 16, (area.height() - 4) / 32.0);
            return List.of(new Projector(area.x() + area.width() / 4.0 - 8 * unit, centerY - 16 * unit, unit, false),
                    new Projector(area.x() + area.width() * .75 - 8 * unit, centerY - 16 * unit, unit, true));
        }
        double unit = Math.min((area.width() - 6) / 16.0, (area.height() - 4) / 32.0);
        double anchorX = 8, anchorY = 16;
        if (view == View.DETAIL) {
            unit *= zoom;
            for (Marker marker : catalog().markers().get(selected)) {
                if (marker.back() != back) continue;
                anchorX = marker.x();
                anchorY = marker.y();
                break;
            }
        }
        return List.of(new Projector(area.x() + area.width() / 2.0 - anchorX * unit + panX,
                centerY - anchorY * unit + panY, unit, back));
    }

    private Dot nearestDot(double mx, double my) {
        double radius = view == View.DETAIL ? 8 : 3.5;
        double nearestDistance = radius * radius;
        Dot nearest = null;
        for (Dot dot : projected.dots()) {
            if (!mapTargets.contains(dot.point().id())) continue;
            double dx = dot.x() - mx, dy = dot.y() - my;
            double distance = dx * dx + dy * dy;
            if (distance < nearestDistance || nearest == null && distance == nearestDistance) {
                nearest = dot;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private List<Dot> hits(double mx, double my) {
        double radius = view == View.DETAIL ? 8 : 3.5;
        return projected.dots().stream().filter(dot -> mapTargets.contains(dot.point().id()))
                .filter(dot -> Math.hypot(dot.x() - mx, dot.y() - my) <= radius)
                .sorted(Comparator.comparingDouble(dot -> Math.hypot(dot.x() - mx, dot.y() - my))).toList();
    }

    private void prepareDetail(Point point, Meridian line) {
        if (detailPoint == point) return;
        detailPoint = point;
        rewardOffset = 0;
        pointTitle = Component.literal(point.name() + " · " + point.id());
        routeTitle = Component.literal(line.name()).append(" / ").append(text("tier." + id(point.tier())));
        realm = text("required_realm", catalog().stageNames().get(point.stage() - 1));
        prerequisites = text("prerequisite", point.prerequisites().stream()
                .map(id -> catalog().points().get(id).name() + " " + id).reduce((a, b) -> a + "、" + b).orElse("—"));
        pointReward = reward(point.stat(), point.bonus());
        routeRewards = line.extraRewards().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> reward(entry.getKey(), entry.getValue())).toList();
    }

    private void drawDetail(GuiGraphics g, int mx, int my) {
        int x = right.x() + 6, y = right.y() + 8, w = right.width() - 12;
        Point point = point();
        if (point == null) return;
        Meridian line = catalog().meridians().get(point.meridian());
        prepareDetail(point, line);
        int step = Mth.clamp((right.height() - 64) / (9 + Math.min(4, routeRewards.size())), 9, 20);
        int section = Math.max(10, step);
        heading(g, pointTitle, x, y - 1, 1.10F);
        rule(g, x, y + 9, w);
        y += section;
        label(g, routeTitle, x, y, w, CultivationUi.TEXT);
        y += section;
        detailLabel(g, realm, x, y, w, CultivationUi.MUTED, mx, my);
        y += step;
        detailLabel(g, prerequisites, x, y, w, CultivationUi.MUTED, mx, my);
        y += step + 2;
        int rewardHeight = Math.max(13, step + 3);
        AlchemyChrome.surface(g, x, y, w, rewardHeight, 0xE4D2AA63, 0x9C22231D, 0x883E3222);
        drawReward(g, pointReward, x + 4, y + (rewardHeight - 8) / 2, w - 8, GOLD, mx, my);
        y += rewardHeight + 3;

        int invested = progress().invested(point);
        Component state = age < result.until() ? text("result." + id(result.value())) : status(point);
        int stateWidth = font.width(state);
        int investmentWidth = Math.max(w * 3 / 5, w - stateWidth - 4);
        var investment = text("investment", invested, point.cost());
        if (font.width(investment) > investmentWidth) investment = text("investment_compact", invested, point.cost());
        detailLabel(g, investment, x, y, investmentWidth, CultivationUi.TEXT, mx, my);
        detailLabel(g, state, x + investmentWidth + 3, y, w - investmentWidth - 3, color(progress(), point, stage()), mx, my);
        drawSegments(g, x, y + 11, w, invested, point.cost());
        y += section + 7;
        rule(g, x, y - 3, w);
        detailLabel(g, text("route_progress_named", line.name(), progress().bonuses().count(line.id()), line.count()),
                x, y, w, CultivationUi.TEXT, mx, my);
        y += section;
        rule(g, x, y - 2, w);
        if (!routeRewards.isEmpty()) {
            rewardRows = Mth.clamp((right.bottom() - 39 - y - step) / step, 1, routeRewards.size());
            rewardOffset = Mth.clamp(rewardOffset, 0, routeRewards.size() - rewardRows);
            boolean scrolling = rewardRows < routeRewards.size();
            Component heading = scrolling ? text("route_extras_scroll", rewardOffset + 1, rewardOffset + rewardRows, routeRewards.size())
                    : text("route_extras");
            detailLabel(g, heading, x, y, w, GOLD, mx, my);
            y += step;
            for (int i = rewardOffset; i < rewardOffset + rewardRows; i++) {
                drawReward(g, routeRewards.get(i), x + 2, y, w - 4, CultivationUi.TEXT, mx, my);
                if (step >= 12) rule(g, x, y + step - 3, w);
                y += step;
            }
        } else {
            rewardRows = 0;
        }
        rule(g, x, right.bottom() - 39, w);
    }

    private void drawSegments(GuiGraphics g, int x, int y, int w, int value, int max) {
        int segments = Math.min(21, w / 4);
        for (int i = 0; i < segments; i++) {
            int start = x + i * w / segments;
            int end = x + (i + 1) * w / segments - 1;
            g.fill(start, y, end, y + 4, 0xFF384553);
            double fraction = Mth.clamp(value * (double) segments / max - i, 0, 1);
            if (fraction > 0) g.fill(start, y, start + (int) Math.ceil((end - start) * fraction), y + 4, BLUE);
        }
    }

    private void drawReward(GuiGraphics g, Reward reward, int x, int y, int w, int color, int mx, int my) {
        int valueWidth = font.width(reward.value());
        detailLabel(g, reward.name(), x, y, w - valueWidth - 4, color, mx, my);
        g.drawString(font, reward.value(), x + w - valueWidth, y, BLUE, false);
    }

    private void detailLabel(GuiGraphics g, Component text, int x, int y, int w, int color, int mx, int my) {
        CultivationUi.fit(g, text, x, y, w, color, mx, my);
    }

    private static void rule(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, RAIL);
    }

    private void drawSummary(GuiGraphics g, int mx, int my) {
        Box panel = new Box(frame.x() + 6, left.y(), frame.width() - 12, left.height());
        panel(g, panel);
        var bonuses = progress().bonuses();
        int x = panel.x() + 10, y = panel.y() + 12, w = panel.width() - 20;
        if (tab == Tab.CYCLES) {
            label(g, text("minor_cycle", text(bonuses.minor() ? "complete" : "incomplete")), x, y, w, GOLD);
            label(g, text("minor_requires", catalog().minorRequires().stream().map(id -> catalog().meridians().get(id).name())
                    .reduce((a, b) -> a + "、" + b).orElse("")), x, y + 15, w, CultivationUi.TEXT);
            label(g, rewards(catalog().minorReward()), x, y + 30, w, BLUE);
            label(g, text("major_cycle", text(bonuses.major() ? "complete" : "incomplete")), x, y + 55, w, GOLD);
            label(g, text("major_requires", bonuses.total(), catalog().points().size()), x, y + 70, w, CultivationUi.TEXT);
            label(g, rewards(catalog().majorReward()), x, y + 85, w, BLUE);
            return;
        }
        int column = (w - 10) / 2;
        var stats = Stat.values();
        int rowHeight = Mth.clamp((panel.height() - 20) / ((stats.length + 1) / 2), 10, 18);
        for (int i = 0; i < stats.length; i++) {
            Stat stat = stats[i];
            int statX = x + i % 2 * (column + 10), statY = y + i / 2 * rowHeight;
            label(g, stat(stat, bonuses.get(stat)), statX, statY, column, bonuses.get(stat) > 0 ? BLUE : CultivationUi.TEXT);
            if (new Box(statX, statY, column, rowHeight).contains(mx, my)
                    && (stat == Stat.CRIT_CHANCE_PCT || stat == Stat.CRIT_DAMAGE_PCT || stat == Stat.ATTACK_SPEED_PCT))
                setTooltipForNextRenderPass(font.split(text("help." + stat.key()), 210));
        }
    }

    private Component rewards(Map<Stat, Double> rewards) {
        var line = Component.empty();
        rewards.forEach((stat, value) -> {
            if (!line.getSiblings().isEmpty()) line.append("  ·  ");
            line.append(stat(stat, value));
        });
        return line;
    }

    private static Component stat(Stat stat, double value) {
        return text("stat_value", text("stat." + stat.key()), formatStat(stat, value));
    }

    private static Reward reward(Stat stat, double value) {
        return new Reward(text("stat." + stat.key()), (value >= 0 ? "+" : "") + formatStat(stat, value));
    }

    private static String formatStat(Stat stat, double value) {
        return String.format(Locale.ROOT, "%.2f", value) + (stat.percent() ? "%" : "");
    }

    private Component status(Point point) {
        var eligibility = progress().eligibility(point, stage());
        if (point.id().equals(active)) return text("charging");
        return text("result." + id(eligibility));
    }

    private void drawFooter(GuiGraphics g) {
        int x = frame.x() + 9, y = frame.bottom() - 15, w = (frame.width() - 18) / 3;
        var bonuses = progress().bonuses();
        rule(g, x, y - 4, w * 3);
        g.fill(x + w, y, x + w + 1, y + 9, RAIL);
        g.fill(x + w * 2, y, x + w * 2 + 1, y + 9, RAIL);
        footerValue(g, text("mana", (int) data().getCurrentMana(), (int) data().getMaxMana()), x, y, w, BLUE);
        footerValue(g, text("point_count", bonuses.total(), catalog().points().size()), x + w, y, w, GOLD);
        footerValue(g, text("meridian_count", bonuses.complete(), catalog().meridians().size()), x + w * 2, y, w, GOLD);
    }

    private void footerValue(GuiGraphics g, Component value, int x, int y, int w, int color) {
        int inset = Math.max(3, (w - font.width(value)) / 2);
        label(g, value, x + inset, y, w - inset - 3, color);
    }

    private void heading(GuiGraphics g, Component label, int x, int y, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, label.copy().withStyle(net.minecraft.ChatFormatting.BOLD), 0, 0, GOLD, false);
        g.pose().popPose();
    }

    private void label(GuiGraphics g, Component text, int x, int y, int w, int color) {
        CultivationUi.fit(g, text, x, y, w, color, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (menu != Menu.NONE && !left.contains(mx, my)) setMenu(Menu.NONE);
        if (super.mouseClicked(mx, my, button)) return true;
        if (menu != Menu.NONE) {
            setMenu(Menu.NONE);
            return true;
        }
        if (!graphTab() || graph == null) return false;
        if (button == 0 && visiblePoints.size() > rows.size() && listRail().contains(mx, my)) {
            draggingList = true;
            dragList(my);
            return true;
        }
        if (!mapArea().contains(mx, my)) return false;
        if (button == 1 && view == View.DETAIL) {
            dragging = true;
            return true;
        }
        if (button != 0) return false;
        var hits = hits(mx, my);
        if (hits.isEmpty()) {
            if (!overlap.isEmpty()) resetList();
            return true;
        }
        select(hits.get(0).point().id());
        Set<String> candidates = new LinkedHashSet<>();
        for (Dot dot : hits) candidates.add(dot.point().id());
        overlap = candidates.size() > 1 ? Set.copyOf(candidates) : Set.of();
        offset = 0;
        refreshList();
        revealSelected();
        return true;
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && holdMouse) endHold();
        if (button == 0 && draggingList) { draggingList = false; return true; }
        if (button == 1 && dragging) { dragging = false; return true; }
        return super.mouseReleased(mx, my, button);
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingList && button == 0 && visiblePoints.size() > rows.size()) {
            dragList(my);
            return true;
        }
        if (dragging && view == View.DETAIL) {
            panX = Mth.clamp(panX + dx, -graph.width() * zoom / 2, graph.width() * zoom / 2);
            panY = Mth.clamp(panY + dy, -graph.height() * zoom / 2, graph.height() * zoom / 2);
            graphRevision++;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override public boolean mouseScrolled(double mx, double my, double amount) {
        if (graphTab() && left != null && left.contains(mx, my)) {
            if (menu == Menu.NONE) scrollList(amount > 0 ? -3 : 3);
            return true;
        }
        if (graphTab() && graph != null && view == View.DETAIL && mapArea().contains(mx, my)) {
            zoom = Mth.clamp(zoom + (amount > 0 ? .25 : -.25), 2, 4);
            graphRevision++;
            return true;
        }
        if (graphTab() && right != null && right.contains(mx, my) && rewardRows < routeRewards.size()) {
            rewardOffset = Mth.clamp(rewardOffset + (amount > 0 ? -1 : 1), 0, routeRewards.size() - rewardRows);
            return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && menu != Menu.NONE) {
            setMenu(Menu.NONE);
            setFocused(null);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        if (key == holdKey) endHold();
        return super.keyReleased(key, scan, modifiers);
    }

    private static String id(Enum<?> value) { return value.name().toLowerCase(Locale.ROOT); }
}
