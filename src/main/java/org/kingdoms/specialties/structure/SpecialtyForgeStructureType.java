package org.kingdoms.specialties.structure;

import org.kingdoms.constants.land.abstraction.gui.KingdomBuildingGUIContext;
import org.kingdoms.constants.land.structures.Structure;
import org.kingdoms.constants.land.structures.StructureType;
import org.kingdoms.gui.InteractiveGUI;
import org.kingdoms.specialties.gui.ForgeGUI;

/**
 * The kingdom structure where specialty items are made.
 * <p>
 * It behaves like any other KingdomsX structure - it shows up in {@code /k structures}, is bought
 * with resource points, built, upgraded and demolished the same way - and its menu lists the
 * recipes of the kingdom's specialty.
 */
public final class SpecialtyForgeStructureType extends StructureType {
    /** Must match the {@code type} field of {@code Structures/specialty-forge.yml}. */
    public static final String NAME = "specialty-forge";

    public static final SpecialtyForgeStructureType INSTANCE = new SpecialtyForgeStructureType();

    private SpecialtyForgeStructureType() {
        super(NAME);
    }

    @Override
    public InteractiveGUI open(KingdomBuildingGUIContext<Structure> context) {
        return ForgeGUI.open(context);
    }
}
