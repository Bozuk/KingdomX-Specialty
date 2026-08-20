package org.kingdoms.specialties.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.abstraction.gui.KingdomBuildingGUIContext;
import org.kingdoms.constants.land.structures.Structure;
import org.kingdoms.gui.GUIAccessor;
import org.kingdoms.gui.InteractiveGUI;
import org.kingdoms.gui.ReusableOptionHandler;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.specialties.config.SpecialtiesLang;
import org.kingdoms.specialties.data.KingdomSpecialties;
import org.kingdoms.specialties.data.Specialty;
import org.kingdoms.specialties.data.SpecialtyRecipe;
import org.kingdoms.specialties.managers.ForgeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The menu of the specialty forge: one clickable entry per recipe of the kingdom's specialty. */
public final class ForgeGUI {
    private ForgeGUI() {}

    public static InteractiveGUI open(KingdomBuildingGUIContext<Structure> context) {
        Structure structure = context.getEvent().getKingdomItem();
        Player player = context.getEvent().getPlayer().getPlayer();
        if (player == null) return null;

        Land land = structure.getLand();
        Kingdom kingdom = land == null ? null : land.getKingdom();
        Specialty specialty = KingdomSpecialties.getSpecialty(kingdom);

        MessagePlaceholderProvider messages = new MessagePlaceholderProvider();
        messages.withContext(player);
        if (kingdom != null) messages.withContext(kingdom);
        structure.addMessageContextEdits(messages);
        messages.raw("specialty", specialty == null ? "-" : specialty.getDisplayName());
        messages.raw("specialty_resource", specialty == null ? "-" : specialty.getResourceName());
        messages.raw("recipes", specialty == null ? 0 : specialty.getRecipes().size());

        InteractiveGUI gui = GUIAccessor.prepare(player, "structures/" + structure.getStyle().getName(), messages);
        if (gui == null) return null;

        if (specialty != null) {
            ReusableOptionHandler entries = gui.getReusableOption("recipes");
            if (entries != null) {
                for (SpecialtyRecipe recipe : specialty.getRecipes()) {
                    if (!entries.hasNext()) break;

                    entries.editItem(item -> describe(item, player, recipe));
                    entries.onNormalClicks(() -> {
                        boolean done = recipe.getType() == SpecialtyRecipe.Type.ENCHANT
                                ? ForgeService.enchant(player, recipe)
                                : ForgeService.craft(player, recipe);
                        if (done) open(context.refresh(gui));
                    });
                    entries.done();
                }
            }
        }

        return context.finalizeGUI(gui, context.isRefreshing());
    }

    /** Rewrites the configured slot item into the recipe it stands for. */
    private static ItemStack describe(ItemStack item, Player player, SpecialtyRecipe recipe) {
        if (item == null) return null;
        item.setType(recipe.getIcon());

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ForgeService.displayNameOf(recipe));

        boolean enchanting = recipe.getType() == SpecialtyRecipe.Type.ENCHANT;
        List<String> lore = new ArrayList<>(recipe.getDescription());

        if (enchanting) {
            lore.add(SpecialtiesLang.FORGE_ENTRY_ENCHANTMENT.parse(player,
                    "enchantment", prettify(recipe.getEnchantment().getKey().getKey()),
                    "level", recipe.getLevel()));
            lore.add(SpecialtiesLang.FORGE_ENTRY_BEYOND_VANILLA.parse(player));
        }

        lore.add("");
        lore.add(SpecialtiesLang.FORGE_ENTRY_INGREDIENTS.parse(player));

        boolean affordable = true;
        for (SpecialtyRecipe.Ingredient ingredient : recipe.getIngredients()) {
            int owned = ForgeService.count(player, ingredient);
            boolean enough = owned >= ingredient.getAmount();
            affordable &= enough;

            SpecialtiesLang line = enough
                    ? SpecialtiesLang.FORGE_ENTRY_INGREDIENT_OK
                    : SpecialtiesLang.FORGE_ENTRY_INGREDIENT_MISSING;
            lore.add(line.parse(player,
                    "owned", owned,
                    "required", ingredient.getAmount(),
                    "ingredient", ingredient.getDisplayName()));
        }

        lore.add("");
        if (enchanting) lore.add(SpecialtiesLang.FORGE_ENTRY_HOLD_ITEM.parse(player));
        lore.add(affordable
                ? SpecialtiesLang.FORGE_ENTRY_CLICK_TO_FORGE.parse(player)
                : SpecialtiesLang.FORGE_ENTRY_MISSING.parse(player));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** {@code fire_aspect} -> {@code Fire Aspect}. */
    private static String prettify(String key) {
        StringBuilder builder = new StringBuilder(key.length());
        for (String word : key.split("[_.]")) {
            if (word.isEmpty()) continue;
            if (builder.length() != 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ENGLISH));
        }
        return builder.toString();
    }
}
