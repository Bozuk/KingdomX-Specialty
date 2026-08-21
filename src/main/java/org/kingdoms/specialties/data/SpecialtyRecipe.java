package org.kingdoms.specialties.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.kingdoms.config.accessor.ConfigAccessor;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.locale.messenger.StaticMessenger;
import org.kingdoms.specialties.items.ItemFactory;
import org.kingdoms.specialties.items.PotionSupport;
import org.kingdoms.specialties.items.SpecialtyItems;

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
 *   <li>{@link Type#CRAFT} consumes a set of ingredients and hands out the item described by
 *       {@code result};</li>
 *   <li>{@link Type#TRANSMUTE} does the same, but one of the ingredients - the {@code source} - is
 *       a potion whose effect is carried over to the result. That is how a poison potion becomes a
 *       wither potion of the very same duration and level: the numbers are read off the bottle the
 *       player brought, not written in the configuration.</li>
 * </ul>
 */
public final class SpecialtyRecipe {
    /** Placeholder used in the config to reference the specialty's unique resource. */
    public static final String RESOURCE_TOKEN = "@resource";

    public enum Type {CRAFT, TRANSMUTE}

    private final Specialty specialty;
    private final String id;
    private final Type type;
    private final List<Ingredient> ingredients;
    private final Ingredient source;
    private final ItemStack result;
    private final PotionEffectType transmuteEffect;
    private final int amplifierShift;
    private final String displayName;
    private final List<String> description;
    private final Material icon;

    private SpecialtyRecipe(Specialty specialty, String id, Type type, List<Ingredient> ingredients,
                            Ingredient source, ItemStack result, PotionEffectType transmuteEffect,
                            int amplifierShift, String displayName, List<String> description, Material icon) {
        this.specialty = specialty;
        this.id = id;
        this.type = type;
        this.ingredients = ingredients;
        this.source = source;
        this.result = result;
        this.transmuteEffect = transmuteEffect;
        this.amplifierShift = amplifierShift;
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

    /** Every ingredient the recipe costs, the {@code source} of a transmutation included. */
    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    /** The potion a transmutation reads its duration and level from, {@code null} otherwise. */
    public Ingredient getSource() {
        return source;
    }

    /** @return a copy of the item as configured, before any transmutation. */
    public ItemStack getResult() {
        return result == null ? null : result.clone();
    }

    /**
     * Builds what the player actually receives.
     *
     * @param consumedSource the source potion taken from the inventory, for a transmutation.
     * @return the item, or {@code null} if the transmutation could not be resolved.
     */
    public ItemStack buildResult(ItemStack consumedSource) {
        if (result == null) return null;
        ItemStack built = result.clone();

        if (type == Type.TRANSMUTE) {
            PotionEffect origin = PotionSupport.findEffect(consumedSource, source.getEffectType());
            if (origin == null) {
                SpecialtiesAddon.get().getLogger().warning(
                        "The recipe '" + id + "' could not read the effect of the potion it consumed.");
                return null;
            }

            ItemMeta meta = built.getItemMeta();
            if (meta instanceof PotionMeta) {
                // Same duration, same level: only the effect - or its level - changes.
                ((PotionMeta) meta).addCustomEffect(new PotionEffect(transmuteEffect, origin.getDuration(),
                        Math.max(0, origin.getAmplifier() + amplifierShift)), true);
                built.setItemMeta(meta);
            } else {
                SpecialtiesAddon.get().getLogger().warning(
                        "The result of the recipe '" + id + "' is not a potion, nothing to transmute into.");
            }
        }

        return SpecialtyItems.tagForged(built, id);
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
        boolean transmuting = "TRANSMUTE".equals(rawType);

        List<Ingredient> ingredients = transmuting || "SHAPELESS".equals(rawType)
                ? parseFlatIngredients(specialty, id, section)
                : parseShapedIngredients(specialty, id, section);
        if (ingredients == null) return null;

        Ingredient source = null;
        PotionEffectType transmuteEffect = null;
        int amplifierShift = 0;

        if (transmuting) {
            source = parseSource(id, section);
            if (source == null) return null;

            if (!section.isSet("transmute")) {
                SpecialtiesAddon.get().getLogger().severe("The recipe '" + id + "' has no 'transmute' section.");
                return null;
            }

            ConfigAccessor transmute = section.gotoSection("transmute");
            transmuteEffect = transmute.isSet("effect")
                    ? ItemFactory.effect(transmute.getString("effect"))
                    : null;
            if (transmuteEffect == null) {
                SpecialtiesAddon.get().getLogger().severe(
                        "The recipe '" + id + "' transmutes into an unknown potion effect.");
                return null;
            }
            amplifierShift = transmute.isSet("amplifier-shift") ? transmute.getInt("amplifier-shift") : 0;

            // The source is an ingredient like any other: it shows up in the menu and gets consumed.
            List<Ingredient> all = new ArrayList<>(ingredients.size() + 1);
            all.add(source);
            all.addAll(ingredients);
            ingredients = Collections.unmodifiableList(all);
        }

        if (ingredients.isEmpty()) {
            SpecialtiesAddon.get().getLogger().severe("The recipe '" + id + "' has no usable ingredient.");
            return null;
        }

        String displayName = section.isSet("display-name")
                ? new StaticMessenger(section.getString("display-name")).parse()
                : null;

        List<String> description = new ArrayList<>();
        if (section.isSet("description")) {
            for (String line : section.getStringList("description")) {
                description.add(new StaticMessenger(line).parse());
            }
        }

        Material icon = section.isSet("icon") ? ItemFactory.material(section.getString("icon")) : null;

        if (!section.isSet("result")) {
            SpecialtiesAddon.get().getLogger().severe("The recipe '" + id + "' has no 'result' section.");
            return null;
        }

        ItemStack result = ItemFactory.parse(section.gotoSection("result"), "The result of the recipe " + id);
        if (result == null) return null;

        return new SpecialtyRecipe(specialty, id, transmuting ? Type.TRANSMUTE : Type.CRAFT, ingredients,
                source, result, transmuteEffect, amplifierShift, displayName,
                Collections.unmodifiableList(description), icon == null ? result.getType() : icon);
    }

    /** The {@code source} of a transmutation: one potion, recognised by the effect it carries. */
    private static Ingredient parseSource(String id, ConfigAccessor section) {
        if (!section.isSet("source")) {
            SpecialtiesAddon.get().getLogger().severe("The recipe '" + id + "' has no 'source' section.");
            return null;
        }

        ConfigAccessor source = section.gotoSection("source");
        Material material = source.isSet("material") ? ItemFactory.material(source.getString("material")) : null;
        if (material == null) {
            SpecialtiesAddon.get().getLogger().severe("The source of the recipe '" + id + "' has no known material.");
            return null;
        }

        PotionEffectType effect = source.isSet("effect")
                ? ItemFactory.effect(source.getString("effect"))
                : null;
        if (effect == null) {
            SpecialtiesAddon.get().getLogger().severe(
                    "The source of the recipe '" + id + "' must name a known potion 'effect'.");
            return null;
        }

        // Without an amplifier the recipe takes the effect at any level. With one it takes that
        // level only, which is what keeps "strength II becomes strength III" from ever laddering.
        Integer amplifier = source.isSet("amplifier") ? source.getInt("amplifier") : null;
        int amount = source.isSet("amount") ? Math.max(1, source.getInt("amount")) : 1;
        String name = source.isSet("name") ? new StaticMessenger(source.getString("name")).parse() : null;

        return new Ingredient(null, material, amount, effect, amplifier, name);
    }

    /** {@code ingredients} written as a plain list, duplicates meaning "more of it". */
    private static List<Ingredient> parseFlatIngredients(Specialty specialty, String id, ConfigAccessor section) {
        if (!section.isSet("ingredients")) return Collections.emptyList();

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
                ingredients.add(new Ingredient(specialty, null, entry.getValue(), null, null, null));
                continue;
            }

            Material material = ItemFactory.material(raw);
            if (material == null) {
                SpecialtiesAddon.get().getLogger().severe(
                        "The recipe '" + id + "' uses an unknown material: " + raw);
                return null;
            }
            ingredients.add(new Ingredient(null, material, entry.getValue(), null, null, null));
        }

        return Collections.unmodifiableList(ingredients);
    }

    /** One ingredient line of a recipe: the specialty resource, a plain material, or a potion. */
    public static final class Ingredient {
        private final Specialty resourceOf;
        private final Material material;
        private final int amount;
        private final PotionEffectType effectType;
        private final Integer effectAmplifier;
        private final String displayName;

        Ingredient(Specialty resourceOf, Material material, int amount,
                   PotionEffectType effectType, Integer effectAmplifier, String displayName) {
            this.resourceOf = resourceOf;
            this.material = material;
            this.amount = amount;
            this.effectType = effectType;
            this.effectAmplifier = effectAmplifier;
            this.displayName = displayName;
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

        /** The effect the item must carry, or {@code null} when the material is enough. */
        public PotionEffectType getEffectType() {
            return effectType;
        }

        /** The exact level that effect must be at, or {@code null} for any level. */
        public Integer getEffectAmplifier() {
            return effectAmplifier;
        }

        /** The name shown in the forge menu. */
        public String getDisplayName() {
            if (displayName != null) return displayName;
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
