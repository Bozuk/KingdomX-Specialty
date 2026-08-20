package org.kingdoms.specialties.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.specialties.data.Specialty;

/**
 * Identity of the specialty resources. The specialty is stamped in the persistent data container,
 * so a resource stays recognisable no matter how it was renamed or re-textured, and an ordinary
 * item of the same material never passes for one.
 */
public final class SpecialtyItems {
    private static NamespacedKey resourceKey;

    private SpecialtyItems() {}

    public static NamespacedKey resourceKey() {
        if (resourceKey == null) resourceKey = new NamespacedKey(SpecialtiesAddon.get(), "specialty_resource");
        return resourceKey;
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
}
