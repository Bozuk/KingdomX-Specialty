package org.kingdoms.specialties.commands;

import org.bukkit.entity.Player;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.specialties.config.SpecialtiesLang;
import org.kingdoms.specialties.data.Specialty;
import org.kingdoms.specialties.gui.SelectionGUI;
import org.kingdoms.specialties.managers.SpecialtySelectionService;

import java.util.List;

/**
 * {@code /k specialty choose [specialty]}
 * <p>
 * Without an argument the selection menu opens. The choice is final, the command refuses to
 * overwrite an existing specialty.
 */
public final class CommandSpecialtyChoose extends KingdomsCommand {
    public CommandSpecialtyChoose(KingdomsParentCommand parent) {
        super("choose", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.assertPlayer();
        if (context.assertHasKingdom()) return CommandResult.FAILED;

        Player player = context.senderAsPlayer();
        KingdomPlayer kingdomPlayer = context.getKingdomPlayer();
        Kingdom kingdom = kingdomPlayer.getKingdom();

        if (!context.hasArgs(1)) {
            // The menu itself runs the same validation once an option is clicked.
            if (!SpecialtySelectionService.validate(player, kingdomPlayer, kingdom)) return CommandResult.FAILED;
            SelectionGUI.open(player, kingdomPlayer, kingdom);
            return CommandResult.SUCCESS;
        }

        String raw = context.arg(0);
        Specialty specialty = Specialty.fromString(raw);
        if (specialty == null) {
            context.var("specialty", raw);
            return context.fail(SpecialtiesLang.COMMAND_SPECIALTY_CHOOSE_UNKNOWN);
        }

        return SpecialtySelectionService.request(player, kingdomPlayer, kingdom, specialty)
                ? CommandResult.SUCCESS
                : CommandResult.PARTIAL;
    }

    @Override
    public List<String> tabComplete(CommandTabContext context) {
        return context.isAtArg(0) ? context.suggest(0, Specialty.configKeys()) : context.emptyTab();
    }
}
