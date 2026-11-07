package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog.*;
import cn.blockforge.generated.generatedmod.acupoint.AcupointProgress;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.Predicate;

final class AcupointBody {
    static final int GOLD = 0xFFFFCF74;
    static final int BLUE = 0xFF77DCFF;
    static final int GRAY = 0xFF8B98A2;
    private static final ResourceLocation BACKDROP = ResourceLocation.fromNamespaceAndPath("wuxingxiantu",
            "textures/gui/wuxia/baiqiao_backdrop.png");
    private static final int BACKDROP_TILE = 400;

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
    // The Baiqiao projection uses the same 64x64 player-skin atlas as the
    // in-world model. These are the front/back rectangles for the second
    // (hat, jacket, sleeve and pants) layer, in PARTS order.
    private static final int[][] OUTER_FRONT = {{40, 8}, {20, 36}, {44, 36}, {52, 52}, {4, 36}, {4, 52}};
    private static final int[][] OUTER_BACK = {{56, 8}, {32, 36}, {60, 52}, {52, 36}, {12, 52}, {12, 36}};

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
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(.60F, .66F, .75F, 1);
        drawSkinLayer(graphics, player, catalog, projector, projector.back ? BACK : FRONT, false);
        // The outer layer is a second, slightly enlarged model shell in
        // PlayerModel (CubeDeformation .25). It must be drawn as a separate
        // full-alpha pass; using the base tint makes transparent hat/jacket
        // pixels disappear into the translucent body projection.
        graphics.setColor(.96F, .98F, 1F, 1);
        drawSkinLayer(graphics, player, catalog, projector, projector.back ? OUTER_BACK : OUTER_FRONT, true);
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    private static void drawSkinLayer(GuiGraphics graphics, LocalPlayer player, Data catalog,
                                      Projector projector, int[][] uvs, boolean outer) {
        for (int i = 0; i < PARTS.length; i++) {
            Region part = catalog.regions().get(PARTS[i]);
            // Match the .25-pixel outer CubeDeformation instead of painting
            // both layers into exactly the same rectangle.
            double pad = outer ? .28 : 0;
            int x = (int) Math.round(projector.x(part.x() - pad));
            int y = (int) Math.round(projector.y(part.y() - pad));
            int w = (int) Math.round(projector.x(part.x() + part.width() + pad)) - x;
            int h = (int) Math.round(projector.y(part.y() + part.height() + pad)) - y;
            int textureWidth = part.width();
            int u = uvs[i][0];
            if ((i == 2 || i == 3) && player.getModelName().equals("slim")) {
                textureWidth = 3;
                if (projector.back) u--;
            }
            graphics.blit(player.getSkinTextureLocation(), x, y, w, h,
                    u, uvs[i][1], textureWidth, part.height(), 64, 64);
        }
    }

    static void backdrop(GuiGraphics g, Box area, List<Projector> projectors) {
        int count = projectors.size();
        int size = Math.min(area.width() / count, area.height() * 9 / 10);
        int y = area.y() + area.height() * 43 / 100 - size / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int i = 0; i < count; i++) {
            // The artwork stays fixed when the projection pans or zooms.
            int center = area.x() + area.width() * (2 * i + 1) / (2 * count);
            int u = projectors.get(i).back() ? BACKDROP_TILE : 0;
            g.blit(BACKDROP, center - size / 2, y, size, size, u, 0,
                    BACKDROP_TILE, BACKDROP_TILE, 2 * BACKDROP_TILE, BACKDROP_TILE);
        }
        RenderSystem.disableBlend();
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
