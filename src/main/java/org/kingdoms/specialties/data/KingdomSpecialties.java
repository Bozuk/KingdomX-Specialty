package org.kingdoms.specialties.data;

import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.structures.Structure;
import org.kingdoms.constants.land.structures.objects.Extractor;
import org.kingdoms.constants.metadata.KingdomMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The public entry point of the addon: everything other plugins or classes need to read or change
 * the specialty of a kingdom.
 */
public final class KingdomSpecialties {
    private KingdomSpecialties() {}

    // ------------------------------------------------------------------ specialty

    /** @return the kingdom's specialty, or {@code null} if it hasn't picked one yet. */
    public static Specialty getSpecialty(Kingdom kingdom) {
        if (kingdom == null) return null;
        KingdomMetadata metadata = kingdom.getMetadata().get(SpecialtyMetaHandler.INSTANCE);
        if (metadata == null) return null;

        Object value = metadata.getValue();
        return value instanceof Specialty ? (Specialty) value : null;
    }

    public static boolean hasSpecialty(Kingdom kingdom) {
        return getSpecialty(kingdom) != null;
    }

    /**
     * Commits a kingdom to a specialty. Players can never call this twice: the command layer
     * refuses to change an existing specialty, only admins go through {@link #clearSpecialty}.
     */
    public static void setSpecialty(Kingdom kingdom, Specialty specialty) {
        kingdom.getMetadata().put(SpecialtyMetaHandler.INSTANCE, new SpecialtyMetaHandler.SpecialtyMeta(specialty));
    }

    /** Admin-only reset, lets the kingdom choose again. */
    public static void clearSpecialty(Kingdom kingdom) {
        kingdom.getMetadata().remove(SpecialtyMetaHandler.INSTANCE);
        setProductionRemainder(kingdom, 0);
    }

    // ----------------------------------------------------------------- extractors

    /**
     * The kingdom's extractors. These are the regular KingdomsX extractor structures - the addon
     * does not add a block of its own, it rides on the ones already placed.
     */
    public static List<Extractor> getExtractors(Kingdom kingdom) {
        if (kingdom == null) return Collections.emptyList();

        List<Extractor> extractors = new ArrayList<>();
        for (Structure structure : kingdom.getAllStructures()) {
            if (structure instanceof Extractor) extractors.add((Extractor) structure);
        }
        return extractors;
    }

    /** @return how many of the kingdom's extractors still hold fuel. */
    public static int countFueledExtractors(Kingdom kingdom) {
        int fueled = 0;
        for (Extractor extractor : getExtractors(kingdom)) {
            if (extractor.getFuel() > 0) fueled++;
        }
        return fueled;
    }

    // --------------------------------------------------------- production remainder

    /** Resource points carried over from previous collects, not yet worth a full unit. */
    public static long getProductionRemainder(Kingdom kingdom) {
        KingdomMetadata metadata = kingdom.getMetadata().get(ProductionRemainderMetaHandler.INSTANCE);
        if (metadata == null) return 0;

        Object value = metadata.getValue();
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }

    public static void setProductionRemainder(Kingdom kingdom, long remainder) {
        if (remainder <= 0) {
            kingdom.getMetadata().remove(ProductionRemainderMetaHandler.INSTANCE);
            return;
        }

        KingdomMetadata metadata = kingdom.getMetadata().get(ProductionRemainderMetaHandler.INSTANCE);
        if (metadata instanceof ProductionRemainderMetaHandler.RemainderMeta) {
            ((ProductionRemainderMetaHandler.RemainderMeta) metadata).setRemainder(remainder);
        } else {
            kingdom.getMetadata().put(ProductionRemainderMetaHandler.INSTANCE,
                    new ProductionRemainderMetaHandler.RemainderMeta(remainder));
        }
    }
}
