package org.kingdoms.specialties.structure;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.kingdoms.constants.land.structures.StructureRegistry;
import org.kingdoms.gui.GUIConfig;
import org.kingdoms.locale.SupportedLanguage;
import org.kingdoms.specialties.SpecialtiesAddon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adds the specialty forge to the structure shop of the nexus.
 * <p>
 * That menu is not built from the registered structures alone. KingdomsX walks the structure
 * registry and, for every style, looks for an option <em>of the same name</em> in
 * {@code guis/&lt;language&gt;/structures/nexus/structures.yml}; a style with no matching option is
 * skipped without a single word in the console. Registering the structure type and dropping
 * {@code Structures/specialty-forge.yml} is therefore not enough to make the forge buyable - the
 * menu entry has to exist too, and it lives in a file that belongs to KingdomsX.
 * <p>
 * So the entry is appended once, to the menu of every installed language, and never touched again:
 * from then on it is an ordinary line of the server's configuration, free to be moved, restyled or
 * deleted.
 */
public final class NexusMenuInstaller {
    /** The GUI KingdomsX opens for the structure shop. */
    private static final String GUI_PATH = "structures/nexus/structures";
    private static final String OPTION = SpecialtyForgeStructureType.NAME;
    private static final String FRAGMENT = "menu/nexus-structures-option.yml";

    /** Menus are nine slots wide, and {@code posx} / {@code posy} start at one. */
    private static final int ROW_WIDTH = 9;

    private NexusMenuInstaller() {}

    public static void install() {
        String fragment = readFragment();
        if (fragment == null) return;

        for (SupportedLanguage language : SupportedLanguage.getInstalled()) {
            Path file = language.getGUIFolder()
                    .resolve("structures").resolve("nexus").resolve("structures.yml");
            if (!Files.isRegularFile(file)) continue;

            try {
                if (!addOption(file, fragment)) continue;
            } catch (IOException | RuntimeException ex) {
                SpecialtiesAddon.get().getLogger().severe(
                        "Could not add the specialty forge to " + file + ": " + ex);
                continue;
            }

            SpecialtiesAddon.get().getLogger().info(
                    "Added the specialty forge to the structure shop of " + language.getLowerCaseName() + '.');

            // KingdomsX read that menu during its own onEnable, before this addon was enabled.
            try {
                GUIConfig.reload(GUI_PATH, language);
            } catch (RuntimeException ex) {
                SpecialtiesAddon.get().getLogger().warning(
                        "The structure shop of " + language.getLowerCaseName()
                                + " could not be reloaded, it will pick the forge up on the next restart: " + ex);
            }
        }
    }

    /**
     * Warns when the structure never made it into the registry. Without this the only symptom is
     * an absence, and an absence looks exactly like a missing menu entry.
     */
    public static void verifyRegistered() {
        if (StructureRegistry.get().getStyle(OPTION) != null) return;

        SpecialtiesAddon.get().getLogger().severe(
                "The '" + OPTION + "' structure is not registered. Check that "
                        + StructureInstaller.targetPath() + " exists and that KingdomsX read it without an error;"
                        + " until then the specialty forge cannot be bought or built.");
    }

    // ------------------------------------------------------------------- writing

    /** @return {@code true} if the option was appended, {@code false} if it was already there. */
    private static boolean addOption(Path file, String fragment) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        YamlConfiguration yaml = parse(file);
        if (yaml != null && yaml.isConfigurationSection("options." + OPTION)) return false;
        if (yaml == null && containsOptionKey(lines)) return false;

        int insertAt = endOfOptions(lines);
        if (insertAt < 0) {
            SpecialtiesAddon.get().getLogger().severe(
                    file + " has no 'options' section, the specialty forge cannot be added to the shop.");
            return false;
        }

        List<String> updated = new ArrayList<>(lines.subList(0, insertAt));
        updated.add("");
        for (String line : fragment.replace("%slot%", String.valueOf(freeSlot(yaml))).split("\n", -1)) {
            updated.add(line.isEmpty() ? "" : "  " + line);
        }
        updated.addAll(lines.subList(insertAt, lines.size()));

        Files.write(file, updated, StandardCharsets.UTF_8);
        return true;
    }

    private static String readFragment() {
        try (InputStream resource = SpecialtiesAddon.get().getResource(FRAGMENT)) {
            if (resource == null) {
                SpecialtiesAddon.get().getLogger().severe("The addon jar has no " + FRAGMENT + " resource.");
                return null;
            }

            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                content.append(line).append('\n');
            }
            // The trailing newline would insert a blank line of its own.
            if (content.length() != 0) content.setLength(content.length() - 1);
            return content.toString();
        } catch (IOException ex) {
            SpecialtiesAddon.get().getLogger().severe("Could not read " + FRAGMENT + ": " + ex);
            return null;
        }
    }

    /**
     * The line the new option goes before: right after the last line that still belongs to the
     * {@code options} block, so anything the file holds after it stays where it is.
     */
    private static int endOfOptions(List<String> lines) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("options:")) {
                start = i;
                break;
            }
        }
        if (start < 0) return -1;

        int end = start + 1;
        for (int i = start + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) continue;
            // A line back at column zero closes the block.
            if (!Character.isWhitespace(line.charAt(0))) break;
            end = i + 1;
        }
        return end;
    }

    // ------------------------------------------------------------------- reading

    private static YamlConfiguration parse(Path file) {
        try {
            return YamlConfiguration.loadConfiguration(file.toFile());
        } catch (RuntimeException | LinkageError ex) {
            SpecialtiesAddon.get().getLogger().warning(
                    "Could not read " + file + " to pick a free slot: " + ex);
            return null;
        }
    }

    private static boolean containsOptionKey(List<String> lines) {
        for (String line : lines) {
            if (line.trim().equals(OPTION + ':')) return true;
        }
        return false;
    }

    /**
     * A slot no other option uses. The rows in between are preferred: that is where the structures
     * sit, the first and the last row being decoration and the back button.
     */
    private static int freeSlot(YamlConfiguration yaml) {
        if (yaml == null) return ROW_WIDTH + 1;

        int rows = Math.max(1, yaml.getInt("rows", 3));
        Set<Integer> used = usedSlots(yaml);

        for (int slot = ROW_WIDTH; slot < (rows - 1) * ROW_WIDTH; slot++) {
            if (!used.contains(slot)) return slot;
        }
        for (int slot = 0; slot < rows * ROW_WIDTH; slot++) {
            if (!used.contains(slot)) return slot;
        }
        return ROW_WIDTH + 1;
    }

    private static Set<Integer> usedSlots(YamlConfiguration yaml) {
        Set<Integer> used = new HashSet<>();

        ConfigurationSection options = yaml.getConfigurationSection("options");
        if (options == null) return used;

        for (String key : options.getKeys(false)) {
            ConfigurationSection option = options.getConfigurationSection(key);
            if (option == null) continue;

            if (option.isInt("slot")) used.add(option.getInt("slot"));
            used.addAll(option.getIntegerList("slots"));

            if (option.isInt("posx") && option.isInt("posy")) {
                used.add((option.getInt("posy") - 1) * ROW_WIDTH + (option.getInt("posx") - 1));
            }
        }
        return used;
    }
}
