package org.kingdoms.specialties.managers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.general.others.CommandCreate;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.events.command.KingdomsPreCommandEvent;
import org.kingdoms.specialties.config.SpecialtiesLang;
import org.kingdoms.specialties.gui.SelectionGUI;

/**
 * Makes the specialty part of the kingdom creation instead of something picked afterwards.
 * <p>
 * {@code KingdomCreateEvent} fires once the kingdom already exists and isn't cancellable, so the
 * interception happens one step earlier: {@code /k create <name>} is held back, the selection menu
 * opens, and the creation only runs once a specialty has been picked.
 */
public final class KingdomCreationListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreCommand(KingdomsPreCommandEvent event) {
        CommandContext context = event.getContext();
        if (!(context.getCommand() instanceof CommandCreate)) return;

        CommandSender sender = context.getMessageReceiver();
        if (!(sender instanceof Player)) return;

        // No name given: let KingdomsX answer with its own usage message.
        String[] args = context.args;
        if (args == null || args.length == 0) return;

        Player player = (Player) sender;

        // Already in a kingdom, or any other reason the command would refuse: let it through so
        // the player gets the real error instead of a specialty menu.
        if (KingdomPlayer.getKingdomPlayer(player).hasKingdom()) return;

        event.setCancelled(true);

        SpecialtySelectionService.beginCreation(player, context.getCommand(), args);
        SpecialtiesLang.CREATION_CHOOSE_FIRST.sendMessage(player);
        SelectionGUI.openForCreation(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SpecialtySelectionService.forget(event.getPlayer().getUniqueId());
    }
}
