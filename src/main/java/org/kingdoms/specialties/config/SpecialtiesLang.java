package org.kingdoms.specialties.config;

import org.kingdoms.locale.LanguageEntry;
import org.kingdoms.locale.messenger.DefinedMessenger;

/**
 * Every message of the addon, in English.
 * <p>
 * These are the fallback texts. The actual text shown to a player comes from the per-language file
 * of {@code plugins/Kingdoms/specialties/languages/}, see
 * {@link org.kingdoms.specialties.locale.SpecialtiesLanguages}. Anything a language file doesn't
 * override falls back to the value defined here.
 * <p>
 * Entries starting with {@code COMMAND_} are written to the global {@code command:} section of the
 * language files, everything else lands under {@code specialties:}. The trailing numbers are the
 * positions at which the enum name is split into path elements.
 */
public enum SpecialtiesLang implements DefinedMessenger {

    // ------------------------------------------------------------------ /k specialty
    COMMAND_SPECIALTY_NAME("specialty"),
    COMMAND_SPECIALTY_ALIASES("specialities spec"),
    COMMAND_SPECIALTY_DESCRIPTION("{$s}Manage your kingdom's specialty.", 1, 2),

    COMMAND_SPECIALTY_INFO_DESCRIPTION("{$s}Shows your kingdom's specialty.", 1, 2, 3),
    COMMAND_SPECIALTY_INFO_NONE("{$e}Your kingdom has no specialty yet. " +
            "hover:{{$es}/k specialty choose;{$p}Click to choose;/k specialty choose}", 1, 2, 3),
    COMMAND_SPECIALTY_INFO_DISPLAY("{$sep}-=[ {$p}Kingdom specialty {$sep}]=-\n" +
            "{$p}Specialty{$colon} {$s}%specialty%\n" +
            "{$p}Resource{$colon} {$s}%specialty_resource%\n" +
            "{$p}Extractors{$colon} {$s}%extractors% {$sep}({$s}%fueled_extractors% {$sep}fuelled)\n" +
            "{$p}Yield{$colon} {$s}1 {$p}per {$s}%per_unit% {$p}resource points\n" +
            "{$p}Pending{$colon} {$s}%remainder%{$sep}/{$s}%per_unit%\n" +
            "{$sep}Collect your extractors to get the resource.\n" +
            "{$sep}The specialty can never be changed.", 1, 2, 3),

    COMMAND_SPECIALTY_CHOOSE_DESCRIPTION("{$s}Picks the specialty of a kingdom that has none.", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_USAGE("{$usage}specialty choose {$p}[specialty]", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_UNKNOWN("{$es}%specialty% {$e}is not a valid specialty.", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_ALREADY_CHOSEN("{$e}Your kingdom already picked the {$es}%specialty% {$e}specialty. " +
            "That choice is final, only an admin can change it.", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_NOT_KING("{$e}Only the king can pick the kingdom's specialty.", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_NO_PERMISSION("{$e}You lack the kingdom permission required to pick the specialty.", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_LOW_LEVEL("{$e}Your kingdom must be at least level {$es}%required_level% {$e}to pick a specialty.", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_NOT_ENOUGH_RESOURCE_POINTS("{$e}Your kingdom needs {$es}%cost% resource points{$e}.", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_NOT_ENOUGH_MONEY("{$e}Your kingdom needs {$es}$%cost%{$e}.", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_CONFIRM("{$e}Picking a specialty is {$es}FINAL{$e}. " +
            "You are about to pick {$es}%specialty%{$e}.\n" +
            "{$e}Do it again to confirm.", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_CHOSEN("{$p}Your kingdom is now {$s}%specialty%{$p}. " +
            "Unique resource{$colon} {$s}%specialty_resource%", 1, 2, 3),
    COMMAND_SPECIALTY_CHOOSE_ANNOUNCEMENT("{$s}%player% {$p}picked the {$s}%specialty% {$p}specialty for the kingdom.", 1, 2, 3),

    // ------------------------------------------------------- /k admin specialty
    COMMAND_ADMIN_SPECIALTY_DESCRIPTION("{$s}Changes the specialty of a kingdom.", 1, 2, 3),
    COMMAND_ADMIN_SPECIALTY_USAGE("{$usage}admin specialty {$p}<kingdom> <specialty|reset>", 1, 2, 3),
    COMMAND_ADMIN_SPECIALTY_UNKNOWN("{$es}%specialty% {$e}is not a valid specialty. " +
            "Allowed{$colon} {$es}%specialties%{$e}, {$es}reset", 1, 2, 3),
    COMMAND_ADMIN_SPECIALTY_SET("{$p}The specialty of {$s}%kingdoms_kingdom_name% {$p}is now {$s}%specialty%{$p}.", 1, 2, 3),
    COMMAND_ADMIN_SPECIALTY_RESET("{$p}The specialty of {$s}%kingdoms_kingdom_name% {$p}was reset.", 1, 2, 3),
    COMMAND_ADMIN_SPECIALTY_CHANGED_NOTIFICATION("{$e}An admin changed your kingdom's specialty to {$es}%specialty%{$e}.", 1, 2, 3),
    COMMAND_ADMIN_SPECIALTY_RESET_NOTIFICATION("{$e}An admin reset your kingdom's specialty.", 1, 2, 3),

    // ----------------------------------------------------------------- creation
    CREATION_CHOOSE_FIRST("{$p}Pick your kingdom's specialty to complete its creation.\n" +
            "{$e}This choice is final and can never be changed.", 1),
    CREATION_CHOSEN("{$p}Kingdom founded with the {$s}%specialty% {$p}specialty.\n" +
            "{$p}Unique resource{$colon} {$s}%specialty_resource%", 1),
    CREATION_EXPIRED("{$e}The creation timed out. Run {$es}/k create <name> {$e}again.", 1),

    // --------------------------------------------------------------- extraction
    EXTRACTION_COLLECTED("{$p}The extractor yields {$s}%collected%x %specialty_resource%{$p}.", 1),
    EXTRACTION_NOT_ENOUGH_YET("{$sep}Extracting %specialty_resource%{$sep}{$colon} " +
            "{$s}%remainder%{$sep}/{$s}%per_unit% {$sep}resource points.", 1),
    EXTRACTION_INVENTORY_FULL("{$e}Your inventory is full, the rest is kept for the next collect.", 1),

    // ---------------------------------------------------------------- forge
    FORGE_MISSING_INGREDIENTS("{$e}You are missing ingredients for this recipe.", 1),
    FORGE_CRAFTED("{$p}Forged{$colon} {$s}%recipe%", 1),
    FORGE_ENCHANTED("{$p}Enchantment applied{$colon} {$s}%recipe% {$sep}(level {$s}%level%{$sep})", 1),
    FORGE_ENCHANT_NO_ITEM("{$e}Hold the item you want to enchant, then click.", 1, 2),
    FORGE_ENCHANT_WRONG_ITEM("{$e}This enchantment does not apply to the item you are holding.", 1, 2),
    FORGE_ENCHANT_ALREADY("{$e}That item already has this enchantment at level {$es}%level%{$e} or above.", 1, 2),

    // Lines the forge menu builds for every recipe entry.
    FORGE_ENTRY_ENCHANTMENT("&7Enchantment &f%enchantment% &7level &e%level%", 1, 2),
    FORGE_ENTRY_BEYOND_VANILLA("&8Beyond the vanilla cap", 1, 2),
    FORGE_ENTRY_INGREDIENTS("&7Required ingredients&8:", 1, 2),
    FORGE_ENTRY_INGREDIENT_OK("&a %owned%/%required% &7%ingredient%", 1, 2),
    FORGE_ENTRY_INGREDIENT_MISSING("&c %owned%/%required% &7%ingredient%", 1, 2),
    FORGE_ENTRY_HOLD_ITEM("&7Hold the item in your hand, then click.", 1, 2),
    FORGE_ENTRY_CLICK_TO_FORGE("&aClick to forge.", 1, 2),
    FORGE_ENTRY_MISSING("&cMissing ingredients.", 1, 2),
    ;

    private final LanguageEntry languageEntry;
    private final String defaultValue;

    SpecialtiesLang(String defaultValue, int... group) {
        this.defaultValue = defaultValue;
        this.languageEntry = DefinedMessenger.getEntry("specialties", this, group);
    }

    @Override
    public LanguageEntry getLanguageEntry() {
        return languageEntry;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
