package us.eunoians.mcrpg.command.statistic;

import com.diamonddagger590.mccore.registry.RegistryKey;
import com.diamonddagger590.mccore.statistic.Statistic;
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
import us.eunoians.mcrpg.command.parser.StatisticParser;
import us.eunoians.mcrpg.configuration.file.localization.LocalizationKey;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.localization.McRPGLocalizationManager;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;

import java.util.Map;
import java.util.Optional;

/**
 * Command: {@code /mcrpg statistic view <player> <statistic>}
 * <p>
 * Views a specific statistic value for a player.
 */
public class StatisticViewCommand extends StatisticCommandBase {

    private static final Permission VIEW_PERMISSION = Permission.of("mcrpg.statistic.view");

    /** Registers the {@code /mcrpg statistic view} command. */
    public static void registerCommand() {
        McRPG mcRPG = McRPG.getInstance();
        CommandManager<CommandSourceStack> commandManager = mcRPG.registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.COMMAND).getCommandManager();
        McRPGLocalizationManager localizationManager = mcRPG.registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.LOCALIZATION);

        commandManager.command(commandManager.commandBuilder("mcrpg")
                .literal("statistic")
                .commandDescription(RichDescription.richDescription(
                        localizationManager.getLocalizedMessageAsComponent(LocalizationKey.COMMAND_DESCRIPTION_STATISTIC)))
                .literal("view")
                .required("player", PlayerParser.playerParser(),
                        RichDescription.richDescription(
                                localizationManager.getLocalizedMessageAsComponent(LocalizationKey.COMMAND_DESCRIPTION_STATISTIC_VIEW_PLAYER)))
                .required("statistic", StatisticParser.statisticParser(),
                        RichDescription.richDescription(
                                localizationManager.getLocalizedMessageAsComponent(LocalizationKey.COMMAND_DESCRIPTION_STATISTIC_VIEW_STATISTIC)))
                .permission(Permission.anyOf(ROOT_PERMISSION, STATISTIC_COMMAND_ROOT_PERMISSION, VIEW_PERMISSION))
                .handler(commandContext -> {
                    Player target = commandContext.get(CloudKey.of("player", Player.class));
                    Statistic statistic = commandContext.get(CloudKey.of("statistic", Statistic.class));
                    Audience sender = commandContext.sender().getSender();

                    Optional<McRPGPlayer> mcRPGPlayerOptional = mcRPG.registryAccess()
                            .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.PLAYER)
                            .getPlayer(target.getUniqueId());
                    Object value = mcRPGPlayerOptional
                            .map(p -> p.getStatisticData().getValue(statistic.getStatisticKey()).orElse(statistic.getDefaultValue()))
                            .orElse(statistic.getDefaultValue());

                    Map<String, String> placeholders = getStatisticPlaceholders(sender, sender, target, statistic, value);
                    placeholders.put("target", target.getName());
                    sender.sendMessage(localizationManager.getLocalizedMessageAsComponent(
                            sender, LocalizationKey.STATISTIC_VIEW_MESSAGE, placeholders));
                }));
    }
}
