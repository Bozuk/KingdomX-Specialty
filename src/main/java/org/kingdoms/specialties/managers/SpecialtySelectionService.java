package org.kingdoms.specialties.managers;

import org.bukkit.entity.Player;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandUserError;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.constants.player.StandardKingdomPermission;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.specialties.SpecialtiesAddon;
import org.kingdoms.specialties.config.SpecialtiesConfig;
import org.kingdoms.specialties.config.SpecialtiesLang;
import org.kingdoms.specialties.data.KingdomSpecialties;
import org.kingdoms.specialties.data.Specialty;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The single place where a kingdom commits to a specialty.
 * <p>
 * The normal path is the creation one: {@code /k create} is held back until the founder picks a
 * specialty, and the kingdom is only created afterwards. The command path
 * ({@code /k specialty choose}) is the fallback for the kingdoms that somehow have none - those
 * that predate the addon, or those an admin reset.
 * <p>
 * Either way the choice is final: this service refuses to touch a kingdom that already has one.
 */
public final class SpecialtySelectionService {
    private static final long TIMEOUT = 60_000L;

    private static final Map<UUID, PendingChoice> PENDING_CONFIRMATIONS = new HashMap<>();
    private static final Map<UUID, PendingCreation> PENDING_CREATIONS = new HashMap<>();

    private SpecialtySelectionService() {}

    // ------------------------------------------------------------------ creation

    /** Remembers the {@code /k create} that was held back until a specialty is picked. */
    public static void beginCreation(Player player, KingdomsCommand command, String[] args) {
        PENDING_CREATIONS.put(player.getUniqueId(),
                new PendingCreation(command, args, System.currentTimeMillis() + TIMEOUT));
    }

    public static boolean isCreating(Player player) {
        return pendingCreation(player) != null;
    }

    private static PendingCreation pendingCreation(Player player) {
        PendingCreation pending = PENDING_CREATIONS.get(player.getUniqueId());
        if (pending == null) return null;

        if (pending.expiresAt < System.currentTimeMillis()) {
            PENDING_CREATIONS.remove(player.getUniqueId());
            return null;
        }
        return pending;
    }

    /**
     * Runs the held back creation with the chosen specialty.
     *
     * @return {@code true} if the kingdom was created and given its specialty.
     */
    public static boolean completeCreation(Player player, Specialty specialty) {
        PendingCreation pending = pendingCreation(player);
        if (pending == null) {
            SpecialtiesLang.CREATION_EXPIRED.sendError(player);
            return false;
        }

        if (needsConfirmation(player, specialty)) return false;

        PENDING_CONFIRMATIONS.remove(player.getUniqueId());
        PENDING_CREATIONS.remove(player.getUniqueId());
        player.closeInventory();

        // Calling the command directly instead of dispatching it again: the permission, cooldown
        // and world checks already ran before the pre-command event we cancelled, and this avoids
        // guessing the base command alias.
        CommandContext context = new CommandContext(Kingdoms.get(), pending.command, player, pending.args);
        try {
            pending.command.execute(context);
        } catch (CommandUserError error) {
            if (error.getError() != null) context.sendError(error.getError());
            return false;
        } catch (RuntimeException ex) {
            SpecialtiesAddon.get().getLogger().severe("The held back kingdom creation failed: " + ex);
            return false;
        }

        Kingdom kingdom = KingdomPlayer.getKingdomPlayer(player).getKingdom();
        if (kingdom == null) return false; // The creation was refused, nothing else to do.

        KingdomSpecialties.setSpecialty(kingdom, specialty);
        SpecialtiesLang.CREATION_CHOSEN.sendMessage(player,
                "specialty", specialty.getDisplayName(),
                "specialty_resource", specialty.getResourceName());
        return true;
    }

    // ------------------------------------------------------------------ fallback

    /**
     * Validates then applies the choice for a kingdom that already exists but has no specialty.
     *
     * @return {@code true} if the specialty was actually committed.
     */
    public static boolean request(Player player, KingdomPlayer kingdomPlayer, Kingdom kingdom, Specialty specialty) {
        if (!validate(player, kingdomPlayer, kingdom)) return false;
        if (needsConfirmation(player, specialty)) return false;

        PENDING_CONFIRMATIONS.remove(player.getUniqueId());
        apply(player, kingdom, specialty);
        return true;
    }

    /** Runs every requirement, sending the matching error message to the player. */
    public static boolean validate(Player player, KingdomPlayer kingdomPlayer, Kingdom kingdom) {
        Specialty current = KingdomSpecialties.getSpecialty(kingdom);
        if (current != null) {
            SpecialtiesLang.COMMAND_SPECIALTY_CHOOSE_ALREADY_CHOSEN.sendError(player,
                    "specialty", current.getDisplayName());
            return false;
        }

        if (SpecialtiesConfig.SELECTION_KING_ONLY.getManager().getBoolean()) {
            if (!player.getUniqueId().equals(kingdom.getKingId())) {
                SpecialtiesLang.COMMAND_SPECIALTY_CHOOSE_NOT_KING.sendError(player);
                return false;
            }
        } else if (!kingdomPlayer.hasPermission(StandardKingdomPermission.UPGRADE)) {
            SpecialtiesLang.COMMAND_SPECIALTY_CHOOSE_NO_PERMISSION.sendError(player);
            return false;
        }

        int requiredLevel = SpecialtiesConfig.SELECTION_REQUIRED_LEVEL.getManager().getInt();
        if (kingdom.getLevel() < requiredLevel) {
            SpecialtiesLang.COMMAND_SPECIALTY_CHOOSE_LOW_LEVEL.sendError(player, "required_level", requiredLevel);
            return false;
        }

        long resourcePoints = SpecialtiesConfig.SELECTION_COST_RESOURCE_POINTS.getManager().getLong();
        if (resourcePoints > 0 && !kingdom.getResourcePoints().has(resourcePoints)) {
            SpecialtiesLang.COMMAND_SPECIALTY_CHOOSE_NOT_ENOUGH_RESOURCE_POINTS.sendError(player, "cost", resourcePoints);
            return false;
        }

        double money = SpecialtiesConfig.SELECTION_COST_MONEY.getManager().getDouble();
        if (money > 0 && !kingdom.getBank().has(money)) {
            SpecialtiesLang.COMMAND_SPECIALTY_CHOOSE_NOT_ENOUGH_MONEY.sendError(player, "cost", money);
            return false;
        }

        return true;
    }

    private static void apply(Player player, Kingdom kingdom, Specialty specialty) {
        long resourcePoints = SpecialtiesConfig.SELECTION_COST_RESOURCE_POINTS.getManager().getLong();
        if (resourcePoints > 0) kingdom.getResourcePoints().add(-resourcePoints);

        double money = SpecialtiesConfig.SELECTION_COST_MONEY.getManager().getDouble();
        if (money > 0) kingdom.getBank().add(-money);

        KingdomSpecialties.setSpecialty(kingdom, specialty);

        SpecialtiesLang.COMMAND_SPECIALTY_CHOOSE_CHOSEN.sendMessage(player,
                "specialty", specialty.getDisplayName(),
                "specialty_resource", specialty.getResourceName());

        for (Player member : kingdom.getOnlineMembers()) {
            if (member.getUniqueId().equals(player.getUniqueId())) continue;
            SpecialtiesLang.COMMAND_SPECIALTY_CHOOSE_ANNOUNCEMENT.sendMessage(member,
                    "player", player.getName(),
                    "specialty", specialty.getDisplayName());
        }
    }

    // -------------------------------------------------------------- confirmation

    /** @return {@code true} when the player still has to click a second time. */
    private static boolean needsConfirmation(Player player, Specialty specialty) {
        if (!SpecialtiesConfig.SELECTION_REQUIRE_CONFIRMATION.getManager().getBoolean()) return false;
        if (isConfirming(player, specialty)) return false;

        PENDING_CONFIRMATIONS.put(player.getUniqueId(),
                new PendingChoice(specialty, System.currentTimeMillis() + TIMEOUT));
        SpecialtiesLang.COMMAND_SPECIALTY_CHOOSE_CONFIRM.sendMessage(player,
                "specialty", specialty.getDisplayName());
        return true;
    }

    private static boolean isConfirming(Player player, Specialty specialty) {
        PendingChoice pending = PENDING_CONFIRMATIONS.get(player.getUniqueId());
        if (pending == null) return false;

        if (pending.expiresAt < System.currentTimeMillis()) {
            PENDING_CONFIRMATIONS.remove(player.getUniqueId());
            return false;
        }
        return pending.specialty == specialty;
    }

    public static void forget(UUID playerId) {
        PENDING_CONFIRMATIONS.remove(playerId);
        PENDING_CREATIONS.remove(playerId);
    }

    private static final class PendingChoice {
        private final Specialty specialty;
        private final long expiresAt;

        private PendingChoice(Specialty specialty, long expiresAt) {
            this.specialty = specialty;
            this.expiresAt = expiresAt;
        }
    }

    private static final class PendingCreation {
        private final KingdomsCommand command;
        private final String[] args;
        private final long expiresAt;

        private PendingCreation(KingdomsCommand command, String[] args, long expiresAt) {
            this.command = command;
            this.args = args;
            this.expiresAt = expiresAt;
        }
    }
}
