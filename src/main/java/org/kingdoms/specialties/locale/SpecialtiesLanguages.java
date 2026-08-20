package org.kingdoms.specialties.locale;

import org.bukkit.configuration.file.YamlConfiguration;
import org.kingdoms.config.accessor.ConfigAccessor;
import org.kingdoms.locale.SupportedLanguage;
import org.kingdoms.locale.compiler.MessageCompiler;
import org.kingdoms.locale.provider.MessageProvider;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.specialties.config.SpecialtiesConfig;
import org.kingdoms.specialties.config.SpecialtiesLang;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

/**
 * One message file per language, under {@code plugins/Kingdoms/specialties/languages/}.
 * <p>
 * The layout mirrors the language files of KingdomsX itself - the same {@code command:} and
 * {@code specialties:} trees, the same keys - so translating the addon feels like translating the
 * main plugin. A file that omits a key simply falls back to the English text defined in
 * {@link SpecialtiesLang}.
 * <p>
 * A file is created for every language installed on the server. Translations shipped with the
 * addon are extracted as-is; the others are generated from the English texts, ready to translate.
 */
public final class SpecialtiesLanguages {
    /** Translations shipped inside the addon jar, under {@code languages/}. */
    private static final String[] BUNDLED = {"fr"};

    private SpecialtiesLanguages() {}

    public static Path folder() {
        return Kingdoms.getPath("specialties").resolve("languages");
    }

    /** Extracts and generates the missing language files. */
    public static void install() {
        Path folder = folder();
        try {
            Files.createDirectories(folder);
        } catch (IOException ex) {
            SpecialtiesAddon.get().getLogger().severe("Could not create " + folder + ": " + ex.getMessage());
            return;
        }

        // Hand written translations, useful even for a language the server hasn't installed:
        // they can be copied into another language file.
        for (String code : BUNDLED) extract(code, folder.resolve(code + ".yml"));

        for (SupportedLanguage language : installedLanguages()) {
            String code = fileCodeOf(language);
            Path target = folder.resolve(code + ".yml");
            if (Files.exists(target)) continue;
            if (!extract(code, target)) generateEnglish(target);
        }
    }

    /**
     * The translation file a language reads from.
     * <p>
     * Normally the language's own code, but {@code messages.language-files} can point a language
     * somewhere else. That matters here: KingdomsX has no French locale, so a French server runs
     * in English and maps {@code EN} to {@code fr}.
     */
    private static String fileCodeOf(SupportedLanguage language) {
        ConfigAccessor root = SpecialtiesConfig.accessor();
        if (root.isSet("messages") && root.gotoSection("messages").isSet("language-files")) {
            ConfigAccessor files = root.gotoSection("messages", "language-files");

            String mapped = files.getString(language.name());
            if (mapped == null) mapped = files.getString(language.getLowerCaseName());
            if (mapped != null && !mapped.isEmpty()) return mapped.trim().toLowerCase(Locale.ENGLISH);
        }
        return language.getLowerCaseName();
    }

    /**
     * Reads every language file and registers its messages for that language.
     * <p>
     * Every entry is registered for every installed language, falling back to the English text
     * when the file doesn't define it. That way a partial - or missing - translation can never
     * leave a message unresolved.
     */
    public static void apply() {
        Path folder = folder();

        for (SupportedLanguage language : installedLanguages()) {
            Path file = folder.resolve(fileCodeOf(language) + ".yml");
            YamlConfiguration yaml = Files.isRegularFile(file)
                    ? YamlConfiguration.loadConfiguration(file.toFile())
                    : new YamlConfiguration();

            int translated = 0;

            for (SpecialtiesLang message : SpecialtiesLang.values()) {
                String path = message.getLanguageEntry().asString();
                String text = yaml.getString(path);

                boolean fromFile = text != null && !text.isEmpty();
                if (!fromFile) text = message.getDefaultValue();

                try {
                    language.addMessage(message, new MessageProvider(MessageCompiler.compile(text)));
                    if (fromFile) translated++;
                } catch (RuntimeException ex) {
                    SpecialtiesAddon.get().getLogger().warning(
                            "Bad message '" + path + "' in " + file.getFileName() + ": " + ex);
                }
            }

            SpecialtiesAddon.get().getLogger().info(language.getLowerCaseName() + " -> "
                    + file.getFileName() + ": " + translated + '/' + SpecialtiesLang.values().length
                    + " translated messages.");
        }
    }

    public static void reload() {
        install();
        apply();
    }

    private static List<SupportedLanguage> installedLanguages() {
        return SupportedLanguage.getInstalled();
    }

    /** @return {@code true} if the addon jar ships a translation for this language. */
    private static boolean extract(String code, Path target) {
        if (Files.exists(target)) return true;

        try (InputStream resource = SpecialtiesAddon.get().getResource("languages/" + code + ".yml")) {
            if (resource == null) return false;

            Files.copy(resource, target, StandardCopyOption.REPLACE_EXISTING);
            SpecialtiesAddon.get().getLogger().info("Created " + target.getFileName());
            return true;
        } catch (IOException ex) {
            SpecialtiesAddon.get().getLogger().severe("Could not write " + target + ": " + ex.getMessage());
            return false;
        }
    }

    /**
     * Writes a complete file out of the English texts. Generating it instead of shipping a copy
     * keeps it in sync with the addon: a message added in a later version can never be missing.
     */
    private static void generateEnglish(Path target) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (SpecialtiesLang message : SpecialtiesLang.values()) {
            yaml.set(message.getLanguageEntry().asString(), message.getDefaultValue());
        }

        try {
            yaml.save(target.toFile());
            SpecialtiesAddon.get().getLogger().info("Created " + target.getFileName() + " from the English texts.");
        } catch (IOException ex) {
            SpecialtiesAddon.get().getLogger().severe("Could not write " + target + ": " + ex.getMessage());
        }
    }
}
