package cn.blockforge.generated.generatedmod.alchemy;

import java.util.List;

/** One table for book registration, teaching, models, and creative inventory entries. */
public enum AlchemyCompendium {
    INTRODUCTION("alchemy_introduction"),
    NOURISHING("alchemy_nourishing", PillCatalog.GATHERING, PillCatalog.CULTIVATION, PillCatalog.FOUNDATION, PillCatalog.ORIGIN_CONDENSING),
    GUARDING("alchemy_guarding", PillCatalog.CLEAR_HEART, PillCatalog.CONTINUE_VEIN, PillCatalog.CALMING),
    BODY("alchemy_body", PillCatalog.DIAMOND_BODY, PillCatalog.BERSERK, PillCatalog.GALE, PillCatalog.PROFOUND_WATER),
    BREAKTHROUGH("alchemy_breakthrough", PillCatalog.BREAKTHROUGH, PillCatalog.TRIBULATION, PillCatalog.BARRIER_PREPARATION),
    APTITUDE("alchemy_aptitude", PillCatalog.ROOT, PillCatalog.PHYSIQUE, PillCatalog.MERIDIAN_EXPANSION),
    COMPLETE("alchemy_complete");
    public final String id;
    private final List<PillCatalog> formulas;
    AlchemyCompendium(String id, PillCatalog... formulas) { this.id = id; this.formulas = List.of(formulas); }
    public List<PillCatalog> formulas() { return this == COMPLETE ? List.of(PillCatalog.values()) : formulas; }
}
