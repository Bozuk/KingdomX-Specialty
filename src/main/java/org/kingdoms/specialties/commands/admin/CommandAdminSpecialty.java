package org.kingdoms.specialties.commands.admin;

import org.bukkit.entity.Player;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.specialties.config.SpecialtiesLang;
import org.kingdoms.specialties.data.KingdomSpecialties;
import org.kingdoms.specialties.data.Specialty;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /k admin specialty <kingdom> <specialty|reset>}
 * <p>
 * The only way a kingdom's specialty can ever change once it has been picked.
 */
public final class CommandAdminSpecialty extends KingdomsCommand {
    private static final String RESET = "reset";

    public CommandAdminSpecialty(KingdomsParentCommand parent) {
        super("specialty", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.requireArgs(2);

        Kingdom kingdom = context.getKingdom(0);
        if (kingdom == null) return CommandResult.FAILED;

        String raw = context.arg(1);
        context.getMessageContext().withContext(kingdom);

        if (RESET.equalsIgnoreCase(raw)) {
            KingdomSpecialties.clearSpecialty(kingdom);
            context.sendMessage(SpecialtiesLang.COMMAND_ADMIN_SPECIALTY_RESET);
            notifyMembers(kingdom, SpecialtiesLang.COMMAND_ADMIN_SPECIALTY_RESET_NOTIFICATION, null);
            return CommandResult.SUCCESS;
        }

        Specialty specialty = Specialty.fromString(raw);
        if (specialty == null) {
            context.var("specialty", raw).var("specialties", Specialty.joinedNames());
            return context.fail(SpecialtiesLang.COMMAND_ADMIN_SPECIALTY_UNKNOWN);
        }

        KingdomSpecialties.setSpecialty(kingdom, specialty);

        context.var("specialty", specialty.getDisplayName());
        context.sendMessage(SpecialtiesLang.COMMAND_ADMIN_SPECIALTY_SET);
        notifyMembers(kingdom, SpecialtiesLang.COMMAND_ADMIN_SPECIALTY_CHANGED_NOTIFICATION, specialty);
        return CommandResult.SUCCESS;
    }

    private void notifyMembers(Kingdom kingdom, SpecialtiesLang message, Specialty specialty) {
        for (Player member : kingdom.getOnlineMembers()) {
            if (specialty == null) {
                message.sendMessage(member);
            } else {
                message.sendMessage(member, "specialty", specialty.getDisplayName());
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandTabContext context) {
        if (context.isAtArg(0)) return context.getKingdoms(0);
        if (context.isAtArg(1)) {
            List<String> options = new ArrayList<>(Specialty.configKeys());
            options.add(RESET);
            return context.suggest(1, options);
        }
        return context.emptyTab();
    }
}
