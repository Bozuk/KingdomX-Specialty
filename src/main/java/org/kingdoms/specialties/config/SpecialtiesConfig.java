package org.kingdoms.specialties.config;

import org.kingdoms.config.accessor.ConfigAccessor;
import org.kingdoms.config.accessor.EnumConfig;
import org.kingdoms.config.accessor.KeyedConfigAccessor;
import org.kingdoms.config.implementation.KeyedYamlConfigAccessor;
import org.kingdoms.config.managers.ConfigManager;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.utils.config.ConfigPath;
import org.kingdoms.utils.config.adapters.YamlResource;
import org.kingdoms.utils.string.Strings;

/**
 * Every scalar option of {@code specialties.yml}.
 * <p>
 * The numbers passed to the constructor are the positions at which the enum name is split
 * into configuration path elements, exactly like the rest of the KingdomsX configs.
 * {@code EXTRACTION_RESOURCE_POINTS_PER_UNIT(1)} therefore resolves to
 * {@code extraction.resource-points-per-unit}.
 */
public enum SpecialtiesConfig implements EnumConfig {
    SELECTION_KING_ONLY(1),
    SELECTION_REQUIRED_LEVEL(1),
    SELECTION_REQUIRE_CONFIRMATION(1),
    SELECTION_COST_RESOURCE_POINTS(1, 2),
    SELECTION_COST_MONEY(1, 2),

    EXTRACTION_RESOURCE_POINTS_PER_UNIT(1),
    EXTRACTION_MULTIPLIER(1),
    EXTRACTION_MAX_PER_COLLECT(1),
    EXTRACTION_CARRY_OVER_REMAINDER(1),
    EXTRACTION_DROP_IF_INVENTORY_FULL(1),
    ;

    private static final YamlResource SPECIALTIES = new YamlResource(
            SpecialtiesAddon.get(),
            Kingdoms.getPath("specialties.yml").toFile(),
            "specialties.yml"
    ).load();

    static {
        ConfigManager.registerAsMainConfig(SPECIALTIES);
        ConfigManager.watch(SPECIALTIES);
    }

    /** Triggers the static initializer. Called from the addon's {@code onLoad()}. */
    public static void init() {}

    private final ConfigPath option;

    SpecialtiesConfig() {
        this.option = new ConfigPath(Strings.configOption(this));
    }

    SpecialtiesConfig(int... grouped) {
        this.option = new ConfigPath(this.name(), grouped);
    }

    @Override
    public KeyedConfigAccessor getManager() {
        return new KeyedYamlConfigAccessor(SPECIALTIES, option);
    }

    public static YamlResource getConfig() {
        return SPECIALTIES;
    }

    /** Root accessor, used for the dynamic sections (specialties, recipes). */
    public static ConfigAccessor accessor() {
        return SPECIALTIES.accessor();
    }
}
