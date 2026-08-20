package org.kingdoms.specialties.items;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.specialties.data.Specialty;

import java.util.Map;

/** Hands the specialty resource over to a player, stack by stack. */
public final class ItemGiver {
    private ItemGiver() {}

    /**
     * @param dropOverflow whether the part that doesn't fit is dropped at the player's feet.
     * @return the amount that could neither be stored nor dropped.
     */
    public static long give(Player player, Specialty specialty, long amount, boolean dropOverflow) {
        ItemStack prototype = specialty.getResource();
        if (prototype == null) return amount;

        int maxStackSize = Math.max(1, prototype.getMaxStackSize());
        long remaining = amount;

        while (remaining > 0) {
            int stackSize = (int) Math.min(remaining, maxStackSize);
            ItemStack stack = specialty.getResource(stackSize);

            Map<Integer, ItemStack> rejected = player.getInventory().addItem(stack);
            if (rejected.isEmpty()) {
                remaining -= stackSize;
                continue;
            }

            // Whatever is left over either hits the ground or waits for the next collect.
            for (ItemStack left : rejected.values()) {
                remaining -= stackSize - left.getAmount();
                if (dropOverflow) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                    remaining -= left.getAmount();
                }
            }

            if (!dropOverflow) return remaining;
        }

        return Math.max(0, remaining);
    }
}
