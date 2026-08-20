package org.kingdoms.specialties.data;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.config.accessor.ConfigAccessor;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.specialties.items.ItemParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A recipe unlocked by a specialty, crafted at the kingdom's specialty forge.
 * <p>
 * Two flavours:
 * <ul>
 *   <li>{@link Type#CRAFT} produces an item;</li>
 *   <li>{@link Type#ENCHANT} stamps an enchantment - above the vanilla cap - onto the item the
 *       player is holding. Anvils clamp enchantments to their vanilla maximum, so this is the only
 *       way to hand out stronger ones.</li>
 * </ul>
 */
public final class SpecialtyRecipe {
    /** Placeholder used in the config to reference the specialty's unique resource. */
    public static final String RESOURCE_TOKEN = "@resource";

    public enum Type {CRAFT, ENCHANT}

    private final Specialty specialty;
    private final String id;
    private final Type type;
    private final List<Ingredient> ingredients;
    private final ItemStack result;
    private final Enchantment enchantment;
    private final int level;
    private final List<String> appliesTo;
    private final String displayName;
    private final List<String> description;
    private final Material icon;

    private SpecialtyRecipe(Specialty specialty, String id, Type type, List<Ingredient> ingredients,
                            ItemStack result, Enchantment enchantment, int level, List<String> appliesTo,
                            String displayName, List<String> description, Material icon) {
        this.specialty = specialty;
        this.id = id;
        this.type = type;
        this.ingredients = ingredients;
        this.result = result;
        this.enchantment = enchantment;
        this.level = level;
        this.appliesTo = appliesTo;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public Specialty getSpecialty() {
        return specialty;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    /** @return a copy of the crafted item, or {@code null} for an enchanting recipe. */
    public ItemStack getResult() {
        return result == null ? null : result.clone();
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    /** Whether this enchanting recipe accepts the given item. */
    public boolean accepts(ItemStack item) {
        if (type != Type.ENCHANT) return false;
        if (item == null || item.getType() == Material.AIR) return false;
        if (appliesTo.isEmpty()) return true;

        String material = item.getType().name();
        for (String pattern : appliesTo) {
            if ("ANY".equals(pattern)) return true;
            if (pattern.startsWith("*") && material.endsWith(pattern.substring(1))) return true;
            if (pattern.endsWith("*") && material.startsWith(pattern.substring(0, pattern.length() - 1))) return true;
            if (pattern.equals(material)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ parsing

    /** Reads the {@code recipes} section of a specialty. */
    public static List<SpecialtyRecipe> parseAll(Specialty specialty, ConfigAccessor specialtySection) {
        if (!specialtySection.isSet("recipes")) return Collections.emptyList();

        ConfigAccessor recipes = specialtySection.gotoSection("recipes");
        List<SpecialtyRecipe> parsed = new ArrayList<>();

        for (String id : recipes.getKeys()) {
            try {
                SpecialtyRecipe recipe = parse(specialty, id, recipes.gotoSection(id));
                if (recipe != null) parsed.add(recipe);
            } catch (RuntimeException ex) {
                SpecialtiesAddon.get().getLogger().severe(
                        "Failed to load the recipe '" + id + "' of the '" + specialty.getConfigKey()
                                + "' specialty: " + ex.getMessage());
            }
        }

        return Collections.unmodifiableList(parsed);
    }

    private static SpecialtyRecipe parse(Specialty specialty, String id, ConfigAccessor section) {
        String rawType = section.isSet("type") ? section.getString("type").trim().toUpperCase(Locale.ENGLISH) : "SHAPED";
        // SHAPED and SHAPELESS both describe a set of ingredients: the forge has no crafting grid,
        // so a shape is only read to count how many of each ingredient it needs.
        boolean enchanting = "ENCHANT".equals(rawType);

        List<Ingredient> ingredients = enchanting || "SHAPELESS".equals(rawType)
                ? parseFlatIngredients(specialty, id, section)
                : parseShapedIngredients(specialty, id, section);
        if (ingredients == null || ingredients.isEmpty()) {
            SpecialtiesAddon.get().getLogger().severe("The recipe '" + id + "' has no usable ingredient.");
            return null;
        }

        String displayName = section.isSet("display-name")
                ? org.kingdoms.specialties.items.TextUtil.colorize(section.getString("display-name"))
                : null;

        List<String> description = new ArrayList<>();
        if (section.isSet("description")) {
            for (String line : section.getStringList("description")) {
                description.add(org.kingdoms.specialties.items.TextUtil.colorize(line));
            }
        }

        Material icon = section.isSet("icon") ? ItemParser.matchMaterial(section.getString("icon")) : null;

        if (enchanting) {
            if (!section.isSet("enchantment")) {
                SpecialtiesAddon.get().getLogger().severe("The recipe '" + id + "' has no 'enchantment'.");
                return null;
            }

            Enchantment enchantment = ItemParser.matchEnchantment(section.getString("enchantment"));
            if (enchantment == null) {
                SpecialtiesAddon.get().getLogger().severe(
                        "The recipe '" + id + "' uses an unknown enchantment: " + section.getString("enchantment"));
                return null;
            }

            int level = Math.max(1, section.isSet("level") ? section.getInt("level") : 1);

            List<String> appliesTo = new ArrayList<>();
            if (section.isSet("applies-to")) {
                for (String pattern : section.getStringList("applies-to")) {
                    appliesTo.add(pattern.trim().toUpperCase(Locale.ENGLISH));
                }
            }

            return new SpecialtyRecipe(specialty, id, Type.ENCHANT, ingredients, null, enchantment, level,
                    Collections.unmodifiableList(appliesTo), displayName,
                    Collections.unmodifiableList(description), icon == null ? Material.ENCHANTED_BOOK : icon);
        }

        if (!section.isSet("result")) {
            SpecialtiesAddon.get().getLogger().severe("The recipe '" + id + "' has no 'result' section.");
            return null;
        }

        ItemStack result = ItemParser.parse(section.gotoSection("result"), "The result of the recipe '" + id + '\'');
        if (result == null) return null;

        return new SpecialtyRecipe(specialty, id, Type.CRAFT, ingredients, result, null, 0,
                Collections.<String>emptyList(), displayName, Collections.unmodifiableList(description),
                icon == null ? result.getType() : icon);
    }

    /** {@code ingredients} written as a plain list, duplicates meaning "more of it". */
    private static List<Ingredient> parseFlatIngredients(Specialty specialty, String id, ConfigAccessor section) {
        if (!section.isSet("ingredients")) return null;

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String raw : section.getStringList("ingredients")) {
            merge(counts, raw, 1);
        }
        return build(specialty, id, counts);
    }

    /** {@code shape} + {@code ingredients}: every non-space character costs one of its ingredient. */
    private static List<Ingredient> parseShapedIngredients(Specialty specialty, String id, ConfigAccessor section) {
        if (!section.isSet("shape") || !section.isSet("ingredients")) {
            SpecialtiesAddon.get().getLogger().severe("The recipe '" + id + "' needs both 'shape' and 'ingredients'.");
            return null;
        }

        ConfigAccessor letters = section.gotoSection("ingredients");
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (String row : section.getStringList("shape")) {
            for (char character : row.toCharArray()) {
                if (character == ' ') continue;

                String letter = String.valueOf(character);
                if (!letters.isSet(letter)) {
                    SpecialtiesAddon.get().getLogger().severe(
                            "The shape of '" + id + "' uses '" + letter + "' but no such ingredient is defined.");
                    return null;
                }
                merge(counts, letters.getString(letter), 1);
            }
        }
        return build(specialty, id, counts);
    }

    private static void merge(Map<String, Integer> counts, String raw, int amount) {
        if (raw == null) return;
        String key = raw.trim();
        Integer current = counts.get(key);
        counts.put(key, current == null ? amount : current + amount);
    }

    private static List<Ingredient> build(Specialty specialty, String id, Map<String, Integer> counts) {
        List<Ingredient> ingredients = new ArrayList<>(counts.size());

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String raw = entry.getKey();

            if (RESOURCE_TOKEN.equalsIgnoreCase(raw)) {
                ingredients.add(new Ingredient(specialty, null, entry.getValue()));
                continue;
            }

            Material material = ItemParser.matchMaterial(raw);
            if (material == null) {
                SpecialtiesAddon.get().getLogger().severe(
                        "The recipe '" + id + "' uses an unknown material: " + raw);
                return null;
            }
            ingredients.add(new Ingredient(null, material, entry.getValue()));
        }

        return Collections.unmodifiableList(ingredients);
    }

    /** One ingredient line of a recipe: either the specialty resource, or a plain material. */
    public static final class Ingredient {
        private final Specialty resourceOf;
        private final Material material;
        private final int amount;

        Ingredient(Specialty resourceOf, Material material, int amount) {
            this.resourceOf = resourceOf;
            this.material = material;
            this.amount = amount;
        }

        public boolean isSpecialtyResource() {
            return resourceOf != null;
        }

        public Specialty getResourceOf() {
            return resourceOf;
        }

        public Material getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }

        /** The name shown in the forge menu. */
        public String getDisplayName() {
            if (resourceOf != null) return resourceOf.getResourceName();

            StringBuilder builder = new StringBuilder(material.name().length());
            for (String word : material.name().split("_")) {
                if (word.isEmpty()) continue;
                if (builder.length() != 0) builder.append(' ');
                builder.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ENGLISH));
            }
            return builder.toString();
        }
    }
}
