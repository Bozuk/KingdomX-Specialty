package org.kingdoms.specialties.managers;

import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.specialties.config.SpecialtiesConfig;
import org.kingdoms.utils.MathUtils;

/** Typed access to the {@code extraction} section of {@code specialties.yml}. */
public final class ExtractionSettings {
    private ExtractionSettings() {}

    /** Resource points an extractor must generate to yield one unit of the specialty resource. */
    public static long resourcePointsPerUnit() {
        return Math.max(1, SpecialtiesConfig.EXTRACTION_RESOURCE_POINTS_PER_UNIT.getManager().getLong());
    }

    /** Per-kingdom yield multiplier, evaluated with the kingdom's placeholders. */
    public static double multiplier(Kingdom kingdom) {
        String expression = SpecialtiesConfig.EXTRACTION_MULTIPLIER.getManager().getString();
        if (expression == null) return 1;

        MessagePlaceholderProvider context = new MessagePlaceholderProvider();
        context.withContext(kingdom);

        try {
            double value = MathUtils.eval(expression, context);
            return value <= 0 ? 0 : value;
        } catch (RuntimeException ex) {
            return 1;
        }
    }

    /** Maximum units handed out per collect, or 0 for no cap. */
    public static long maxPerCollect() {
        return Math.max(0, SpecialtiesConfig.EXTRACTION_MAX_PER_COLLECT.getManager().getLong());
    }

    public static boolean carryOverRemainder() {
        return SpecialtiesConfig.EXTRACTION_CARRY_OVER_REMAINDER.getManager().getBoolean();
    }

    public static boolean dropIfInventoryFull() {
        return SpecialtiesConfig.EXTRACTION_DROP_IF_INVENTORY_FULL.getManager().getBoolean();
    }
}
