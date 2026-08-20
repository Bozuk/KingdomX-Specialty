package org.kingdoms.specialties.commands;

import org.kingdoms.commands.KingdomsParentCommand;

/** {@code /k specialty} */
public final class CommandSpecialty extends KingdomsParentCommand {
    @SuppressWarnings("this-escape")
    public CommandSpecialty() {
        super("specialty");
        if (isDisabled()) return;

        new CommandSpecialtyInfo(this);
        new CommandSpecialtyChoose(this);
    }
}
