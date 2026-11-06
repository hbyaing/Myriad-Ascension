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

    private static final String[] ZONES = {"", "head", "neck", "torso", "arm", "hand", "leg", "foot"};
    private static final int ROW_HEIGHT = 15;
    private static final String KEY = "gui.wuxingxiantu.baiqiao.";

    private final Screen parent;
    private final List<CultivationUi.Action> rows = new ArrayList<>();
    private Tab tab = Tab.OVERVIEW;
    private View view = View.BODY;
    private Filter filter = Filter.ALL;
    private String meridian = "", selected = "", active = "", holdPoint = "", query = "";
    private ResultView result = new ResultView(AcupointProgress.Result.IDLE, 0);
    private Set<String> overlap = Set.of();
    private List<Point> visiblePoints = List.of();
    private Topology projected = Topology.EMPTY;
    private List<Projector> graphViews = List.of();
    private Data shownCatalog;
    private boolean back, dragging, initialized;
    private boolean holdMouse;
    private int zoneIndex, offset, graphRevision, renderedRevision = -1, age, holdKey = -1, shownStage = -1;
    private double zoom = 2, panX, panY;
    private Box frame, left, graph, right;
    private CultivationUi.Action lineFilter, stateFilter, zoneFilter, side, tap, hold, auto;
    private ForgedSearchBox search;

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
        projected = Topology.EMPTY;
        renderedRevision = -1;
        int w = Math.min(900, width - 12), h = Math.min(520, height - 12);
        frame = new Box((width - w) / 2, (height - h) / 2, w, h);
        int x = frame.x() + 6, y = frame.y() + 45, bottom = frame.bottom() - 22;
        int l = Mth.clamp(w / 4, 86, 145), r = Mth.clamp(w * 30 / 100, 105, 205);
        left = new Box(x, y, l, bottom - y);
        graph = new Box(x + l + 3, y, w - l - r - 18, bottom - y);
        right = new Box(graph.right() + 3, y, r, bottom - y);
        action(frame.right() - 70, frame.y() + 5, 64, 16, text("back"), this::onClose, () -> false);
        int tabWidth = (w - 15) / 4;
        for (Tab value : Tab.values()) {
            addRenderableWidget(new CultivationUi.Tab(x + value.ordinal() * tabWidth, frame.y() + 25, tabWidth - 3, 16,
                    text("tab." + id(value)), button -> changeTab(value), () -> tab == value));
        }
        if (!graphTab()) return;

        int inset = left.x() + 4, available = left.width() - 8;
        search = addRenderableWidget(new ForgedSearchBox(font, inset + 3, y + 4, available - 6, 16, text("search")));
        search.setMaxLength(32);
        search.setHint(text("search"));
        search.setTextColor(CultivationUi.TEXT);
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            resetList();
        });

        lineFilter = action(inset, y + 23, available, 15, text("all_routes"), () -> {
            List<String> keys = new ArrayList<>();
            keys.add("");
            keys.addAll(catalog().meridians().keySet());
            int step = hasShiftDown() ? -1 : 1;
            meridian = keys.get(Math.floorMod(keys.indexOf(meridian) + step, keys.size()));
            resetList();
        }, () -> !meridian.isEmpty());
        lineFilter.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text("route_filter_hint")));
        int half = (available - 3) / 2;
        stateFilter = action(inset, y + 40, half, 15, text("filter.all"), () -> {
            filter = Filter.values()[(filter.ordinal() + 1) % Filter.values().length];
            resetList();
        }, () -> filter != Filter.ALL);
        zoneFilter = action(inset + half + 3, y + 40, available - half - 3, 15, text("zone.all"), () -> {
            zoneIndex = (zoneIndex + 1) % ZONES.length;
            resetList();
        }, () -> zoneIndex != 0);
        for (int row = 0; row < Math.max(1, (left.height() - 80) / ROW_HEIGHT); row++) {
            final int index = row;
            var item = action(inset, y + 58 + row * ROW_HEIGHT, available, ROW_HEIGHT - 1, Component.empty(),
                    () -> select(visiblePoints.get(offset + index).id()),
                    () -> offset + index < visiblePoints.size() && selected.equals(visiblePoints.get(offset + index).id()));
            item.customSurface = (g, widget) -> {
                boolean selectedRow = offset + index < visiblePoints.size() && selected.equals(visiblePoints.get(offset + index).id());
                int rowX = widget.getX(), rowY = widget.getY();
                int edge = selectedRow ? GOLD : widget.isHoveredOrFocused() ? 0xCFA18453 : 0x364C6272;
                g.fill(rowX, rowY, rowX + widget.getWidth(), rowY + widget.getHeight(), selectedRow ? 0xB2473827 : 0x70314250);
                g.fill(rowX, rowY, rowX + 1, rowY + widget.getHeight(), edge);
                g.fill(rowX, rowY + widget.getHeight() - 1, rowX + widget.getWidth(), rowY + widget.getHeight(), edge);
            };
            rows.add(item);
        }
        action(inset, left.bottom() - 18, half, 14, text("previous"), () -> scrollList(-rows.size()), () -> false);
        action(inset + half + 3, left.bottom() - 18, available - half - 3, 14,
                text("next"), () -> scrollList(rows.size()), () -> false);

        int small = (graph.width() - 10) / 2;
        action(graph.x() + 3, graph.y() + 4, small, 15, text("view." + id(view)), () -> {
            view = View.values()[(view.ordinal() + 1) % View.values().length];
            if (view == View.MERIDIAN && meridian.isEmpty() && point() != null) meridian = point().meridian();
            if (view != View.BODY && point() != null) back = catalog().markers().get(point().id()).get(0).back();
            panX = panY = 0;
            resetList();
            layout();
        }, () -> view != View.BODY);
        side = action(graph.x() + small + 6, graph.y() + 4, graph.width() - small - 9, 15,
                text(back ? "back_view" : "front_view"), () -> {
                    back = !back;
                    panX = panY = 0;
                    graphRevision++;
                    side.setMessage(text(back ? "back_view" : "front_view"));
                }, () -> back);
        int actionWidth = right.width() - 8;
        tap = action(right.x() + 4, right.bottom() - 52, actionWidth, 15, text("tap"),
                () -> request(TAP, selected), () -> false);
        hold = addRenderableWidget(new CultivationUi.Action(right.x() + 4, right.bottom() - 35, actionWidth, 15,
                text("hold"), button -> {}, () -> selected.equals(active) && !active.isEmpty()) {
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
        auto = action(right.x() + 4, right.bottom() - 18, actionWidth, 15, text("auto_off"), () -> {
            boolean same = progress().auto() && progress().autoMeridian().equals(autoMeridian());
            request(same ? AUTO_OFF : AUTO_ON, autoMeridian());
        }, () -> progress().auto() && progress().autoMeridian().equals(autoMeridian()));
        refreshList();
        updateControls();
    }

    private CultivationUi.Action action(int x, int y, int w, int h, Component label, Runnable run, BooleanSupplier chosen) {
        return addRenderableWidget(new CultivationUi.Action(x, y, w, h, label, button -> run.run(), chosen));
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
        visiblePoints = candidates.stream().filter(this::matches)
                .filter(point -> overlap.isEmpty() || overlap.contains(point.id())).toList();
        graphRevision++;
        scrollList(0);
    }

    private void scrollList(int delta) {
        offset = Mth.clamp(offset + delta, 0, Math.max(0, visiblePoints.size() - rows.size()));
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            row.visible = row.active = offset + i < visiblePoints.size();
            if (row.visible) {
                var point = visiblePoints.get(offset + i);
                row.setMessage(Component.literal(point.name() + " · " + point.id()));
                row.setTooltip(net.minecraft.client.gui.components.Tooltip.create(status(point)));
            }
        }
    }

    private void select(String id) {
        if (id.equals(selected)) return;
        endHold();
        selected = id;
        if (view != View.BODY && point() != null) {
            back = catalog().markers().get(id).get(0).back();
            side.setMessage(text(back ? "back_view" : "front_view"));
            graphRevision++;
        }
        request(SELECT, id);
        result = new ResultView(AcupointProgress.Result.IDLE, 0);
        updateControls();
    }

    private String autoMeridian() {
        return !meridian.isEmpty() ? meridian : point() == null ? "" : point().meridian();
    }

    private void updateControls() {
        if (!graphTab() || tap == null) return;
        Meridian line = catalog().meridians().get(meridian);
        lineFilter.setMessage(line == null ? text("all_routes") : Component.literal(line.name()));
        stateFilter.setMessage(text("filter." + id(filter)));
        zoneFilter.setMessage(text("zone." + (zoneIndex == 0 ? "all" : ZONES[zoneIndex])));
        boolean allowed = progress().eligibility(point(), stage()) == AcupointProgress.Result.IDLE;
        tap.active = hold.active = allowed && (active.isEmpty() || active.equals(holdPoint));
        auto.active = !autoMeridian().isEmpty();
        auto.setMessage(text(progress().auto() && progress().autoMeridian().equals(autoMeridian()) ? "auto_on" : "auto_off"));
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
        g.drawString(font, title, frame.x() + 9, frame.y() + 10, CultivationUi.GOLD, false);
        if (graphTab()) {
            panel(g, left);
            panel(g, graph);
            panel(g, right);
            Box input = new Box(left.x() + 4, left.y() + 3, left.width() - 8, 17);
            panel(g, input);
            if (search.isFocused()) g.renderOutline(input.x(), input.y(), input.width(), input.height(), CultivationUi.GOLD);
            if (visiblePoints.isEmpty()) label(g, text("empty"), left.x() + 6, left.y() + 62, left.width() - 12, CultivationUi.MUTED);
            if (!overlap.isEmpty()) label(g, text("overlap"), left.x() + 6, left.bottom() - 28, left.width() - 12, BLUE);
            drawGraph(g, mx, my, partial);
            drawDetail(g, mx, my);
        } else {
            drawSummary(g, mx, my);
        }
        drawFooter(g);
        super.render(g, mx, my, partial);
    }

    private void panel(GuiGraphics g, Box box) {
        g.fill(box.x(), box.y(), box.right(), box.bottom(), 0x97111E29);
        g.renderOutline(box.x(), box.y(), box.width(), box.height(), 0x807D6948);
    }

    private void drawGraph(GuiGraphics g, int mx, int my, float partial) {
        var area = mapArea();
        if (renderedRevision != graphRevision) {
            graphViews = projectors(area);
            projected = project(catalog(), graphViews, this::matches);
            renderedRevision = graphRevision;
        }
        var views = graphViews;
        g.enableScissor(area.x(), area.y(), area.right(), area.bottom());
        backdrop(g, area, views);
        for (var projector : views) skin(g, minecraft.player, catalog(), projector);
        String highlight = !meridian.isEmpty() ? meridian : point() == null ? "" : point().meridian();
        graph(g, projected, progress(), stage(), selected, active, highlight, true, (age + partial) / 20.0);
        g.disableScissor();
        if (views.size() == 2) {
            for (var projector : views) {
                int center = (int) projector.x(8);
                g.drawCenteredString(font, text(projector.back() ? "back_view" : "front_view"), center, graph.bottom() - 12, CultivationUi.TEXT);
            }
        } else label(g, text(view == View.DETAIL ? "graph_hint" : back ? "back_view" : "front_view"),
                graph.x() + 4, graph.bottom() - 12, graph.width() - 8, CultivationUi.MUTED);
        if (area.contains(mx, my) && !dragging) {
            Dot hovered = nearestDot(mx, my);
            if (hovered != null) {
                Point point = hovered.point();
                setTooltipForNextRenderPass(font.split(Component.literal(point.name() + " · " + point.id() + "  ").append(status(point)), 190));
            }
        }
    }

    private Box mapArea() {
        return new Box(graph.x() + 3, graph.y() + 22, graph.width() - 6, graph.height() - 37);
    }

    private List<Projector> projectors(Box area) {
        double centerY = area.y() + area.height() / 2.0;
        if (view == View.BODY && area.width() >= 130) {
            double unit = Math.min((area.width() / 2.0 - 7) / 16, area.height() / 32.0);
            return List.of(new Projector(area.x() + area.width() / 4.0 - 8 * unit, centerY - 16 * unit, unit, false),
                    new Projector(area.x() + area.width() * .75 - 8 * unit, centerY - 16 * unit, unit, true));
        }
        double unit = Math.min((area.width() - 6) / 16.0, area.height() / 32.0);
        if (view == View.DETAIL) unit *= zoom;
        return List.of(new Projector(area.x() + area.width() / 2.0 - 8 * unit + panX,
                centerY - 16 * unit + panY, unit, back));
    }

    private Dot nearestDot(double mx, double my) {
        double radius = view == View.DETAIL ? 8 : 3.5;
        double nearestDistance = radius * radius;
        Dot nearest = null;
        for (Dot dot : projected.dots()) {
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
        return projected.dots().stream().filter(dot -> Math.hypot(dot.x() - mx, dot.y() - my) <= radius)
                .sorted(Comparator.comparingDouble(dot -> Math.hypot(dot.x() - mx, dot.y() - my))).toList();
    }

    private void drawDetail(GuiGraphics g, int mx, int my) {
        int x = right.x() + 6, y = right.y() + 7, w = right.width() - 12;
        Point point = point();
        if (point == null) return;
        Meridian line = catalog().meridians().get(point.meridian());
        label(g, Component.literal(point.name() + " · " + point.id()), x, y, w, GOLD);
        label(g, Component.literal(line.name()).append(" / ").append(text("tier." + id(point.tier()))), x, y + 12, w, CultivationUi.TEXT);
        label(g, text("required_realm", catalog().stageNames().get(point.stage() - 1)), x, y + 24, w, CultivationUi.TEXT);
        label(g, text("gain", stat(point.stat(), point.bonus())), x, y + 36, w, BLUE);
        Component prerequisites = Component.literal(point.prerequisites().stream()
                .map(id -> catalog().points().get(id).name() + " " + id).reduce((a, b) -> a + "、" + b).orElse("—"));
        label(g, text("prerequisite", prerequisites), x, y + 48, w, CultivationUi.TEXT);
        label(g, status(point), x, y + 60, w, color(progress(), point, stage()));
        int invested = progress().invested(point);
        label(g, text("investment", invested, point.cost()), x, y + 72, w, CultivationUi.TEXT);
        g.fill(x, y + 83, x + w, y + 86, 0xFF384553);
        g.fill(x, y + 83, x + (int) (w * (invested / (double) point.cost())), y + 86, 0xFF6EC4F2);
        int statusY = right.bottom() - 65;
        if (statusY >= y + 103 && !line.extraRewards().isEmpty()) {
            var extra = text("route_extra", rewards(line.extraRewards()));
            label(g, extra, x, statusY - 12, w, BLUE);
            if (new Box(x, statusY - 13, w, 11).contains(mx, my))
                setTooltipForNextRenderPass(font.split(extra, 220));
        } else if (new Box(x, y + 11, w, 11).contains(mx, my)) {
            setTooltipForNextRenderPass(font.split(text("route_extra", rewards(line.extraRewards())), 220));
        }
        if (statusY >= y + 90) {
            Component state = age < result.until() ? text("result." + id(result.value()))
                    : text("route_progress", progress().bonuses().count(line.id()), line.count());
            label(g, state, x, statusY, w, CultivationUi.MUTED);
        }
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
        return text("stat_value", text("stat." + stat.key()), String.format(Locale.ROOT, "%.2f", value) + (stat.percent() ? "%" : ""));
    }

    private Component status(Point point) {
        var eligibility = progress().eligibility(point, stage());
        if (point.id().equals(active)) return text("charging");
        return text("result." + id(eligibility));
    }

    private void drawFooter(GuiGraphics g) {
        int x = frame.x() + 9, y = frame.bottom() - 16, w = (frame.width() - 18) / 3;
        var bonuses = progress().bonuses();
        label(g, text("mana", (int) data().getCurrentMana(), (int) data().getMaxMana()), x, y, w, BLUE);
        label(g, text("opened", bonuses.total(), catalog().points().size(), bonuses.complete()), x + w, y, w, GOLD);
        String line = progress().autoMeridian();
        Meridian automatic = catalog().meridians().get(line);
        Component label = progress().auto() && automatic != null
                ? text(data().wuxingxiantu$isMeditating() || !catalog().charge().autoOnlyDuringMeditation() ? "auto_running" : "auto_waiting",
                automatic.name()) : text("auto_off");
        label(g, label, x + w * 2, y, w, CultivationUi.TEXT);
    }

    private void label(GuiGraphics g, Component text, int x, int y, int w, int color) {
        CultivationUi.fit(g, text, x, y, w, color, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (!graphTab() || graph == null || !mapArea().contains(mx, my)) return false;
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
        return true;
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && holdMouse) endHold();
        if (button == 1 && dragging) { dragging = false; return true; }
        return super.mouseReleased(mx, my, button);
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
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
            scrollList(amount > 0 ? -3 : 3);
            return true;
        }
        if (graphTab() && graph != null && view == View.DETAIL && mapArea().contains(mx, my)) {
            zoom = Mth.clamp(zoom + (amount > 0 ? .25 : -.25), 2, 4);
            graphRevision++;
            return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }

    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        if (key == holdKey) endHold();
        return super.keyReleased(key, scan, modifiers);
    }

    private static String id(Enum<?> value) { return value.name().toLowerCase(Locale.ROOT); }
}
