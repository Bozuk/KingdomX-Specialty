package org.kingdoms.specialties.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.kingdoms.libs.xseries.XPotion;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.specialties.data.Specialty;

import java.util.Arrays;
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
 *   <li>{@code consume_effects} - the effects a food item hands out when eaten, in the XSeries
 *       {@code EFFECT, ticks, amplifier} form, one per line.</li>
 * </ul>
 */
public final class SpecialtyItems {
    private static final String EFFECT_SEPARATOR = "\n";

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
        return tag(item, resourceKey(), specialty.name());
    }

    /** @return the specialty this item is the resource of, or {@code null}. */
    public static Specialty getResourceSpecialty(ItemStack item) {
        return Specialty.fromString(read(item, resourceKey()));
    }

    /** Stamps the recipe an item came out of, so the forge never eats its own output. */
    public static ItemStack tagForged(ItemStack item, String recipeId) {
        return tag(item, forgedKey(), recipeId);
    }

    /** @return whether this item was forged at a specialty forge. */
    public static boolean isForged(ItemStack item) {
        return read(item, forgedKey()) != null;
    }

    // ----------------------------------------------------------- consume effects

    public static ItemStack tagConsumeEffects(ItemStack item, List<String> effects) {
        return tag(item, consumeEffectsKey(), String.join(EFFECT_SEPARATOR, effects));
    }

    /**
     * @return the effects this item hands out when eaten. Parsing on the way out rather than on the
     *         way in means an item keeps working across an XSeries update that widens the syntax.
     */
    public static List<XPotion.Effect> getConsumeEffects(ItemStack item) {
        String encoded = read(item, consumeEffectsKey());
        if (encoded == null || encoded.isEmpty()) return Collections.emptyList();

        return XPotion.parseEffects(Arrays.asList(encoded.split(EFFECT_SEPARATOR)));
    }

    // ------------------------------------------------------------------ plumbing

    private static ItemStack tag(ItemStack item, NamespacedKey key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    private static String read(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(key, PersistentDataType.STRING);
    }
}
