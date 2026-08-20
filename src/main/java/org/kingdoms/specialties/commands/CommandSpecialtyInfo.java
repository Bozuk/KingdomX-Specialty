package org.kingdoms.specialties.commands;

import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.specialties.config.SpecialtiesLang;
import org.kingdoms.specialties.data.KingdomSpecialties;
import org.kingdoms.specialties.data.Specialty;
import org.kingdoms.specialties.managers.ExtractionSettings;

/** {@code /k specialty info} */
public final class CommandSpecialtyInfo extends KingdomsCommand {
    public CommandSpecialtyInfo(KingdomsParentCommand parent) {
        super("info", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.assertPlayer();
        if (context.assertHasKingdom()) return CommandResult.FAILED;

        Kingdom kingdom = context.getKingdom();
        Specialty specialty = KingdomSpecialties.getSpecialty(kingdom);
        if (specialty == null) return context.fail(SpecialtiesLang.COMMAND_SPECIALTY_INFO_NONE);

        context.var("specialty", specialty.getDisplayName())
                .var("specialty_resource", specialty.getResourceName())
                .var("extractors", KingdomSpecialties.getExtractors(kingdom).size())
                .var("fueled_extractors", KingdomSpecialties.countFueledExtractors(kingdom))
                .var("per_unit", ExtractionSettings.resourcePointsPerUnit())
                .var("remainder", KingdomSpecialties.getProductionRemainder(kingdom));

        context.sendMessage(SpecialtiesLang.COMMAND_SPECIALTY_INFO_DISPLAY);
        return CommandResult.SUCCESS;
    }
}
