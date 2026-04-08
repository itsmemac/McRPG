package us.eunoians.mcrpg.command.statistic;

import com.diamonddagger590.mccore.registry.RegistryKey;
import com.diamonddagger590.mccore.statistic.Statistic;
import com.diamonddagger590.mccore.statistic.StatisticRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.incendo.cloud.permission.Permission;
import org.incendo.cloud.processors.confirmation.ConfirmationManager;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.command.parser.StatisticParser;
import us.eunoians.mcrpg.configuration.file.localization.LocalizationKey;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.localization.McRPGLocalizationManager;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;

import java.util.Map;
import java.util.Optional;

/**
 * Command: {@code /mcrpg statistic reset <player> [statistic]}
 * <p>
 * Resets a specific statistic (or all statistics) for a player. Requires {@code /mcrpg confirm}.
 */
public class StatisticResetCommand extends StatisticCommandBase {

    private static final Permission RESET_PERMISSION = Permission.of("mcrpg.statistic.reset");

    /** Registers the {@code /mcrpg statistic reset} command. */
    public static void registerCommand() {
        McRPG mcRPG = McRPG.getInstance();
        CommandManager<CommandSourceStack> commandManager = mcRPG.registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.COMMAND).getCommandManager();
        McRPGLocalizationManager localizationManager = mcRPG.registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.LOCALIZATION);

        // Reset a specific statistic
        commandManager.command(commandManager.commandBuilder("mcrpg")
                .literal("statistic")
                .literal("reset")
                .required("player", PlayerParser.playerParser(),
                        RichDescription.richDescription(
                                localizationManager.getLocalizedMessageAsComponent(LocalizationKey.COMMAND_DESCRIPTION_STATISTIC_RESET_PLAYER)))
                .required("statistic", StatisticParser.statisticParser(),
                        RichDescription.richDescription(
                                localizationManager.getLocalizedMessageAsComponent(LocalizationKey.COMMAND_DESCRIPTION_STATISTIC_RESET_STATISTIC)))
                .permission(Permission.anyOf(ROOT_PERMISSION, ADMIN_BASE_PERMISSION, STATISTIC_COMMAND_ROOT_PERMISSION, RESET_PERMISSION))
                .meta(ConfirmationManager.META_CONFIRMATION_REQUIRED, true)
                .handler(commandContext -> {
                    Player target = commandContext.get(CloudKey.of("player", Player.class));
                    Statistic statistic = commandContext.get(CloudKey.of("statistic", Statistic.class));
                    Audience sender = commandContext.sender().getSender();
                    handleSingleReset(mcRPG, localizationManager, sender, target, statistic);
                }));

        // Reset all statistics
        commandManager.command(commandManager.commandBuilder("mcrpg")
                .literal("statistic")
                .literal("reset")
                .required("player", PlayerParser.playerParser(),
                        RichDescription.richDescription(
                                localizationManager.getLocalizedMessageAsComponent(LocalizationKey.COMMAND_DESCRIPTION_STATISTIC_RESET_PLAYER)))
                .permission(Permission.anyOf(ROOT_PERMISSION, ADMIN_BASE_PERMISSION, STATISTIC_COMMAND_ROOT_PERMISSION, RESET_PERMISSION))
                .meta(ConfirmationManager.META_CONFIRMATION_REQUIRED, true)
                .handler(commandContext -> {
                    Player target = commandContext.get(CloudKey.of("player", Player.class));
                    Audience sender = commandContext.sender().getSender();
                    handleResetAll(mcRPG, localizationManager, sender, target);
                }));
    }

    private static void handleSingleReset(
            @NotNull McRPG mcRPG,
            @NotNull McRPGLocalizationManager localizationManager,
            @NotNull Audience sender,
            @NotNull Player target,
            @NotNull Statistic statistic) {

        Optional<McRPGPlayer> mcRPGPlayerOptional = mcRPG.registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.PLAYER)
                .getPlayer(target.getUniqueId());
        if (mcRPGPlayerOptional.isPresent()) {
            mcRPGPlayerOptional.get().getStatisticData().setValue(statistic.getStatisticKey(), statistic.getDefaultValue());
        }

        Map<String, String> placeholders = getStatisticPlaceholders(sender, sender, target, statistic, statistic.getDefaultValue());
        placeholders.put("target", target.getName());
        sender.sendMessage(localizationManager.getLocalizedMessageAsComponent(
                sender, LocalizationKey.STATISTIC_RESET_SUCCESS, placeholders));
    }

    private static void handleResetAll(
            @NotNull McRPG mcRPG,
            @NotNull McRPGLocalizationManager localizationManager,
            @NotNull Audience sender,
            @NotNull Player target) {

        StatisticRegistry statisticRegistry = mcRPG.registryAccess().registry(RegistryKey.STATISTIC);
        Optional<McRPGPlayer> mcRPGPlayerOptional = mcRPG.registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.PLAYER)
                .getPlayer(target.getUniqueId());
        if (mcRPGPlayerOptional.isPresent()) {
            McRPGPlayer mcRPGPlayer = mcRPGPlayerOptional.get();
            for (Statistic stat : statisticRegistry.getRegisteredStatistics()) {
                mcRPGPlayer.getStatisticData().setValue(stat.getStatisticKey(), stat.getDefaultValue());
            }
        }

        Map<String, String> placeholders = getStatisticPlaceholders(sender, sender, target, null, null);
        placeholders.put("target", target.getName());
        sender.sendMessage(localizationManager.getLocalizedMessageAsComponent(
                sender, LocalizationKey.STATISTIC_RESET_ALL_SUCCESS, placeholders));
    }
}
