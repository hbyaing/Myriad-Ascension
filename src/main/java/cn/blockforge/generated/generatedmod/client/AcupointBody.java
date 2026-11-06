package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog.*;
import cn.blockforge.generated.generatedmod.acupoint.AcupointProgress;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.Predicate;

final class AcupointBody {
    static final int GOLD = 0xFFFFCF74;
    static final int BLUE = 0xFF77DCFF;
    static final int GRAY = 0xFF8B98A2;

    record Box(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
    }

    record Projector(double x, double y, double unit, boolean back) {
        double x(double value) { return x + value * unit; }
        double y(double value) { return y + value * unit; }
    }

    record Dot(Point point, Marker marker, double x, double y) {}
    record Edge(Dot from, Dot to, boolean shortLink) {}
    record Topology(List<Dot> dots, List<Edge> edges) {
        static final Topology EMPTY = new Topology(List.of(), List.of());
    }
    private static final String[] PARTS = {"head", "torso", "left_arm", "right_arm", "left_leg", "right_leg"};
    private static final int[][] FRONT = {{8, 8}, {20, 20}, {44, 20}, {36, 52}, {4, 20}, {20, 52}};
    private static final int[][] BACK = {{24, 8}, {32, 20}, {44, 52}, {52, 20}, {28, 52}, {12, 20}};

    static Topology project(Data catalog, List<Projector> projectors, Predicate<Point> filter) {
        List<Dot> dots = new ArrayList<>();
        for (Point point : catalog.points().values()) {
            if (!filter.test(point)) continue;
            for (Marker marker : catalog.markers().get(point.id())) {
                for (Projector projector : projectors) {
                    if (marker.back() == projector.back())
                        dots.add(new Dot(point, marker, projector.x(marker.x()), projector.y(marker.y())));
                }
            }
        }
        Map<String, Dot> paths = new HashMap<>();
        for (Dot dot : dots) paths.put(key(dot.point().id(), dot.marker()), dot);
        List<Edge> edges = new ArrayList<>();
        for (Dot dot : dots) {
            for (String previous : dot.point().prerequisites()) {
                Dot from = paths.get(key(previous, dot.marker()));
                if (from == null || (dot.point().id().equals("BL-40") && previous.equals("BL-41"))
                        || (dot.point().id().equals("BL-41") && previous.equals("BL-40"))) continue;
                edges.add(new Edge(from, dot, Math.hypot(dot.x() - from.x(), dot.y() - from.y()) < 24));
            }
        }
        return new Topology(List.copyOf(dots), List.copyOf(edges));
    }

    static int color(AcupointProgress state, Point point, int stage) {
        if (state.opened(point.id())) return GOLD;
        return state.eligibility(point, stage) == AcupointProgress.Result.IDLE ? BLUE : GRAY;
    }

    static void skin(GuiGraphics graphics, LocalPlayer player, Data catalog, Projector projector) {
        int[][] uvs = projector.back ? BACK : FRONT;
        RenderSystem.enableBlend();
        graphics.setColor(.60F, .66F, .75F, 1);
        for (int i = 0; i < PARTS.length; i++) {
            Region part = catalog.regions().get(PARTS[i]);
            int x = (int) Math.round(projector.x(part.x()));
            int y = (int) Math.round(projector.y(part.y()));
            int w = (int) Math.round(projector.x(part.x() + part.width())) - x;
            int h = (int) Math.round(projector.y(part.y() + part.height())) - y;
            int textureWidth = part.width();
            int u = uvs[i][0];
            if ((i == 2 || i == 3) && player.getModelName().equals("slim")) {
                textureWidth = 3;
                if (projector.back) u--;
            }
            graphics.blit(player.getSkinTextureLocation(), x, y, w, h,
                    u, uvs[i][1], textureWidth, part.height(), 64, 64);
        }
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    static void backdrop(GuiGraphics g, Box area, List<Projector> projectors) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var matrix = g.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        double radius = Math.min(area.width() / (projectors.size() == 2 ? 4.5 : 2.25), area.height() * .38);
        for (Projector projector : projectors) {
            double x = projector.x(8), y = area.y() + area.height() / 2.0;
            circle(buffer, matrix, x, y, radius, 0x688C704D);
            circle(buffer, matrix, x, y, radius * .82, 0x387A6C54);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                double outwardX = Math.cos(angle), outwardY = Math.sin(angle);
                for (int bar = 0; bar < 3; bar++) {
                    double distance = radius * (.90 + bar * .045);
                    double cx = x + outwardX * distance, cy = y + outwardY * distance;
                    double vx = -outwardY * radius * .10, vy = outwardX * radius * .10;
                    if ((i & 1 << bar) != 0) {
                        segment(buffer, matrix, cx - vx, cy - vy, cx + vx, cy + vy, .45, 0x649D855F);
                    } else {
                        segment(buffer, matrix, cx - vx, cy - vy, cx - vx * .25, cy - vy * .25, .45, 0x649D855F);
                        segment(buffer, matrix, cx + vx * .25, cy + vy * .25, cx + vx, cy + vy, .45, 0x649D855F);
                    }
                }
            }
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void circle(BufferBuilder b, Matrix4f m, double x, double y, double radius, int color) {
        for (int i = 0; i < 48; i++) {
            double start = i * Math.PI / 24, end = (i + 1) * Math.PI / 24;
            segment(b, m, x + radius * Math.cos(start), y + radius * Math.sin(start),
                    x + radius * Math.cos(end), y + radius * Math.sin(end), .42, color);
        }
    }

    static void graph(GuiGraphics g, Topology topology, AcupointProgress state, int stage, String selected,
                      String active, String meridian, boolean animate, double seconds) {
        List<Dot> dots = topology.dots();
        if (dots.isEmpty()) return;
        var matrix = g.pose().last().pose();
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Meridian line = state.catalog().meridians().get(meridian);
        int count = state.bonuses().count(meridian);
        boolean full = line != null && count == line.count();
        int lineTone = full ? 0xC0FBC86D : line != null && count >= line.half() ? 0xAF91E5FF : 0xD873CBDF;
        for (Edge edge : topology.edges()) {
            Dot from = edge.from(), dot = edge.to();
            if (!dot.point().meridian().equals(meridian)) {
                if (edge.shortLink()) segment(buffer, matrix, from.x(), from.y(), dot.x(), dot.y(), .21, 0x627B8791);
                continue;
            }
            int tone = selected.equals(dot.point().id()) ? 0xFFF8D08D : lineTone;
            segment(buffer, matrix, from.x(), from.y(), dot.x(), dot.y(), .53, tone);
            if (animate && (full || dot.point().id().equals(active))) {
                double t = (seconds * 1.6 + dot.point().order() * .07) % 1;
                square(buffer, matrix, from.x() + (dot.x() - from.x()) * t,
                        from.y() + (dot.y() - from.y()) * t, 1, 0xFFFBE5AA);
            }
        }
        for (Dot dot : dots) {
            if (dot.point().id().equals(selected)) continue;
            int color = color(state, dot.point(), stage);
            boolean ready = color == BLUE;
            double radius = dot.point().tier() == Tier.HUB ? 1.15 : .72;
            if (dot.point().meridian().equals(meridian)) {
                radius += .25;
                if (color == GRAY) color = 0xFF8FD7EA;
            } else {
                color = (color & 0x00FFFFFF) | 0x81000000;
            }
            if (color == GOLD || color == BLUE) square(buffer, matrix, dot.x(), dot.y(), radius + 1, color & 0x00FFFFFF | 0x3C000000);
            if (ready) ring(buffer, matrix, dot.x(), dot.y(), radius + .45, color);
            else square(buffer, matrix, dot.x(), dot.y(), radius, color);
        }
        for (Dot dot : dots) {
            boolean chosen = dot.point().id().equals(selected);
            boolean charging = dot.point().id().equals(active);
            if (!chosen && !charging) continue;
            double radius = charging && animate ? 3.3 + Math.sin(seconds * 11) * .8 : 3.3;
            int edge = charging ? 0xFFFFF0CB : GOLD;
            ring(buffer, matrix, dot.x(), dot.y(), radius + 1.8, edge & 0x00FFFFFF | 0x65000000);
            ring(buffer, matrix, dot.x(), dot.y(), radius, edge);
            square(buffer, matrix, dot.x(), dot.y(), 1.15, 0xFFFFF3CF);
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private static String key(String id, Marker marker) { return id + marker.side() + marker.back(); }

    private static void ring(BufferBuilder b, Matrix4f m, double x, double y, double radius, int color) {
        segment(b, m, x, y - radius, x + radius, y, .5, color);
        segment(b, m, x + radius, y, x, y + radius, .5, color);
        segment(b, m, x, y + radius, x - radius, y, .5, color);
        segment(b, m, x - radius, y, x, y - radius, .5, color);
    }

    private static void square(BufferBuilder b, Matrix4f m, double x, double y, double r, int color) {
        vertex(b, m, x - r, y - r, color);
        vertex(b, m, x - r, y + r, color);
        vertex(b, m, x + r, y + r, color);
        vertex(b, m, x + r, y - r, color);
    }

    private static void segment(BufferBuilder b, Matrix4f m, double x, double y, double endX, double endY, double radius, int color) {
        double length = Math.hypot(endX - x, endY - y);
        if (length < .01) return;
        double dx = (endY - y) / length * radius;
        double dy = (x - endX) / length * radius;
        vertex(b, m, x + dx, y + dy, color);
        vertex(b, m, x - dx, y - dy, color);
        vertex(b, m, endX - dx, endY - dy, color);
        vertex(b, m, endX + dx, endY + dy, color);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, double x, double y, int color) {
        buffer.vertex(matrix, (float) x, (float) y, 0)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24).endVertex();
    }

    private AcupointBody() {}
}
