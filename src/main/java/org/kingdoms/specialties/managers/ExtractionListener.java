package org.kingdoms.specialties.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.structures.objects.Extractor;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.events.items.structures.ExtractorCollectEvent;
import org.kingdoms.specialties.config.SpecialtiesLang;
import org.kingdoms.specialties.data.KingdomSpecialties;
import org.kingdoms.specialties.data.Specialty;
import org.kingdoms.specialties.items.ItemGiver;

/**
 * Turns the resource points a KingdomsX extractor produced into the unique resource of the
 * kingdom's specialty.
 * <p>
 * No extra block is introduced: the addon listens to the collection of the extractors that are
 * already part of KingdomsX. Since an extractor out of fuel generates no resource points at all,
 * the "needs fuel" rule comes for free.
 */
public final class ExtractionListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCollect(ExtractorCollectEvent event) {
        Extractor extractor = event.getItem();
        if (extractor == null) return;

        Land land = extractor.getLand();
        if (land == null) return;

        Kingdom kingdom = land.getKingdom();
        if (kingdom == null) return;

        Specialty specialty = KingdomSpecialties.getSpecialty(kingdom);
        if (specialty == null || specialty.getResource() == null) return;

        // Same value KingdomsX is about to hand out as resource points. Calling it here is safe:
        // it is time-based and the extractor is collected in the very next instruction.
        long points = extractor.getCollectedResourcePoints();

        double multiplier = ExtractionSettings.multiplier(kingdom);
        long effective = (long) Math.floor(points * multiplier);

        boolean carryOver = ExtractionSettings.carryOverRemainder();
        long pool = effective + (carryOver ? KingdomSpecialties.getProductionRemainder(kingdom) : 0);
        if (pool <= 0) return;

        long perUnit = ExtractionSettings.resourcePointsPerUnit();
        long units = pool / perUnit;
        long remainder = pool % perUnit;

        long cap = ExtractionSettings.maxPerCollect();
        if (cap > 0 && units > cap) {
            remainder += (units - cap) * perUnit;
            units = cap;
        }

        Player player = onlineCollector(event);
        if (player == null) {
            // Hopper or offline collection: keep everything for the next manual collect
            // instead of dropping resources into the void.
            if (carryOver) KingdomSpecialties.setProductionRemainder(kingdom, pool);
            return;
        }

        if (units > 0) {
            long undelivered = ItemGiver.give(player, specialty, units, ExtractionSettings.dropIfInventoryFull());
            if (undelivered > 0) {
                remainder += undelivered * perUnit;
                units -= undelivered;
                SpecialtiesLang.EXTRACTION_INVENTORY_FULL.sendError(player);
            }
        }

        KingdomSpecialties.setProductionRemainder(kingdom, carryOver ? remainder : 0);

        if (units > 0) {
            SpecialtiesLang.EXTRACTION_COLLECTED.sendMessage(player,
                    "collected", units,
                    "specialty_resource", specialty.getResourceName(),
                    "specialty", specialty.getDisplayName());
        } else if (carryOver) {
            SpecialtiesLang.EXTRACTION_NOT_ENOUGH_YET.sendMessage(player,
                    "remainder", remainder,
                    "per_unit", perUnit,
                    "specialty_resource", specialty.getResourceName());
        }
    }

    private Player onlineCollector(ExtractorCollectEvent event) {
        KingdomPlayer collector = event.getPlayer();
        if (collector == null) return null;

        Player player = collector.getPlayer();
        return player != null && player.isOnline() ? player : null;
    }
}
