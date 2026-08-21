package org.kingdoms.specialties.managers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.kingdoms.specialties.config.SpecialtiesLang;
import org.kingdoms.specialties.data.Specialty;
import org.kingdoms.specialties.data.SpecialtyRecipe;
import org.kingdoms.specialties.items.PotionSupport;
import org.kingdoms.specialties.items.SpecialtyItems;

import java.util.Map;

/**
 * The crafting logic behind the specialty forge: what a player can make, what it costs them, and
 * handing the result over.
 */
public final class ForgeService {
    private ForgeService() {}

    /** @return how many times the recipe could be run with what the player carries. */
    public static int affordableTimes(Player player, SpecialtyRecipe recipe) {
        int times = Integer.MAX_VALUE;
        for (SpecialtyRecipe.Ingredient ingredient : recipe.getIngredients()) {
            times = Math.min(times, count(player, ingredient) / ingredient.getAmount());
            if (times == 0) return 0;
        }
        return times == Integer.MAX_VALUE ? 0 : times;
    }

    public static boolean hasIngredients(Player player, SpecialtyRecipe recipe) {
        return affordableTimes(player, recipe) > 0;
    }

    /** Counts the matching items in the player's inventory. */
    public static int count(Player player, SpecialtyRecipe.Ingredient ingredient) {
        int total = 0;
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (matches(item, ingredient)) total += item.getAmount();
        }
        return total;
    }

    /** @return the first matching item of the inventory, or {@code null}. */
    private static ItemStack find(Player player, SpecialtyRecipe.Ingredient ingredient) {
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (matches(item, ingredient)) return item;
        }
        return null;
    }

    private static boolean matches(ItemStack item, SpecialtyRecipe.Ingredient ingredient) {
        if (item == null || item.getType() == Material.AIR) return false;

        Specialty tagged = SpecialtyItems.getResourceSpecialty(item);
        if (ingredient.isSpecialtyResource()) return tagged == ingredient.getResourceOf();

        // A plain material ingredient must never eat a specialty item of the same material: neither
        // a resource, nor something the forge produced itself.
        if (tagged != null || SpecialtyItems.isForged(item)) return false;
        if (item.getType() != ingredient.getMaterial()) return false;

        if (ingredient.getEffectType() == null) return true;

        // A potion ingredient is picked by what it does, not by its material: every potion of the
        // game shares POTION. An amplifier, when set, has to match exactly.
        PotionEffect effect = PotionSupport.findEffect(item, ingredient.getEffectType());
        if (effect == null) return false;
        return ingredient.getEffectAmplifier() == null
                || effect.getAmplifier() == ingredient.getEffectAmplifier();
    }

    private static void consume(Player player, SpecialtyRecipe recipe) {
        Inventory inventory = player.getInventory();

        for (SpecialtyRecipe.Ingredient ingredient : recipe.getIngredients()) {
            int remaining = ingredient.getAmount();

            for (int slot = 0; slot < inventory.getSize() && remaining > 0; slot++) {
                ItemStack item = inventory.getItem(slot);
                if (!matches(item, ingredient)) continue;

                int taken = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - taken);
                if (item.getAmount() <= 0) inventory.setItem(slot, null);
                remaining -= taken;
            }
        }
    }

    /**
     * Runs a recipe.
     *
     * @return {@code true} if the item was produced. Errors are reported to the player.
     */
    public static boolean craft(Player player, SpecialtyRecipe recipe) {
        if (!hasIngredients(player, recipe)) {
            SpecialtiesLang.FORGE_MISSING_INGREDIENTS.sendError(player);
            return false;
        }

        // A transmutation copies the duration and the level of the potion it consumes, so the
        // bottle has to be read before the inventory is emptied of it.
        ItemStack source = null;
        if (recipe.getType() == SpecialtyRecipe.Type.TRANSMUTE) {
            ItemStack found = find(player, recipe.getSource());
            if (found == null) {
                SpecialtiesLang.FORGE_MISSING_INGREDIENTS.sendError(player);
                return false;
            }
            source = found.clone();
        }

        ItemStack result = recipe.buildResult(source);
        if (result == null) {
            SpecialtiesLang.FORGE_MISSING_INGREDIENTS.sendError(player);
            return false;
        }

        consume(player, recipe);
        give(player, result);

        SpecialtiesLang.FORGE_CRAFTED.sendMessage(player, "recipe", displayNameOf(recipe));
        return true;
    }

    /** Adds the item to the inventory, dropping whatever doesn't fit at the player's feet. */
    public static void give(Player player, ItemStack item) {
        Map<Integer, ItemStack> rejected = player.getInventory().addItem(item);
        for (ItemStack left : rejected.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }

    public static String displayNameOf(SpecialtyRecipe recipe) {
        if (recipe.getDisplayName() != null) return recipe.getDisplayName();

        ItemStack result = recipe.getResult();
        if (result != null && result.hasItemMeta() && result.getItemMeta() != null
                && !result.getItemMeta().getDisplayName().isEmpty()) {
            return result.getItemMeta().getDisplayName();
        }
        return recipe.getId();
    }
}
