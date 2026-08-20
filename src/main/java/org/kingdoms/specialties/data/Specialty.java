package org.kingdoms.specialties.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.config.accessor.ConfigAccessor;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.specialties.config.SpecialtiesConfig;
import org.kingdoms.specialties.items.ItemParser;
import org.kingdoms.specialties.items.SpecialtyItems;
import org.kingdoms.specialties.items.TextUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The four specialties a kingdom can pick from. A kingdom owns exactly one of them, forever.
 * <p>
 * The enum constants are fixed, but everything they expose (name, icon, unique resource and the
 * recipes they unlock) comes from {@code specialties.yml} and is refreshed on every reload.
 */
public enum Specialty {
    /** Weapons. */
    WEAPONSMITH("weaponsmith", Material.IRON_SWORD),
    /** Armors. */
    ARMORER("armorer", Material.IRON_CHESTPLATE),
    /** Potions. */
    ALCHEMIST("alchemist", Material.BREWING_STAND),
    /** Enchantments beyond the vanilla caps. */
    ENCHANTER("enchanter", Material.ENCHANTING_TABLE),
    ;

    private static final Specialty[] VALUES = values();

    private final String configKey;
    private final Material fallbackIcon;

    private String displayName;
    private Material icon;
    private List<String> description = Collections.emptyList();
    private ItemStack resource;
    private List<SpecialtyRecipe> recipes = Collections.emptyList();

    Specialty(String configKey, Material fallbackIcon) {
        this.configKey = configKey;
        this.fallbackIcon = fallbackIcon;
        this.displayName = configKey;
        this.icon = fallbackIcon;
    }

    public String getConfigKey() {
        return configKey;
    }

    /** Coloured name shown to players. */
    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    /**
     * The unique resource this specialty produces. Always returns a fresh copy of amount 1,
     * never the internal prototype.
     */
    public ItemStack getResource() {
        return resource == null ? null : resource.clone();
    }

    public ItemStack getResource(int amount) {
        if (resource == null) return null;
        ItemStack copy = resource.clone();
        copy.setAmount(Math.max(1, Math.min(amount, copy.getMaxStackSize())));
        return copy;
    }

    /** The uncoloured name of the resource, used inside messages. */
    public String getResourceName() {
        if (resource == null || !resource.hasItemMeta() || resource.getItemMeta().getDisplayName().isEmpty()) {
            return displayName;
        }
        return resource.getItemMeta().getDisplayName();
    }

    public List<SpecialtyRecipe> getRecipes() {
        return recipes;
    }

    public static Specialty fromString(String name) {
        if (name == null) return null;
        String normalized = name.trim().toUpperCase(Locale.ENGLISH).replace('-', '_');
        for (Specialty specialty : VALUES) {
            if (specialty.name().equals(normalized)) return specialty;
        }
        return null;
    }

    public static String joinedNames() {
        StringBuilder builder = new StringBuilder();
        for (Specialty specialty : VALUES) {
            if (builder.length() != 0) builder.append(", ");
            builder.append(specialty.configKey);
        }
        return builder.toString();
    }

    public static List<String> configKeys() {
        List<String> keys = new ArrayList<>(VALUES.length);
        for (Specialty specialty : VALUES) keys.add(specialty.configKey);
        return keys;
    }

    /** Re-reads every specialty from the configuration. */
    public static void reload() {
        ConfigAccessor root = SpecialtiesConfig.accessor();
        if (!root.isSet("specialties")) {
            SpecialtiesAddon.get().getLogger().severe("specialties.yml has no 'specialties' section.");
            return;
        }

        ConfigAccessor specialties = root.gotoSection("specialties");
        for (Specialty specialty : VALUES) {
            if (!specialties.isSet(specialty.configKey)) {
                SpecialtiesAddon.get().getLogger().severe(
                        "specialties.yml is missing the '" + specialty.configKey + "' specialty.");
                continue;
            }
            specialty.load(specialties.gotoSection(specialty.configKey));
        }
    }

    private void load(ConfigAccessor section) {
        this.displayName = section.isSet("display-name")
                ? TextUtil.colorize(section.getString("display-name"))
                : configKey;

        Material parsedIcon = section.isSet("icon") ? ItemParser.matchMaterial(section.getString("icon")) : null;
        this.icon = parsedIcon == null ? fallbackIcon : parsedIcon;

        List<String> lines = new ArrayList<>();
        if (section.isSet("description")) {
            for (String line : section.getStringList("description")) lines.add(TextUtil.colorize(line));
        }
        this.description = Collections.unmodifiableList(lines);

        if (section.isSet("resource")) {
            ItemStack parsed = ItemParser.parse(section.gotoSection("resource"), "The resource of " + configKey);
            if (parsed != null) {
                parsed.setAmount(1);
                this.resource = SpecialtyItems.tagResource(parsed, this);
            }
        }
        if (this.resource == null) {
            SpecialtiesAddon.get().getLogger().severe(
                    "The '" + configKey + "' specialty has no usable resource item, it will produce nothing.");
        }

        this.recipes = SpecialtyRecipe.parseAll(this, section);
    }
}
