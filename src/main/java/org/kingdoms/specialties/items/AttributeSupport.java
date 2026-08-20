package org.kingdoms.specialties.items;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.kingdoms.specialties.SpecialtiesAddon;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

/**
 * Applies attribute modifiers to an item so a specialty weapon or armor can genuinely outclass
 * netherite instead of only carrying better enchantments.
 * <p>
 * The whole thing goes through reflection on purpose. Between 1.16 and 1.21 Bukkit changed both
 * how attributes are looked up (enum, then registry, and the {@code GENERIC_} prefix was dropped)
 * and how modifiers are built ({@code UUID + EquipmentSlot}, then
 * {@code NamespacedKey + EquipmentSlotGroup}). Binding to either one at compile time would break
 * the addon on half of the supported servers.
 */
public final class AttributeSupport {
    private static Boolean unsupported;

    private AttributeSupport() {}

    /**
     * @param slotName the equipment slot the modifier applies to, or {@code null} for all slots.
     * @return {@code true} if the modifier was added.
     */
    public static boolean apply(ItemMeta meta, String attributeName, double amount,
                                String operationName, String slotName, String modifierId) {
        Attribute attribute = matchAttribute(attributeName);
        if (attribute == null) {
            warn("Unknown attribute: " + attributeName);
            return false;
        }

        AttributeModifier.Operation operation;
        try {
            operation = operationName == null
                    ? AttributeModifier.Operation.ADD_NUMBER
                    : AttributeModifier.Operation.valueOf(operationName.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            warn("Unknown attribute operation: " + operationName);
            return false;
        }

        EquipmentSlot slot = null;
        if (slotName != null) {
            try {
                slot = EquipmentSlot.valueOf(slotName.trim().toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException ex) {
                warn("Unknown equipment slot: " + slotName);
            }
        }

        AttributeModifier modifier = createModifier(modifierId, amount, operation, slot);
        if (modifier == null) return false;

        meta.addAttributeModifier(attribute, modifier);
        return true;
    }

    // ---------------------------------------------------------------- attribute

    private static Attribute matchAttribute(String rawName) {
        if (rawName == null) return null;
        String name = rawName.trim().toUpperCase(Locale.ENGLISH).replace('.', '_');

        // 1.21.3+ dropped the GENERIC_ prefix, older versions require it.
        Attribute attribute = lookup(name);
        if (attribute != null) return attribute;

        if (name.startsWith("GENERIC_")) return lookup(name.substring("GENERIC_".length()));
        return lookup("GENERIC_" + name);
    }

    private static Attribute lookup(String name) {
        Attribute byEnum = byEnum(name);
        if (byEnum != null) return byEnum;
        return byRegistry(name);
    }

    private static Attribute byEnum(String name) {
        try {
            Method valueOf = Attribute.class.getMethod("valueOf", String.class);
            return (Attribute) valueOf.invoke(null, name);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static Attribute byRegistry(String name) {
        try {
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Object registry = registryClass.getField("ATTRIBUTE").get(null);
            Method get = registryClass.getMethod("get", NamespacedKey.class);
            return (Attribute) get.invoke(registry, NamespacedKey.minecraft(name.toLowerCase(Locale.ENGLISH)));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    // ----------------------------------------------------------------- modifier

    private static AttributeModifier createModifier(String id, double amount,
                                                    AttributeModifier.Operation operation, EquipmentSlot slot) {
        AttributeModifier modern = createModern(id, amount, operation, slot);
        if (modern != null) return modern;

        AttributeModifier legacy = createLegacy(id, amount, operation, slot);
        if (legacy != null) return legacy;

        if (unsupported == null) {
            unsupported = Boolean.TRUE;
            warn("This server exposes no known AttributeModifier constructor, "
                    + "the 'attributes' options of specialties.yml are ignored.");
        }
        return null;
    }

    /** 1.21+: {@code AttributeModifier(NamespacedKey, double, Operation, EquipmentSlotGroup)}. */
    private static AttributeModifier createModern(String id, double amount,
                                                  AttributeModifier.Operation operation, EquipmentSlot slot) {
        try {
            Class<?> slotGroupClass = Class.forName("org.bukkit.inventory.EquipmentSlotGroup");
            Constructor<AttributeModifier> constructor = AttributeModifier.class
                    .getConstructor(NamespacedKey.class, double.class, AttributeModifier.Operation.class, slotGroupClass);

            Object slotGroup;
            if (slot == null) {
                slotGroup = slotGroupClass.getField("ANY").get(null);
            } else {
                Object fromSlot = EquipmentSlot.class.getMethod("getGroup").invoke(slot);
                slotGroup = fromSlot == null ? slotGroupClass.getField("ANY").get(null) : fromSlot;
            }

            NamespacedKey key = new NamespacedKey(SpecialtiesAddon.get(), id.toLowerCase(Locale.ENGLISH));
            return constructor.newInstance(key, amount, operation, slotGroup);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return null;
        }
    }

    /** 1.16 - 1.20: {@code AttributeModifier(UUID, String, double, Operation, EquipmentSlot)}. */
    private static AttributeModifier createLegacy(String id, double amount,
                                                  AttributeModifier.Operation operation, EquipmentSlot slot) {
        try {
            Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                    UUID.class, String.class, double.class, AttributeModifier.Operation.class, EquipmentSlot.class);

            // A stable UUID per modifier id keeps items stackable and idempotent across restarts.
            UUID uuid = UUID.nameUUIDFromBytes(("kingdoms-specialties:" + id).getBytes());
            return constructor.newInstance(uuid, id, amount, operation, slot);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return null;
        }
    }

    private static void warn(String message) {
        SpecialtiesAddon.get().getLogger().warning(message);
    }
}
