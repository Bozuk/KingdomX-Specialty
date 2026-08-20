package org.kingdoms.specialties.items;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.kingdoms.config.accessor.ConfigAccessor;
import org.kingdoms.specialties.SpecialtiesAddon;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds {@link ItemStack}s out of the addon's configuration sections.
 * <p>
 * Only plain Bukkit API is used on purpose: the addon must keep working across KingdomsX updates,
 * so no shaded internal library is touched here.
 */
public final class ItemParser {
    /** Legacy effect name -> the name used since 1.20.5. */
    private static final Map<String, String> MODERN_EFFECT_NAMES = new HashMap<>();

    static {
        MODERN_EFFECT_NAMES.put("INCREASE_DAMAGE", "STRENGTH");
        MODERN_EFFECT_NAMES.put("DAMAGE_RESISTANCE", "RESISTANCE");
        MODERN_EFFECT_NAMES.put("SLOW", "SLOWNESS");
        MODERN_EFFECT_NAMES.put("SLOW_DIGGING", "MINING_FATIGUE");
        MODERN_EFFECT_NAMES.put("FAST_DIGGING", "HASTE");
        MODERN_EFFECT_NAMES.put("HEAL", "INSTANT_HEALTH");
        MODERN_EFFECT_NAMES.put("HARM", "INSTANT_DAMAGE");
        MODERN_EFFECT_NAMES.put("JUMP", "JUMP_BOOST");
        MODERN_EFFECT_NAMES.put("CONFUSION", "NAUSEA");
        MODERN_EFFECT_NAMES.put("DAMAGE_RESISTANCE", "RESISTANCE");
    }

    private ItemParser() {}

    /**
     * @param section the section describing the item.
     * @param what    a human readable identifier used in the warnings.
     * @return the built item, or {@code null} if the material is missing or unknown.
     */
    public static ItemStack parse(ConfigAccessor section, String what) {
        String materialName = section.getString("material");
        if (materialName == null) {
            warn(what + " has no 'material' option.");
            return null;
        }

        Material material = matchMaterial(materialName);
        if (material == null) {
            warn(what + " uses an unknown material: " + materialName);
            return null;
        }

        int amount = section.isSet("amount") ? Math.max(1, section.getInt("amount")) : 1;
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (section.isSet("name")) {
            meta.setDisplayName(TextUtil.colorize(section.getString("name")));
        }

        if (section.isSet("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) lore.add(TextUtil.colorize(line));
            meta.setLore(lore);
        }

        if (section.isSet("unbreakable")) {
            meta.setUnbreakable(section.getBoolean("unbreakable"));
        }

        if (section.isSet("custom-model-data")) {
            setCustomModelData(meta, section.getInt("custom-model-data"));
        }

        if (section.isSet("enchants")) {
            ConfigAccessor enchants = section.gotoSection("enchants");
            for (String name : enchants.getKeys()) {
                Enchantment enchantment = matchEnchantment(name);
                if (enchantment == null) {
                    warn(what + " uses an unknown enchantment: " + name);
                    continue;
                }
                meta.addEnchant(enchantment, Math.max(1, enchants.getInt(name)), true);
            }
        }

        if (section.isSet("stored-enchants") && meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta book = (EnchantmentStorageMeta) meta;
            ConfigAccessor stored = section.gotoSection("stored-enchants");
            for (String name : stored.getKeys()) {
                Enchantment enchantment = matchEnchantment(name);
                if (enchantment == null) {
                    warn(what + " uses an unknown enchantment: " + name);
                    continue;
                }
                // ignoreLevelRestriction: that's the whole point of the enchanter specialty.
                book.addStoredEnchant(enchantment, Math.max(1, stored.getInt(name)), true);
            }
        }

        if (section.isSet("attributes")) {
            ConfigAccessor attributes = section.gotoSection("attributes");
            for (String name : attributes.getKeys()) {
                ConfigAccessor attribute = attributes.gotoSection(name);
                AttributeSupport.apply(meta, name,
                        attribute.getDouble("amount"),
                        attribute.isSet("operation") ? attribute.getString("operation") : null,
                        attribute.isSet("slot") ? attribute.getString("slot") : null,
                        sanitizeId(what + '_' + name));
            }
        }

        if (section.isSet("glow") && section.getBoolean("glow") && meta.getEnchants().isEmpty()) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (section.isSet("item-flags")) {
            for (String flagName : section.getStringList("item-flags")) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase(Locale.ENGLISH)));
                } catch (IllegalArgumentException ex) {
                    warn(what + " uses an unknown item flag: " + flagName);
                }
            }
        }

        if (section.isSet("potion") && meta instanceof PotionMeta) {
            applyPotion((PotionMeta) meta, section.gotoSection("potion"), what);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static void applyPotion(PotionMeta meta, ConfigAccessor section, String what) {
        if (section.isSet("color")) {
            Color color = TextUtil.parseColor(section.getString("color"));
            if (color != null) meta.setColor(color);
        }

        if (!section.isSet("effects")) return;
        ConfigAccessor effects = section.gotoSection("effects");
        for (String name : effects.getKeys()) {
            PotionEffectType type = matchPotionEffect(name);
            if (type == null) {
                warn(what + " uses an unknown potion effect: " + name);
                continue;
            }

            ConfigAccessor effect = effects.gotoSection(name);
            Duration duration = effect.get("duration").getTime();
            int ticks = duration == null ? 600 : (int) (duration.toMillis() / 50L);
            int amplifier = effect.isSet("amplifier") ? effect.getInt("amplifier") : 0;
            meta.addCustomEffect(new PotionEffect(type, Math.max(1, ticks), Math.max(0, amplifier)), true);
        }
    }

    /** Modifier ids must be usable as a NamespacedKey on modern servers. */
    private static String sanitizeId(String raw) {
        String id = raw.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9_]", "_");
        return id.isEmpty() ? "modifier" : id;
    }

    public static Material matchMaterial(String name) {
        if (name == null) return null;
        return Material.matchMaterial(name.trim().toUpperCase(Locale.ENGLISH));
    }

    @SuppressWarnings("deprecation")
    public static Enchantment matchEnchantment(String name) {
        String normalized = name.trim().toUpperCase(Locale.ENGLISH);
        Enchantment byKey = Enchantment.getByKey(NamespacedKey.minecraft(normalized.toLowerCase(Locale.ENGLISH)));
        if (byKey != null) return byKey;
        return Enchantment.getByName(normalized);
    }

    /**
     * 1.20.5 renamed the effects ({@code INCREASE_DAMAGE} became {@code strength}) and moved them
     * to a registry, while {@code getByKey} doesn't exist on the older versions this addon still
     * supports. Both spellings are therefore accepted.
     */
    @SuppressWarnings("deprecation")
    private static PotionEffectType matchPotionEffect(String name) {
        String normalized = name.trim().toUpperCase(Locale.ENGLISH);

        PotionEffectType byName = PotionEffectType.getByName(normalized);
        if (byName != null) return byName;

        PotionEffectType byRegistry = fromRegistry(normalized);
        if (byRegistry != null) return byRegistry;

        String alias = MODERN_EFFECT_NAMES.get(normalized);
        if (alias == null) return null;

        byName = PotionEffectType.getByName(alias);
        return byName == null ? fromRegistry(alias) : byName;
    }

    private static PotionEffectType fromRegistry(String name) {
        try {
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Object registry = registryClass.getField("EFFECT").get(null);
            java.lang.reflect.Method get = registryClass.getMethod("get", org.bukkit.NamespacedKey.class);
            return (PotionEffectType) get.invoke(registry,
                    NamespacedKey.minecraft(name.toLowerCase(Locale.ENGLISH)));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    /**
     * {@code setCustomModelData} only exists since 1.14, the addon still supports older servers.
     */
    private static void setCustomModelData(ItemMeta meta, int data) {
        try {
            ItemMeta.class.getMethod("setCustomModelData", Integer.class).invoke(meta, data);
        } catch (ReflectiveOperationException ignored) {
            // Server older than 1.14, silently skipped.
        }
    }

    private static void warn(String message) {
        SpecialtiesAddon.get().getLogger().warning(message);
    }
}
