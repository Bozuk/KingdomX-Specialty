package org.kingdoms.specialties.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.specialties.data.Specialty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Identity of the addon's items. Everything is stamped in the persistent data container, so an item
 * stays recognisable no matter how it was renamed or re-textured, and an ordinary item of the same
 * material never passes for one.
 * <ul>
 *   <li>{@code specialty_resource} - which specialty this resource belongs to;</li>
 *   <li>{@code forged_by} - the recipe an item came out of. A plain material ingredient refuses
 *       these, so upgrading an already upgraded netherite sword is not a way to waste a stack of
 *       the kingdom's resource;</li>
 *   <li>{@code consume_effects} - the effects a food item hands out when eaten. Food effects are
 *       hardcoded in the game, so a golden apple cannot be made stronger through its meta: the
 *       effects are read back from here when the item is consumed.</li>
 * </ul>
 */
public final class SpecialtyItems {
    private static NamespacedKey resourceKey;
    private static NamespacedKey forgedKey;
    private static NamespacedKey consumeEffectsKey;

    private SpecialtyItems() {}

    public static NamespacedKey resourceKey() {
        if (resourceKey == null) resourceKey = new NamespacedKey(SpecialtiesAddon.get(), "specialty_resource");
        return resourceKey;
    }

    public static NamespacedKey forgedKey() {
        if (forgedKey == null) forgedKey = new NamespacedKey(SpecialtiesAddon.get(), "forged_by");
        return forgedKey;
    }

    public static NamespacedKey consumeEffectsKey() {
        if (consumeEffectsKey == null) {
            consumeEffectsKey = new NamespacedKey(SpecialtiesAddon.get(), "consume_effects");
        }
        return consumeEffectsKey;
    }

    /** Stamps the specialty this resource belongs to onto the item. */
    public static ItemStack tagResource(ItemStack item, Specialty specialty) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(resourceKey(), PersistentDataType.STRING, specialty.name());
        item.setItemMeta(meta);
        return item;
    }

    /** @return the specialty this item is the resource of, or {@code null}. */
    public static Specialty getResourceSpecialty(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String raw = container.get(resourceKey(), PersistentDataType.STRING);
        return raw == null ? null : Specialty.fromString(raw);
    }

    /** Stamps the recipe an item came out of, so the forge never eats its own output. */
    public static ItemStack tagForged(ItemStack item, String recipeId) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(forgedKey(), PersistentDataType.STRING, recipeId);
        item.setItemMeta(meta);
        return item;
    }

    /** @return whether this item was forged at a specialty forge. */
    public static boolean isForged(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(forgedKey(), PersistentDataType.STRING);
    }

    // ----------------------------------------------------------- consume effects

    /** Stores the effects as {@code key:ticks:amplifier}, separated by semicolons. */
    public static void setConsumeEffects(ItemMeta meta, List<PotionEffect> effects) {
        if (effects.isEmpty()) return;

        StringBuilder encoded = new StringBuilder();
        for (PotionEffect effect : effects) {
            if (encoded.length() != 0) encoded.append(';');
            encoded.append(nameOf(effect.getType()))
                    .append(':').append(effect.getDuration())
                    .append(':').append(effect.getAmplifier());
        }
        meta.getPersistentDataContainer().set(consumeEffectsKey(), PersistentDataType.STRING, encoded.toString());
    }

    /** @return the effects this item hands out when eaten or drunk. Never {@code null}. */
    public static List<PotionEffect> getConsumeEffects(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Collections.emptyList();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Collections.emptyList();

        String encoded = meta.getPersistentDataContainer().get(consumeEffectsKey(), PersistentDataType.STRING);
        if (encoded == null || encoded.isEmpty()) return Collections.emptyList();

        List<PotionEffect> effects = new ArrayList<>(2);
        for (String entry : encoded.split(";")) {
            String[] parts = entry.split(":");
            if (parts.length != 3) continue;

            PotionEffectType type = ItemParser.matchPotionEffect(parts[0]);
            if (type == null) continue;

            try {
                effects.add(new PotionEffect(type,
                        Math.max(1, Integer.parseInt(parts[1])),
                        Math.max(0, Integer.parseInt(parts[2]))));
            } catch (NumberFormatException ignored) {
                // A hand-edited item, skipped rather than crashing the meal.
            }
        }
        return effects;
    }

    /** Whatever name this server knows the effect by, see {@link PotionSupport#keyOf}. */
    private static String nameOf(PotionEffectType type) {
        return PotionSupport.keyOf(type);
    }
}
