package org.kingdoms.specialties.managers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.kingdoms.libs.xseries.XPotion;
import org.kingdoms.specialties.items.SpecialtyItems;

import java.util.List;

/**
 * Hands out the effects of a specialty food item.
 * <p>
 * A golden apple's effects are hardcoded in the game: no amount of item meta makes it stronger.
 * The sublimated apple therefore carries its effects in its persistent data, and they are applied
 * here, before the game applies the vanilla ones. That order is what makes it work - the game only
 * replaces an effect with a stronger or longer one, so the vanilla Absorption I never overwrites
 * the one handed out here.
 */
public final class ConsumeListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        List<XPotion.Effect> effects = SpecialtyItems.getConsumeEffects(event.getItem());
        if (effects.isEmpty()) return;

        for (XPotion.Effect effect : effects) effect.apply(event.getPlayer());
    }
}
