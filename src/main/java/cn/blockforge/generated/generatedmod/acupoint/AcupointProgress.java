package cn.blockforge.generated.generatedmod.acupoint;

import cn.blockforge.generated.generatedmod.CultivationData;
import cn.blockforge.generated.generatedmod.ProgressionGate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;

import static cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog.*;

public final class AcupointProgress {
    public enum Result {
        IDLE, SPENT, OPENED, UNKNOWN_POINT, ALREADY_OPEN, REALM_LOCKED, PREREQUISITE,
        MANA_EMPTY, COOLDOWN, INVALID_AUTO, CONFIG_MISMATCH
    }

    public record Bonuses(Map<Stat, Double> values, Map<String, Integer> counts, int total,
                          int complete, boolean minor, boolean major) {
        public double get(Stat stat) { return values.getOrDefault(stat, 0.0); }
        public float multiplier(Stat stat) { return (float) (1 + get(stat) / 100); }
        public int count(String meridian) { return counts.getOrDefault(meridian, 0); }
    }

    private final Set<String> opened = new TreeSet<>();
    private final Map<String, Integer> invested = new TreeMap<>();
    private String selected = "";
    private String autoMeridian = "";
    private boolean auto;
    private Data clientCatalog;
    private Data cachedCatalog;
    private Bonuses cachedBonuses;

    public Data catalog() { return clientCatalog == null ? AcupointCatalog.server() : clientCatalog; }
    public void useClientCatalog(Data data) { clientCatalog = data; }
    public boolean opened(String id) { return opened.contains(id); }
    public String selected() { return selected; }
    public String autoMeridian() { return autoMeridian; }
    public boolean auto() { return auto; }

    public void select(String id) { selected = id; }
    public void configureAuto(String meridian, boolean enabled) {
        autoMeridian = meridian;
        auto = enabled;
    }

    public int invested(Point point) {
        return opened(point.id()) ? point.cost() : Math.min(point.cost(), invested.getOrDefault(point.id(), 0));
    }

    public static int stage(CultivationData data) {
        return Math.min(4, ProgressionGate.realm(data) - ProgressionGate.QI_CONDENSATION + 1);
    }

    public Result eligibility(Point point, int stage) {
        if (point == null) return Result.UNKNOWN_POINT;
        if (opened(point.id())) return Result.ALREADY_OPEN;
        if (point.stage() > stage) return Result.REALM_LOCKED;
        if (!opened.containsAll(point.prerequisites())) return Result.PREREQUISITE;
        return Result.IDLE;
    }

    Result charge(Point point, CultivationData data, int maximum) {
        Result allowed = eligibility(point, stage(data));
        if (allowed != Result.IDLE) return allowed;
        int before = invested(point);
        float balance = data.getCurrentMana();
        int spend = Math.min(Math.min(maximum, point.cost() - before), (int) Math.floor(balance));
        // A data pack can lower the cost below a previously paid amount.
        if (before >= point.cost()) return open(point.id());
        if (spend <= 0 || !Float.isFinite(balance)) return Result.MANA_EMPTY;
        data.setCurrentMana(balance - spend);
        if (before + spend >= point.cost()) return open(point.id());
        invested.put(point.id(), before + spend);
        return Result.SPENT;
    }

    private Result open(String id) {
        invested.remove(id);
        opened.add(id);
        cachedBonuses = null;
        return Result.OPENED;
    }

    public Bonuses bonuses() {
        Data catalog = catalog();
        if (cachedBonuses != null && cachedCatalog == catalog) return cachedBonuses;
        Map<Stat, Double> values = new EnumMap<>(Stat.class);
        Map<String, Integer> counts = new HashMap<>();
        int total = 0;
        for (String id : opened) {
            Point point = catalog.points().get(id);
            if (point == null) continue;
            total++;
            values.merge(point.stat(), point.bonus(), Double::sum);
            counts.merge(point.meridian(), 1, Integer::sum);
        }
        int complete = 0;
        for (Meridian meridian : catalog.meridians().values()) {
            int count = counts.getOrDefault(meridian.id(), 0);
            if (count >= meridian.half()) {
                boolean full = count == meridian.count();
                if (full) complete++;
                double fraction = full ? 1 : meridian.halfFraction();
                values.merge(meridian.stat(), meridian.reward() * fraction, Double::sum);
                meridian.extraRewards().forEach((stat, value) -> values.merge(stat, value * fraction, Double::sum));
            }
        }
        boolean minor = catalog.minorRequires().stream().allMatch(id -> counts.getOrDefault(id, 0) == catalog.meridians().get(id).count());
        boolean major = total == catalog.points().size();
        if (minor) catalog.minorReward().forEach((stat, amount) -> values.merge(stat, amount, Double::sum));
        if (major) catalog.majorReward().forEach((stat, amount) -> values.merge(stat, amount, Double::sum));
        cachedCatalog = catalog;
        return cachedBonuses = new Bonuses(Map.copyOf(values), Map.copyOf(counts), total, complete, minor, major);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1);
        var nodes = new ListTag();
        for (String id : opened) nodes.add(StringTag.valueOf(id));
        tag.put("Opened", nodes);
        var partial = new CompoundTag();
        invested.forEach(partial::putInt);
        tag.put("Invested", partial);
        tag.putString("Selected", selected);
        tag.putString("AutoMeridian", autoMeridian);
        tag.putBoolean("Auto", auto);
        return tag;
    }

    public void load(CompoundTag tag) {
        opened.clear();
        ListTag nodes = tag.getList("Opened", Tag.TAG_STRING);
        for (int i = 0; i < nodes.size(); i++) {
            String id = nodes.getString(i);
            if (validId(id)) opened.add(id);
        }
        invested.clear();
        var partial = tag.getCompound("Invested");
        for (String id : partial.getAllKeys()) {
            int value = partial.getInt(id);
            if (validId(id) && value > 0 && !opened(id)) invested.put(id, value);
        }
        selected = tag.getString("Selected");
        if (!validId(selected)) selected = "";
        autoMeridian = tag.getString("AutoMeridian");
        if (!autoMeridian.matches("[A-Z]{2}")) autoMeridian = "";
        auto = tag.getBoolean("Auto") && !autoMeridian.isEmpty();
        cachedBonuses = null;
    }

    private static boolean validId(String id) { return id.matches("[A-Z]{2}-[1-9][0-9]{0,2}"); }
}
