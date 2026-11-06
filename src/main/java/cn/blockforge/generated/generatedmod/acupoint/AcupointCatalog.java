package cn.blockforge.generated.generatedmod.acupoint;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class AcupointCatalog extends SimplePreparableReloadListener<AcupointCatalog.Data> {
    public static final int POINT_COUNT = 361;
    public static final int FILE_LIMIT = 225_000;
    private static final String[] FILES = {"acupoints_361", "meridians_14", "balance_v1", "ui_layout_361"};
    private static final String DIRECTORY = "acupoints/";
    private static volatile Data server;

    public enum Stat {
        MAX_HEALTH, MAX_MANA, MANA_REGEN_PCT, ATTACK, DEFENSE, SKILL_DAMAGE_PCT, MOVE_SPEED_PCT,
        TOUGHNESS, MINING_SPEED_PCT, FALL_RESIST_PCT, SENSE_CAPACITY, SENSE_REGEN_PCT,
        CRIT_CHANCE_PCT, CRIT_DAMAGE_PCT, ATTACK_SPEED_PCT, SWIM_SPEED_PCT, JUMP_POWER_PCT,
        LIGHTNESS_PCT, STEP_HEIGHT, LUCK, BLOCK_REACH, ENTITY_REACH, RANGED_DAMAGE_PCT,
        TRAINING_PCT, REALM_FLIGHT_SPEED_PCT, SENSE_SCAN_RADIUS;
        public String key() { return name().toLowerCase(Locale.ROOT); }
        public boolean percent() { return name().endsWith("_PCT"); }
        static Stat read(String key) { return valueOf(key.toUpperCase(Locale.ROOT)); }
    }

    public enum Tier { NORMAL, IMPORTANT, HUB }
    public record Point(String id, String name, String meridian, int index, int order, int stage,
                        Tier tier, Stat stat, double bonus, int cost, boolean bilateral, List<String> prerequisites) {}
    public record Meridian(String id, String name, String element, String root, int count, boolean bilateral,
                           int half, Stat stat, double reward, double halfFraction, Map<Stat, Double> extraRewards) {}
    public record Marker(boolean back, String side, double x, double y, String zone) {}
    public record Region(int x, int y, int width, int height) {}
    public record Charge(int tap, int interval, int minimum, int maximum, double factor, boolean interrupt,
                         boolean moving, boolean autoOnlyDuringMeditation, int syncInterval) {
        public int perInterval(float maxMana) {
            return Math.min(maximum, Math.max(minimum, (int) Math.floor(maxMana * factor)));
        }
    }

    public record Data(Map<String, Point> points, Map<String, Meridian> meridians,
                       Map<String, List<Point>> routes, Map<String, List<Marker>> markers,
                       Map<String, Region> regions, List<String> stageNames,
                       List<String> minorRequires, Map<Stat, Double> minorReward, Map<Stat, Double> majorReward,
                       Charge charge, List<String> documents, String version) {}

    private static final class Defaults {
        private static final Data DATA = parse(Arrays.stream(FILES).map(name -> {
            var path = "/data/" + GeneratedMod.MOD_ID + "/" + DIRECTORY + name + ".json";
            try (InputStream stream = AcupointCatalog.class.getResourceAsStream(path)) {
                return read(stream, path);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }).toList());
    }

    public static Data defaults() { return Defaults.DATA; }
    public static Data server() { return server == null ? defaults() : server; }

    @SubscribeEvent
    public static void register(AddReloadListenerEvent event) { event.addListener(new AcupointCatalog()); }

    @Override
    protected Data prepare(ResourceManager manager, ProfilerFiller profiler) {
        return parse(Arrays.stream(FILES).map(name -> {
            var id = new ResourceLocation(GeneratedMod.MOD_ID, DIRECTORY + name + ".json");
            try (InputStream stream = manager.getResourceOrThrow(id).open()) {
                return read(stream, id.toString());
            } catch (IOException ex) {
                throw new UncheckedIOException("Cannot read Baiqiao resource " + id, ex);
            }
        }).toList());
    }

    @Override
    protected void apply(Data data, ResourceManager manager, ProfilerFiller profiler) { server = data; }

    private static String read(InputStream stream, String name) throws IOException {
        if (stream == null) throw new FileNotFoundException(name);
        byte[] bytes = stream.readNBytes(FILE_LIMIT + 1);
        if (bytes.length > FILE_LIMIT) throw new IOException("Baiqiao resource exceeds size limit: " + name);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static Data parse(List<String> documents) {
        require(documents.size() == FILES.length, "Expected four Baiqiao documents");
        JsonObject[] json = new JsonObject[FILES.length];
        for (int i = 0; i < FILES.length; i++) {
            json[i] = JsonParser.parseString(documents.get(i)).getAsJsonObject();
            require(integer(json[i], "schema_version") == 1, "Unsupported version in " + FILES[i]);
        }
        JsonObject balance = json[2];
        require(integer(balance, "logical_points") == POINT_COUNT && integer(balance, "physical_markers") == 670,
                "Invalid Baiqiao catalog sizes");
        List<String> stages = new ArrayList<>();
        for (JsonElement stage : array(balance, "stages")) {
            JsonObject value = stage.getAsJsonObject();
            require(integer(value, "index") == stages.size() + 1, "Unexpected realm stage");
            stages.add(label(value, "name"));
        }
        require(stages.size() == 4, "Expected four realm stages");

        Map<String, Meridian> meridians = new LinkedHashMap<>();
        for (JsonElement row : array(json[1], "meridians")) {
            JsonObject entry = row.getAsJsonObject();
            JsonObject reward = entry.getAsJsonObject("reward");
            String id = label(entry, "id");
            int count = integer(entry, "standard_point_count");
            int half = integer(entry, "half_threshold");
            require(id.matches("[A-Z]{2}") && count > 0 && half > 0 && half <= count, "Bad meridian: " + id);
            Meridian meridian = new Meridian(id, label(entry, "name"), label(entry, "element_association_for_game"),
                    label(entry, "root_point_id"), count, flag(entry, "bilateral"), half,
                    Stat.read(label(reward, "attribute")), number(reward, "full_value", 1000),
                    number(reward, "half_fraction", 1), rewards(entry.getAsJsonObject("extra_rewards")));
            require(meridians.putIfAbsent(id, meridian) == null, "Duplicate meridian: " + id);
        }
        require(meridians.size() == 14 && meridians.containsKey("CV") && meridians.containsKey("GV"), "Expected 14 meridians");
        Map<String, Point> points = new LinkedHashMap<>();
        Map<String, List<Point>> routes = new LinkedHashMap<>();
        meridians.keySet().forEach(id -> routes.put(id, new ArrayList<>()));
        for (JsonElement row : array(json[0], "acupoints")) {
            JsonObject entry = row.getAsJsonObject();
            String id = label(entry, "id");
            String meridian = label(entry, "meridian");
            int index = integer(entry, "standard_index");
            int order = integer(entry, "route_order");
            int stage = integer(entry, "unlock_stage");
            int cost = integer(entry, "required_mana");
            boolean bilateral = flag(entry, "bilateral");
            Meridian line = meridians.get(meridian);
            require(line != null && id.equals(meridian + "-" + index) && index > 0 && index <= line.count()
                    && order > 0 && order <= line.count() && stage >= 1 && stage <= stages.size()
                    && cost > 0 && cost <= 1_000_000 && bilateral == line.bilateral(), "Bad point: " + id);
            List<String> prerequisites = strings(entry, "prerequisites");
            require(prerequisites.size() == new HashSet<>(prerequisites).size(), "Duplicate prerequisite: " + id);
            Point point = new Point(id, label(entry, "name_zh"), meridian, index, order, stage,
                    Tier.valueOf(label(entry, "tier")), Stat.read(label(entry, "attribute")),
                    number(entry, "bonus", 1000), cost, bilateral, prerequisites);
            require(points.putIfAbsent(id, point) == null, "Duplicate point: " + id);
            routes.get(meridian).add(point);
        }
        require(points.size() == POINT_COUNT, "Expected 361 points");
        routes.replaceAll((id, route) -> {
            Meridian line = meridians.get(id);
            require(route.size() == line.count(), "Wrong point count: " + id);
            var sorted = route.stream().sorted(Comparator.comparingInt(Point::order)).toList();
            for (int i = 0; i < sorted.size(); i++) {
                Point point = sorted.get(i);
                require(point.order() == i + 1 && point.prerequisites().isEmpty() == point.id().equals(line.root()),
                        "Bad root or route order: " + point.id());
                for (String prerequisite : point.prerequisites()) {
                    Point before = points.get(prerequisite);
                    require(before != null && before.meridian().equals(id) && before.stage() <= point.stage()
                            && before.order() < point.order(), "Bad prerequisite " + prerequisite + " for " + point.id());
                }
            }
            return sorted;
        });

        Map<String, Region> regions = new LinkedHashMap<>();
        json[3].getAsJsonObject("body_model").entrySet().forEach(entry -> {
            JsonArray box = entry.getValue().getAsJsonArray();
            require(box.size() == 4, "Invalid body box " + entry.getKey());
            Region region = new Region(box.get(0).getAsInt(), box.get(1).getAsInt(), box.get(2).getAsInt(), box.get(3).getAsInt());
            require(region.x() >= 0 && region.y() >= 0 && region.width() > 0 && region.height() > 0
                    && region.x() + region.width() <= 16 && region.y() + region.height() <= 32, "Invalid body box " + entry.getKey());
            regions.put(entry.getKey(), region);
        });
        require(regions.equals(Map.of("head", new Region(4, 0, 8, 8), "torso", new Region(4, 8, 8, 12),
                "left_arm", new Region(0, 8, 4, 12), "right_arm", new Region(12, 8, 4, 12),
                "left_leg", new Region(4, 20, 4, 12), "right_leg", new Region(8, 20, 4, 12))), "Invalid player proportions");
        Map<String, List<Marker>> markers = new LinkedHashMap<>();
        int markerCount = 0;
        for (JsonElement row : array(json[3], "positions")) {
            JsonObject entry = row.getAsJsonObject();
            String id = label(entry, "point_id");
            Point point = points.get(id);
            require(point != null, "Unknown layout point: " + id);
            List<Marker> positions = new ArrayList<>();
            for (JsonElement raw : array(entry, "markers")) {
                JsonObject value = raw.getAsJsonObject();
                String view = label(value, "view");
                String side = label(value, "side");
                double x = number(value, "x", 16), y = number(value, "y", 32);
                require(view.equals("front") || view.equals("back"), "Invalid view: " + id);
                require(regions.values().stream().anyMatch(box -> x >= box.x() && y >= box.y()
                        && x <= box.x() + box.width() && y <= box.y() + box.height()), "Point outside player: " + id);
                positions.add(new Marker(view.equals("back"), side, x, y, label(value, "zone")));
            }
            require(positions.size() == (point.bilateral() ? 2 : 1), "Wrong marker count: " + id);
            if (point.bilateral()) {
                Marker left = positions.get(0), right = positions.get(1);
                require(left.side().equals("L") && right.side().equals("R") && left.back() == right.back()
                        && Math.abs(left.x() + right.x() - 16) < 0.001 && Math.abs(left.y() - right.y()) < 0.001,
                        "Invalid mirror: " + id);
            }
            require(markers.putIfAbsent(id, List.copyOf(positions)) == null, "Duplicate layout point: " + id);
            markerCount += positions.size();
        }
        require(markerCount == 670 && markers.keySet().equals(points.keySet()), "Incomplete marker catalog");

        JsonObject charge = balance.getAsJsonObject("charge");
        int interval = integer(charge, "channel_interval_ticks");
        int tap = integer(charge, "manual_once_mana");
        int min = integer(charge, "channel_spend_min");
        int max = integer(charge, "channel_spend_max");
        int hz = integer(balance.getAsJsonObject("sync"), "max_update_rate_hz");
        require(tap > 0 && tap <= 1000 && interval > 0 && interval <= 200 && min > 0 && max >= min && max <= 1000
                && hz > 0 && hz <= 4, "Invalid Baiqiao rates");
        Charge rates = new Charge(tap, interval, min, max, number(charge, "channel_spend_max_mana_factor", 1),
                flag(charge, "channel_damage_interrupt"), flag(charge, "charge_while_moving"),
                flag(charge, "auto_only_during_meditation"), (20 + hz - 1) / hz);
        JsonObject milestones = balance.getAsJsonObject("milestones");
        JsonObject minor = milestones.getAsJsonObject("minor_cycle");
        JsonObject major = milestones.getAsJsonObject("major_cycle");
        List<String> minorRequires = strings(minor, "requires");
        require(!minorRequires.isEmpty() && meridians.keySet().containsAll(minorRequires)
                && flag(major, "requires_all_points"), "Invalid cycle requirements");
        String version = UUID.nameUUIDFromBytes(String.join("\n", documents).getBytes(StandardCharsets.UTF_8)).toString();
        return new Data(immutable(points), immutable(meridians), immutable(routes), immutable(markers), immutable(regions),
                List.copyOf(stages), minorRequires, reward(minor), reward(major), rates, List.copyOf(documents), version);
    }

    private static <K, V> Map<K, V> immutable(Map<K, V> map) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    private static Map<Stat, Double> reward(JsonObject entry) {
        return rewards(entry.getAsJsonObject("reward"));
    }

    private static Map<Stat, Double> rewards(JsonObject values) {
        if (values == null) return Map.of();
        Map<Stat, Double> reward = new EnumMap<>(Stat.class);
        values.keySet().forEach(key -> reward.put(Stat.read(key), number(values, key, 1000)));
        return Map.copyOf(reward);
    }

    private static List<String> strings(JsonObject row, String key) {
        List<String> values = new ArrayList<>();
        for (JsonElement value : array(row, key)) values.add(value.getAsString());
        return List.copyOf(values);
    }

    private static JsonArray array(JsonObject row, String key) { return row.getAsJsonArray(key); }
    private static boolean flag(JsonObject row, String key) {
        JsonPrimitive value = row.getAsJsonPrimitive(key);
        require(value.isBoolean(), "Invalid boolean " + key);
        return value.getAsBoolean();
    }
    private static String label(JsonObject row, String key) {
        String label = row.get(key).getAsString();
        require(!label.isBlank() && label.length() <= 48, "Invalid label " + key);
        return label;
    }
    private static int integer(JsonObject row, String key) { return row.get(key).getAsBigDecimal().intValueExact(); }
    private static double number(JsonObject row, String key, double maximum) {
        double value = row.get(key).getAsDouble();
        require(Double.isFinite(value) && value >= 0 && value <= maximum, "Invalid number " + key);
        return value;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
