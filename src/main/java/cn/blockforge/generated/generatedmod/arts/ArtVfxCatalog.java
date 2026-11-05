package cn.blockforge.generated.generatedmod.arts;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

/** 选材来自粒子库 V2；造型与施法几何独立，不把二维预览路径冒充伤害范围。 */
public final class ArtVfxCatalog {
    public record Recipe(String body, String accent, String debris, int color) {}
    public static Recipe recipe(String id) {
        return switch (id) {
            case "art_sword_aura" -> new Recipe("ink_crescent", "pierce_diamond", "spark_needle", 0xA7F0FF);
            case "art_spirit_shield" -> new Recipe("shield_hex", "meridian_knot", "spark_soft", 0xA7F0FF);
            case "art_immobilize" -> new Recipe("root_lattice", "chain_barb", "seal_glyph", 0xFFD167);
            case "art_golden_bell" -> new Recipe("bell_shell", "armor_scale", "coin_charm", 0xFFD167);
            case "art_ice_spike" -> new Recipe("ice_lance", "frost_fern", "snow_crystal", 0x64CFEF);
            case "art_armor_break" -> new Recipe("pierce_diamond", "armor_scale", "sword_cross", 0xFFD167);
            case "art_flame_wave" -> new Recipe("fire_brush", "heat_wisp", "ember_fragment", 0xFF793D);
            case "art_flash_step" -> new Recipe("thought_rift", "feather_lift", "lens_crescent", 0xA88DFF);
            case "art_thunder" -> new Recipe("lightning_branch", "thunder_core", "spark_needle", 0xA88DFF);
            case "art_myriad_swords" -> new Recipe("sword_fan", "sword_ghost", "pierce_diamond", 0xFFD167);
            case "art_wanjian_guizong" -> new Recipe("sword_fan", "sword_ghost", "pierce_diamond", 0xFFD167);
            case "art_jiuxiao_sword_rain" -> new Recipe("mirage_frame", "sword_ghost", "thought_rift", 0xA88DFF);
            case "art_immovable_mountain" -> new Recipe("mountain_ridge", "earth_shard", "sand_grain", 0xD1AC70);
            case "art_human_harmony" -> new Recipe("meridian_knot", "pill_orbit", "feather_lift", 0x79DFA0);
            case "art_five_elements" -> new Recipe("metal_sigil", "crystal_lotus", "earth_sigil", 0xE6FFFF);
            case "art_sword_pierce" -> new Recipe("spear_head", "wind_streak", "prism_glint", 0xA7F0FF);
            case "art_sword_sweep" -> new Recipe("ink_crescent", "feather_lift", "tide_crest", 0xB6E4D9);
            case "art_sword_guard" -> new Recipe("sword_ghost", "armor_scale", "metal_sigil", 0xFFD167);
            case "art_sword_array" -> new Recipe("mirage_frame", "sword_ghost", "thought_rift", 0xA88DFF);
            case "art_sword_flight" -> new Recipe("spear_head", "feather_lift", "ruyi_cloud", 0xA7F0FF);
            default -> throw new IllegalArgumentException("Unknown art: " + id);
        };
    }
    public static final List<String> ELEMENTS = List.of("metal_sigil", "wood_sigil", "water_sigil", "fire_sigil", "earth_sigil");
    public static final int[] ELEMENT_COLORS = {0xFFD167, 0x79DFA0, 0x64CFEF, 0xFF793D, 0xD1AC70};
    public static Set<String> materials() {
        Set<String> result = new LinkedHashSet<>(ELEMENTS);
        for (var entry : cn.blockforge.generated.generatedmod.ArtCatalog.ENTRIES) {
            if (FourArtsVisuals.replaces(entry.id())) continue;
            Recipe r = recipe(entry.id());
            result.add(r.body()); result.add(r.accent()); result.add(r.debris());
        }
        return Set.copyOf(result);
    }
    private ArtVfxCatalog() {}
}
