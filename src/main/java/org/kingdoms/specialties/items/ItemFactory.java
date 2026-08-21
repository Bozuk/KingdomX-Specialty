package org.kingdoms.specialties.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.kingdoms.config.accessor.ConfigAccessor;
import org.kingdoms.libs.xseries.XMaterial;
import org.kingdoms.libs.xseries.XPotion;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.utils.KingdomsItemDeserializer;

import java.util.List;

/**
 * Builds the addon's items out of its configuration.
 * <p>
 * The work belongs to KingdomsX. {@link KingdomsItemDeserializer} wraps XSeries'
 * {@code XItemStack} deserializer, so every option the main plugin understands works here with the
 * same syntax - material, amount, name, lore, enchants, stored-enchants, flags, attributes, potion
 * effects, colours, custom model data, trims, skulls, NBT - and keeps working when a Minecraft
 * release moves the goalposts. XSeries is where the cross-version knowledge lives; rewriting it by
 * hand only means owning the version drift.
 * <p>
 * Two options are the addon's own, for want of an equivalent:
 * <ul>
 *   <li>{@code glint} - the enchanted shimmer through the component 1.20.5 added for it. XSeries'
 *       {@code glow} still does it with a hidden enchantment, and that really does change the item:
 *       a specialty weapon would silently gain Unbreaking I, so "the same stats as netherite" would
 *       stop being true. {@code glow} remains available for whoever wants the old behaviour.</li>
 *   <li>{@code consume-effects} - what a food item hands out when eaten. Food effects are hardcoded
 *       in the game and cannot live in an item's meta, so they are stamped into its persistent data
 *       and applied by {@link org.kingdoms.specialties.managers.ConsumeListener}.</li>
 * </ul>
 */
public final class ItemFactory {
    private ItemFactory() {}

    /**
     * @param section the section describing the item.
     * @param what    a human readable identifier used in the warnings.
     * @return the built item, or {@code null} if the section could not be read.
     */
    public static ItemStack parse(ConfigAccessor section, String what) {
        ItemStack item;
        try {
            item = new KingdomsItemDeserializer()
                    .withSection(section)
                    // Without this, an option the deserializer chokes on is dropped in silence.
                    .withRestart(ex -> SpecialtiesAddon.get().getLogger().warning(
                            what + " has an option that could not be read: " + ex))
                    .deserialize();
        } catch (RuntimeException | LinkageError ex) {
            SpecialtiesAddon.get().getLogger().severe(what + " could not be built: " + ex);
            return null;
        }

        if (item == null) {
            SpecialtiesAddon.get().getLogger().severe(what + " describes no item.");
            return null;
        }

        applyGlint(item, section, what);
        applyConsumeEffects(item, section, what);
        return item;
    }

    /**
     * A material by any of the names it ever had. XSeries maps the legacy spellings onto whatever
     * the running server calls the thing, and answers for materials this addon was compiled before
     * - the netherite spear among them.
     *
     * @return the material, or {@code null} if this server has no such thing.
     */
    public static Material material(String name) {
        if (name == null) return null;
        return XMaterial.matchXMaterial(name.trim())
                .map(XMaterial::parseMaterial)
                .orElse(null);
    }

    /** A potion effect by any of its names: 1.20.5 renamed most of them. */
    public static PotionEffectType effect(String name) {
        if (name == null) return null;
        return XPotion.of(name.trim())
                .map(XPotion::getPotionEffectType)
                .orElse(null);
    }

    /** The shimmer of an enchanted item, without an enchantment. */
    private static void applyGlint(ItemStack item, ConfigAccessor section, String what) {
        if (!section.isSet("glint") || !section.getBoolean("glint")) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        try {
            ItemMeta.class.getMethod("setEnchantmentGlintOverride", Boolean.class).invoke(meta, Boolean.TRUE);
            item.setItemMeta(meta);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            SpecialtiesAddon.get().getLogger().warning(what
                    + " uses 'glint', which needs a 1.20.5+ server. Use 'glow' instead on this one,"
                    + " keeping in mind that it adds a hidden Unbreaking I to the item.");
        }
    }

    /**
     * Effects are kept as written - the XSeries {@code EFFECT, ticks, amplifier} form - and only
     * checked here, so a typo is a startup warning rather than a meal that quietly does nothing.
     */
    private static void applyConsumeEffects(ItemStack item, ConfigAccessor section, String what) {
        if (!section.isSet("consume-effects")) return;

        List<String> effects = section.getStringList("consume-effects");
        if (effects.isEmpty()) return;

        for (String effect : effects) {
            if (XPotion.parseEffect(effect) == null) {
                SpecialtiesAddon.get().getLogger().warning(
                        what + " has an unreadable consume effect: " + effect);
            }
        }

        SpecialtyItems.tagConsumeEffects(item, effects);
    }
}
