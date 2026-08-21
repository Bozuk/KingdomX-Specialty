package org.kingdoms.specialties.items;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads what a potion item would actually do once drunk: its custom effects, and - for an ordinary
 * brewed potion - the effect its base type stands for.
 * <p>
 * Everything goes through reflection, for the same reason as {@link AttributeSupport}. 1.20.5
 * replaced {@code PotionMeta#getBasePotionData()} - which returns a {@code PotionData} carrying
 * {@code extended} and {@code upgraded} flags - with {@code getBasePotionType()}, where every
 * variant is its own {@code PotionType} that knows its effects. Both paths are tried, so the addon
 * keeps reading potions correctly from 1.16 to today.
 */
public final class PotionSupport {
    /**
     * Vanilla brewing durations in ticks, {@code {normal, extended, upgraded}}, {@code -1} when the
     * variant cannot be brewed. Only needed on the legacy path: a {@code PotionData} says which
     * variant it is but not how long it lasts.
     */
    private static final Map<String, int[]> LEGACY_DURATIONS = new HashMap<>();

    static {
        LEGACY_DURATIONS.put("SPEED", new int[]{3600, 9600, 1800});
        LEGACY_DURATIONS.put("SLOWNESS", new int[]{1800, 4800, 400});
        LEGACY_DURATIONS.put("STRENGTH", new int[]{3600, 9600, 1800});
        LEGACY_DURATIONS.put("JUMP", new int[]{3600, 9600, 1800});
        LEGACY_DURATIONS.put("REGEN", new int[]{900, 1800, 450});
        LEGACY_DURATIONS.put("REGENERATION", new int[]{900, 1800, 450});
        LEGACY_DURATIONS.put("POISON", new int[]{900, 1800, 432});
        LEGACY_DURATIONS.put("WEAKNESS", new int[]{1800, 4800, -1});
        LEGACY_DURATIONS.put("FIRE_RESISTANCE", new int[]{3600, 9600, -1});
        LEGACY_DURATIONS.put("WATER_BREATHING", new int[]{3600, 9600, -1});
        LEGACY_DURATIONS.put("INVISIBILITY", new int[]{3600, 9600, -1});
        LEGACY_DURATIONS.put("NIGHT_VISION", new int[]{3600, 9600, -1});
        LEGACY_DURATIONS.put("SLOW_FALLING", new int[]{1800, 4800, -1});
        LEGACY_DURATIONS.put("TURTLE_MASTER", new int[]{400, 800, 400});
        LEGACY_DURATIONS.put("LUCK", new int[]{6000, -1, -1});
    }

    private PotionSupport() {}

    /**
     * @return the effect of that type the item would apply, or {@code null} if it carries none.
     *         Custom effects win over the base potion, exactly like the game applies them.
     */
    public static PotionEffect findEffect(ItemStack item, PotionEffectType type) {
        if (item == null || type == null) return null;
        for (PotionEffect effect : effectsOf(item)) {
            if (isSame(effect.getType(), type)) return effect;
        }
        return null;
    }

    /** @return every effect the item would apply, custom ones first. Never {@code null}. */
    public static List<PotionEffect> effectsOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Collections.emptyList();

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof PotionMeta)) return Collections.emptyList();
        PotionMeta potion = (PotionMeta) meta;

        List<PotionEffect> effects = new ArrayList<>(potion.getCustomEffects());
        effects.addAll(baseEffects(potion));
        return effects;
    }

    /** Two effect types can be different objects for the same effect once a registry is involved. */
    public static boolean isSame(PotionEffectType left, PotionEffectType right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return keyOf(left).equals(keyOf(right));
    }

    /**
     * The stable, upper case name of an effect - {@code STRENGTH} on a modern server,
     * {@code INCREASE_DAMAGE} on an older one. {@code PotionEffectType} only became
     * {@code Keyed} along the way, hence the reflection.
     */
    @SuppressWarnings("deprecation")
    public static String keyOf(PotionEffectType type) {
        if (type == null) return "";
        try {
            Object key = PotionEffectType.class.getMethod("getKey").invoke(type);
            if (key != null) {
                String raw = key.toString();
                int colon = raw.indexOf(':');
                return (colon < 0 ? raw : raw.substring(colon + 1)).toUpperCase(Locale.ENGLISH);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Server without a keyed PotionEffectType, the legacy name it is.
        }

        String name = type.getName();
        return name == null ? "" : name.toUpperCase(Locale.ENGLISH);
    }

    // -------------------------------------------------------------- base potion

    private static List<PotionEffect> baseEffects(PotionMeta meta) {
        List<PotionEffect> modern = modernBaseEffects(meta);
        if (modern != null) return modern;

        List<PotionEffect> legacy = legacyBaseEffects(meta);
        return legacy == null ? Collections.<PotionEffect>emptyList() : legacy;
    }

    /** 1.20.5+: every variant is its own {@code PotionType} and knows its own effects. */
    @SuppressWarnings("unchecked")
    private static List<PotionEffect> modernBaseEffects(PotionMeta meta) {
        try {
            Method getBasePotionType = PotionMeta.class.getMethod("getBasePotionType");
            Object type = getBasePotionType.invoke(meta);
            if (type == null) return Collections.emptyList();

            Method getPotionEffects = PotionType.class.getMethod("getPotionEffects");
            Object effects = getPotionEffects.invoke(type);
            return effects == null ? Collections.<PotionEffect>emptyList() : (List<PotionEffect>) effects;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return null;
        }
    }

    /** 1.16 - 1.20.4: a base type plus the {@code extended} / {@code upgraded} flags. */
    private static List<PotionEffect> legacyBaseEffects(PotionMeta meta) {
        try {
            Object data = PotionMeta.class.getMethod("getBasePotionData").invoke(meta);
            if (data == null) return Collections.emptyList();

            Class<?> dataClass = data.getClass();
            Object type = dataClass.getMethod("getType").invoke(data);
            if (type == null) return Collections.emptyList();

            boolean extended = Boolean.TRUE.equals(dataClass.getMethod("isExtended").invoke(data));
            boolean upgraded = Boolean.TRUE.equals(dataClass.getMethod("isUpgraded").invoke(data));

            Object effectType = PotionType.class.getMethod("getEffectType").invoke(type);
            if (!(effectType instanceof PotionEffectType)) return Collections.emptyList();

            String name = ((Enum<?>) type).name().toUpperCase(Locale.ENGLISH);
            int[] durations = LEGACY_DURATIONS.get(name);
            if (durations == null) return Collections.emptyList();

            int ticks = durations[0];
            if (upgraded && durations[2] > 0) ticks = durations[2];
            else if (extended && durations[1] > 0) ticks = durations[1];
            if (ticks <= 0) return Collections.emptyList();

            return Collections.singletonList(
                    new PotionEffect((PotionEffectType) effectType, ticks, upgraded ? 1 : 0));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return null;
        }
    }
}
