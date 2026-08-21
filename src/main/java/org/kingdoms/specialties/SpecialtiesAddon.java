package org.kingdoms.specialties;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.kingdoms.addons.Addon;
import org.kingdoms.commands.admin.CommandAdmin;
import org.kingdoms.constants.metadata.KingdomMetadataHandler;
import org.kingdoms.constants.metadata.KingdomMetadataRegistry;
import org.kingdoms.gui.GUIConfig;
import org.kingdoms.locale.LanguageManager;
import org.kingdoms.constants.land.structures.StructureRegistry;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.specialties.commands.CommandSpecialty;
import org.kingdoms.specialties.commands.admin.CommandAdminSpecialty;
import org.kingdoms.specialties.config.SpecialtiesConfig;
import org.kingdoms.specialties.config.SpecialtiesLang;
import org.kingdoms.specialties.data.ProductionRemainderMetaHandler;
import org.kingdoms.specialties.data.Specialty;
import org.kingdoms.specialties.data.SpecialtyMetaHandler;
import org.kingdoms.specialties.managers.ConsumeListener;
import org.kingdoms.specialties.managers.ExtractionListener;
import org.kingdoms.specialties.locale.SpecialtiesLanguages;
import org.kingdoms.specialties.managers.KingdomCreationListener;
import org.kingdoms.specialties.structure.NexusMenuInstaller;
import org.kingdoms.specialties.structure.SpecialtyForgeStructureType;
import org.kingdoms.specialties.structure.StructureInstaller;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * KingdomsX addon: every kingdom commits to one of three specialties, which unlocks a unique
 * resource - produced by the kingdom's extractors - and a set of exclusive crafting recipes.
 */
public final class SpecialtiesAddon extends JavaPlugin implements Addon {
    private static SpecialtiesAddon instance;
    private static boolean loaded;

    private final Set<KingdomMetadataHandler> metadataHandlers = new HashSet<>();
    private boolean structureInstalled;

    public SpecialtiesAddon() {
        instance = this;
    }

    public static SpecialtiesAddon get() {
        return instance;
    }

    @Override
    public void onLoad() {
        if (!isKingdomsLoaded()) return;

        getLogger().info("Registering the kingdom metadata handlers...");
        metadataHandlers.addAll(Arrays.asList(
                SpecialtyMetaHandler.INSTANCE,
                ProductionRemainderMetaHandler.INSTANCE));
        for (KingdomMetadataHandler handler : metadataHandlers) {
            Kingdoms.get().getMetadataRegistry().register(handler);
        }

        LanguageManager.registerMessenger(SpecialtiesLang.class);
        SpecialtiesConfig.init();

        // Must happen before KingdomsX reads the Structures folder, which it does in its onEnable.
        StructureRegistry.get().registerType(SpecialtyForgeStructureType.INSTANCE);
        structureInstalled = StructureInstaller.installEarly();
    }

    @Override
    public void onEnable() {
        if (!isKingdomsEnabled()) {
            getLogger().severe("Kingdoms didn't load correctly. Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Before the commands: their names, aliases and descriptions come from the messages.
        SpecialtiesLanguages.reload();

        Specialty.reload();
        if (!structureInstalled) StructureInstaller.installLate();
        NexusMenuInstaller.verifyRegistered();

        // The structure shop lists what its own GUI file names, not what the registry holds.
        NexusMenuInstaller.install();

        Bukkit.getPluginManager().registerEvents(new ExtractionListener(), this);
        Bukkit.getPluginManager().registerEvents(new KingdomCreationListener(), this);
        Bukkit.getPluginManager().registerEvents(new ConsumeListener(), this);

        new CommandSpecialty();
        new CommandAdminSpecialty(CommandAdmin.getInstance());

        GUIConfig.loadInternalGUIs(this);

        registerAddon();
        loaded = true;
    }

    @Override
    public void onDisable() {
        if (!loaded) return;
        signalDisable();
        disableAddon();
    }

    @Override
    public void reloadAddon() {
        SpecialtiesConfig.getConfig().reload();
        SpecialtiesLanguages.reload();
        Specialty.reload();
        new CommandSpecialty();
    }

    @Override
    public void uninstall() {
        getLogger().info("Removing the specialties metadata...");
        KingdomMetadataRegistry.removeMetadata(Kingdoms.get().getDataCenter().getKingdomManager(), metadataHandlers);
    }

    @Override
    public String getAddonName() {
        return "specialties";
    }

    @Override
    public File getFile() {
        return super.getFile();
    }
}
