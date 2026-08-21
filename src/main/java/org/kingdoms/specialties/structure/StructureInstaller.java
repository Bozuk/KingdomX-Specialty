package org.kingdoms.specialties.structure;

import org.kingdoms.constants.land.structures.StructureRegistry;
import org.kingdoms.specialties.SpecialtiesAddon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Drops {@code Structures/specialty-forge.yml} into the KingdomsX data folder so the structure
 * registry picks it up.
 * <p>
 * The timing matters. KingdomsX only extracts its own default structures when the
 * {@code Structures} folder does not exist yet, so creating that folder from our {@code onLoad()}
 * on a brand new server would silently rob the server of the nexus, the extractor and every other
 * built-in structure. So: write the file early when the folder is already there, and otherwise
 * wait until KingdomsX has extracted its defaults and rescan the folder afterwards.
 */
public final class StructureInstaller {
    private static final String RESOURCE = "Structures/" + SpecialtyForgeStructureType.NAME + ".yml";

    private static boolean installed;

    private StructureInstaller() {}

    /** @return {@code true} if the file could already be written, before KingdomsX read the folder. */
    public static boolean installEarly() {
        if (!Files.isDirectory(StructureRegistry.STRUCTURES_PATH)) return false;
        return install();
    }

    /**
     * Writes the file after KingdomsX extracted its defaults, then makes the registry read the
     * folder again. Re-running {@code init()} only rebuilds the styles, the registered types are
     * kept.
     */
    public static void installLate() {
        if (!install()) return;
        SpecialtiesAddon.get().getLogger().info("Registering the specialty forge structure...");
        StructureRegistry.get().init();
    }

    /** Where the structure file is expected to live. */
    public static Path targetPath() {
        return StructureRegistry.STRUCTURES_PATH.resolve(SpecialtyForgeStructureType.NAME + ".yml");
    }

    private static boolean install() {
        if (installed) return true;

        Path target = targetPath();
        if (Files.exists(target)) {
            installed = true;
            return true;
        }

        try (InputStream resource = SpecialtiesAddon.get().getResource(RESOURCE)) {
            if (resource == null) {
                SpecialtiesAddon.get().getLogger().severe("The addon jar has no " + RESOURCE + " resource.");
                return false;
            }

            Files.createDirectories(target.getParent());
            Files.copy(resource, target, StandardCopyOption.REPLACE_EXISTING);
            installed = true;
            SpecialtiesAddon.get().getLogger().info("Created " + target);
            return true;
        } catch (IOException ex) {
            SpecialtiesAddon.get().getLogger().severe("Could not write " + target + ": " + ex.getMessage());
            return false;
        }
    }
}
