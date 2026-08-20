package org.kingdoms.specialties.config;

import org.kingdoms.gui.GUIPathContainer;

public enum SpecialtiesGUI implements GUIPathContainer {
    SELECTION,
    ;

    private final String path;

    SpecialtiesGUI() {
        this.path = "specialties/" + GUIPathContainer.translateEnumPath(this);
    }

    @Override
    public String getGUIPath() {
        return path;
    }
}
