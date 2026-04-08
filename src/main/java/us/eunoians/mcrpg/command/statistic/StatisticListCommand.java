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
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.configuration.file.localization.LocalizationKey;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.localization.McRPGLocalizationManager;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;

import java.util.Map;
import java.util.Optional;

/**
 * Command: {@code /mcrpg statistic list <player>}
 * <p>
 * Lists all statistic values for a player.
 */
public class StatisticListCommand extends StatisticCommandBase {

    private static final Permission LIST_PERMISSION = Permission.of("mcrpg.statistic.list");

    /** Registers the {@code /mcrpg statistic list} command. */
    public static void registerCommand() {
        McRPG mcRPG = McRPG.getInstance();
        CommandManager<CommandSourceStack> commandManager = mcRPG.registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.COMMAND).getCommandManager();
        McRPGLocalizationManager localizationManager = mcRPG.registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.LOCALIZATION);

        commandManager.command(commandManager.commandBuilder("mcrpg")
                .literal("statistic")
                .literal("list")
                .required("player", PlayerParser.playerParser(),
                        RichDescription.richDescription(
                                localizationManager.getLocalizedMessageAsComponent(LocalizationKey.COMMAND_DESCRIPTION_STATISTIC_LIST_PLAYER)))
                .permission(Permission.anyOf(ROOT_PERMISSION, STATISTIC_COMMAND_ROOT_PERMISSION, LIST_PERMISSION))
                .handler(commandContext -> {
                    Player target = commandContext.get(CloudKey.of("player", Player.class));
                    Audience sender = commandContext.sender().getSender();
                    StatisticRegistry statisticRegistry = mcRPG.registryAccess().registry(RegistryKey.STATISTIC);

                    Optional<McRPGPlayer> mcRPGPlayerOptional = mcRPG.registryAccess()
                            .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.PLAYER)
                            .getPlayer(target.getUniqueId());
                    sendStatisticList(sender, target, statisticRegistry, statistic -> mcRPGPlayerOptional
                            .map(p -> p.getStatisticData().getValue(statistic.getStatisticKey()).orElse(statistic.getDefaultValue()))
                            .orElse(statistic.getDefaultValue()), localizationManager);
                }));
    }

    /**
     * Sends the formatted statistic list to the sender.
     *
     * @param sender              The audience to send messages to.
     * @param target              The target player.
     * @param statisticRegistry   The statistic registry.
     * @param valueResolver       A function that resolves a statistic's value.
     * @param localizationManager The localization manager.
     */
    private static void sendStatisticList(
            @NotNull Audience sender,
            @NotNull Player target,
            @NotNull StatisticRegistry statisticRegistry,
            @NotNull StatisticValueResolver valueResolver,
            @NotNull McRPGLocalizationManager localizationManager) {

        Map<String, String> headerPlaceholders = getStatisticPlaceholders(sender, sender, target, null, null);
        headerPlaceholders.put("target", target.getName());
        sender.sendMessage(localizationManager.getLocalizedMessageAsComponent(
                sender, LocalizationKey.STATISTIC_LIST_HEADER, headerPlaceholders));

        for (Statistic statistic : statisticRegistry.getRegisteredStatistics()) {
            Object value = valueResolver.resolve(statistic);
            Map<String, String> entryPlaceholders = getStatisticPlaceholders(sender, sender, target, statistic, value);
            entryPlaceholders.put("target", target.getName());
            sender.sendMessage(localizationManager.getLocalizedMessageAsComponent(
                    sender, LocalizationKey.STATISTIC_LIST_ENTRY, entryPlaceholders));
        }
    }

    @FunctionalInterface
    private interface StatisticValueResolver {
        Object resolve(@NotNull Statistic statistic);
    }
}
