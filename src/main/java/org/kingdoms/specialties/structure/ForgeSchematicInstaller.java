package org.kingdoms.specialties.structure;

import org.bukkit.Bukkit;
import org.kingdoms.constants.land.structures.StructureRegistry;
import org.kingdoms.constants.land.structures.StructureStyle;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.utils.config.ConfigSection;
import org.kingdoms.utils.config.adapters.YamlContainer;
import org.kingdoms.utils.config.adapters.YamlResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Gives the specialty forge a schematic, for the servers that build their structures out of
 * WorldEdit schematics.
 * <p>
 * That is the job of the official EngineHub addon, and this class only follows its conventions:
 * <ul>
 *     <li>it reads every file of {@code Kingdoms/schematics}, a schematic being named after its
 *     path relative to that folder, without the extension - {@code structures/nexus/1};</li>
 *     <li>it ships its own under {@code schematics/structures/<structure>/<level>} and copies the
 *     missing ones over, never overwriting a file that is already there;</li>
 *     <li>a building level picks its schematic with a {@code schematic} option. Its setup writes
 *     {@code '<type>/<name>/<level>'} into the shared building declaration, which for the forge
 *     resolves to the very path the file below is shipped under.</li>
 * </ul>
 * So the schematic is dropped next to the EngineHub ones, and the forge is only pointed at it when
 * none of its levels has one: a schematic set by the admin, in the forge's file or in a
 * declaration, is left exactly as it is. Without EngineHub nothing is written at all, the option
 * would mean nothing there.
 */
public final class ForgeSchematicInstaller {
    /** The official addon that builds structures out of WorldEdit schematics. */
    private static final String ENGINEHUB = "Kingdoms-Addon-EngineHub";

    /** The folder EngineHub reads, under the KingdomsX one; the jar mirrors its layout. */
    private static final String FOLDER = "schematics";
    private static final String RESOURCE =
            FOLDER + "/structures/" + SpecialtyForgeStructureType.NAME + "/1.schem";

    /** The building option EngineHub reads the schematic name from. */
    private static final String OPTION = "schematic";

    private ForgeSchematicInstaller() {}

    // ------------------------------------------------------------------- the file

    /**
     * Copies the schematic into {@code Kingdoms/schematics}. Called from {@code onLoad()}, so the
     * file is there before EngineHub loads that folder in its {@code onEnable()}.
     */
    public static void extract() {
        if (!isEngineHubInstalled()) return;

        Path target = folder().resolve(RESOURCE.substring(FOLDER.length() + 1));
        // EngineHub renames schematics to the extension WorldEdit prefers (.schematic, .schem...),
        // so the file is looked up by name: whatever its extension, it is already installed.
        if (hasSibling(target)) return;

        try (InputStream resource = SpecialtiesAddon.get().getResource(RESOURCE)) {
            if (resource == null) {
                SpecialtiesAddon.get().getLogger().severe("The addon jar has no " + RESOURCE + " resource.");
                return;
            }
            Files.createDirectories(target.getParent());
            Files.copy(resource, target);
            SpecialtiesAddon.get().getLogger().info("Created " + target);
        } catch (IOException ex) {
            SpecialtiesAddon.get().getLogger().severe("Could not write " + target + ": " + ex);
        }
    }

    // ------------------------------------------------------------------- the structure

    /**
     * Points every forge level that has no schematic at the one shipped with the addon. Called once
     * the structures are registered, since the levels only exist once the declarations the forge
     * imports have been merged into it.
     */
    public static void assign() {
        if (!isEngineHubInstalled()) return;

        String shipped = schematicName(RESOURCE.substring(FOLDER.length() + 1));
        boolean changed = false;

        // Every style of the forge type: the file may have been renamed, or copied into variants.
        for (StructureStyle style : StructureRegistry.get().getStyles().values()) {
            if (style.getType() != SpecialtyForgeStructureType.INSTANCE) continue;

            ConfigSection building = style.getConfig().getConfig().getSection("building");
            List<String> missing = levelsWithoutSchematic(building);
            if (missing.isEmpty()) {
                warnAboutUnknownSchematics(style, building);
                continue;
            }

            File file = style.getConfig().getFile();
            try {
                setSchematic(file, missing, shipped);
            } catch (RuntimeException ex) {
                SpecialtiesAddon.get().getLogger().severe(
                        "Could not give the '" + style.getName() + "' structure a schematic in " + file + ": " + ex);
                continue;
            }

            changed = true;
            SpecialtiesAddon.get().getLogger().info(
                    "The '" + style.getName() + "' structure had no schematic, it now uses '" + shipped
                            + "' (levels " + missing + ", in " + file + ").");
        }

        // The styles hold the configuration they were built from: rebuild them from the files.
        if (changed) StructureRegistry.get().init();
    }

    /** The levels of the {@code building} section that name no schematic, imports included. */
    private static List<String> levelsWithoutSchematic(ConfigSection building) {
        if (building == null) return Collections.singletonList("1");

        List<String> missing = new ArrayList<>();
        for (String level : building.getKeys()) {
            ConfigSection settings = building.getSection(level);
            if (settings != null && isBlank(settings.getString(OPTION))) missing.add(level);
        }
        return missing;
    }

    /**
     * Writes the option into the raw file, which leaves the imports where they are: the levels
     * written here are merged over the ones of the building declaration, like the setup of
     * EngineHub does for the built-in structures.
     */
    private static void setSchematic(File file, List<String> levels, String schematic) {
        YamlContainer raw = new YamlResource(file).setResolveAliases(false).load();
        ConfigSection building = raw.getConfig().createSection("building");
        for (String level : levels) {
            building.createSection(level).set(OPTION, schematic);
        }
        raw.saveConfig();
    }

    /**
     * A level whose schematic does not exist is silently built as a single block, which looks like
     * EngineHub not working at all. The configuration is not touched, only reported.
     */
    private static void warnAboutUnknownSchematics(StructureStyle style, ConfigSection building) {
        if (building == null) return;

        for (String level : building.getKeys()) {
            ConfigSection settings = building.getSection(level);
            if (settings == null) continue;

            String schematic = settings.getString(OPTION);
            if (isBlank(schematic) || schematicExists(schematic)) continue;

            SpecialtiesAddon.get().getLogger().warning(
                    "Level " + level + " of the '" + style.getName() + "' structure uses the schematic '" + schematic
                            + "', which is not in " + folder() + ": that level will be built as a single block.");
        }
    }

    // ------------------------------------------------------------------- the folder

    private static boolean isEngineHubInstalled() {
        return Bukkit.getPluginManager().getPlugin(ENGINEHUB) != null;
    }

    private static Path folder() {
        return Kingdoms.getPath(FOLDER);
    }

    /** Whether a file of the same name, whatever its extension, sits next to {@code file}. */
    private static boolean hasSibling(Path file) {
        Path parent = file.getParent();
        if (!Files.isDirectory(parent)) return false;

        String name = withoutExtension(file.getFileName().toString());
        try (DirectoryStream<Path> files = Files.newDirectoryStream(parent)) {
            for (Path sibling : files) {
                if (Files.isRegularFile(sibling) && withoutExtension(sibling.getFileName().toString()).equals(name)) {
                    return true;
                }
            }
        } catch (IOException ex) {
            SpecialtiesAddon.get().getLogger().warning("Could not list " + parent + ": " + ex);
            // Better a missing schematic than one written over the admin's.
            return true;
        }
        return false;
    }

    private static boolean schematicExists(String schematic) {
        Path folder = folder();
        if (!Files.isDirectory(folder)) return false;

        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(Files::isRegularFile)
                    .anyMatch(file -> schematicName(folder.relativize(file).toString()).equals(schematic));
        } catch (IOException | RuntimeException ex) {
            // Unknown, so assume it is there rather than warn about nothing.
            return true;
        }
    }

    /** The name EngineHub gives the file at that path, relative to its folder. */
    private static String schematicName(String relativePath) {
        return withoutExtension(relativePath).replace('\\', '/').replace(' ', '-');
    }

    private static String withoutExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
