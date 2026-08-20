package org.kingdoms.specialties.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.gui.GUIAccessor;
import org.kingdoms.gui.InteractiveGUI;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.specialties.config.SpecialtiesGUI;
import org.kingdoms.specialties.data.Specialty;
import org.kingdoms.specialties.managers.SpecialtySelectionService;

import java.util.ArrayList;
import java.util.List;

/**
 * The menu used to pick a specialty. Opened while founding a kingdom - the creation waits for the
 * answer - and, for the kingdoms that have none, from {@code /k specialty choose}.
 * <p>
 * The icons, names and descriptions come straight from {@code specialties.yml} so the GUI file
 * only has to declare the slots.
 */
public final class SelectionGUI {
    private SelectionGUI() {}

    /** Opened while {@code /k create} is on hold: there is no kingdom yet. */
    public static void openForCreation(Player player) {
        open(player, null, null, true);
    }

    public static void open(Player player, KingdomPlayer kingdomPlayer, Kingdom kingdom) {
        open(player, kingdomPlayer, kingdom, false);
    }

    private static void open(Player player, KingdomPlayer kingdomPlayer, Kingdom kingdom, boolean creating) {
        MessagePlaceholderProvider context = new MessagePlaceholderProvider();
        context.withContext(player);
        if (kingdom != null) context.withContext(kingdom);

        InteractiveGUI gui = GUIAccessor.prepare(player, SpecialtiesGUI.SELECTION, context);
        if (gui == null) return;

        for (Specialty specialty : Specialty.values()) {
            gui.option(specialty.getConfigKey())
                    .editItem(item -> decorate(item, specialty))
                    .onNormalClicks(() -> {
                        if (creating) {
                            // Either the kingdom gets created, or a confirmation is awaited and the
                            // menu stays up so the player can click the same option again.
                            if (!SpecialtySelectionService.completeCreation(player, specialty)) {
                                if (SpecialtySelectionService.isCreating(player)) {
                                    open(player, null, null, true);
                                }
                            }
                            return;
                        }

                        if (SpecialtySelectionService.request(player, kingdomPlayer, kingdom, specialty)) {
                            player.closeInventory();
                        } else {
                            open(player, kingdomPlayer, kingdom, false);
                        }
                    })
                    .done();
        }

        gui.open();
    }

    private static ItemStack decorate(ItemStack item, Specialty specialty) {
        if (item == null) return null;
        item.setType(specialty.getIcon());

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(specialty.getDisplayName());

        List<String> lore = new ArrayList<>(specialty.getDescription());
        if (meta.hasLore() && meta.getLore() != null) {
            lore.add("");
            lore.addAll(meta.getLore());
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }
}
