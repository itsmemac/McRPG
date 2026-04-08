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
import org.incendo.cloud.parser.standard.StringParser;
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
 * Command: {@code /mcrpg statistic set <player> <statistic> <value>}
 * <p>
 * Sets a statistic value for a player. Requires {@code /mcrpg confirm}.
 */
public class StatisticSetCommand extends StatisticCommandBase {

    private static final Permission SET_PERMISSION = Permission.of("mcrpg.statistic.set");

    /** Registers the {@code /mcrpg statistic set} command. */
    public static void registerCommand() {
        McRPG mcRPG = McRPG.getInstance();
        CommandManager<CommandSourceStack> commandManager = mcRPG.registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.COMMAND).getCommandManager();
        McRPGLocalizationManager localizationManager = mcRPG.registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.LOCALIZATION);

        commandManager.command(commandManager.commandBuilder("mcrpg")
                .literal("statistic")
                .literal("set")
                .required("player", PlayerParser.playerParser(),
                        RichDescription.richDescription(
                                localizationManager.getLocalizedMessageAsComponent(LocalizationKey.COMMAND_DESCRIPTION_STATISTIC_SET_PLAYER)))
                .required("statistic", StatisticParser.statisticParser(),
                        RichDescription.richDescription(
                                localizationManager.getLocalizedMessageAsComponent(LocalizationKey.COMMAND_DESCRIPTION_STATISTIC_SET_STATISTIC)))
                .required("value", StringParser.stringParser(),
                        RichDescription.richDescription(
                                localizationManager.getLocalizedMessageAsComponent(LocalizationKey.COMMAND_DESCRIPTION_STATISTIC_SET_VALUE)))
                .permission(Permission.anyOf(ROOT_PERMISSION, ADMIN_BASE_PERMISSION, STATISTIC_COMMAND_ROOT_PERMISSION, SET_PERMISSION))
                .meta(ConfirmationManager.META_CONFIRMATION_REQUIRED, true)
                .handler(commandContext -> {
                    Player target = commandContext.get(CloudKey.of("player", Player.class));
                    Statistic statistic = commandContext.get(CloudKey.of("statistic", Statistic.class));
                    String valueString = commandContext.get(CloudKey.of("value", String.class));
                    Audience sender = commandContext.sender().getSender();

                    Object parsedValue;
                    try {
                        parsedValue = parseValue(statistic, valueString);
                    } catch (IllegalArgumentException e) {
                        Map<String, String> placeholders = getStatisticPlaceholders(sender, sender, target, statistic, null);
                        placeholders.put("target", target.getName());
                        placeholders.put("statistic-value", valueString);
                        sender.sendMessage(localizationManager.getLocalizedMessageAsComponent(
                                sender, LocalizationKey.STATISTIC_INVALID_VALUE, placeholders));
                        return;
                    }

                    Optional<McRPGPlayer> mcRPGPlayerOptional = mcRPG.registryAccess()
                            .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.PLAYER)
                            .getPlayer(target.getUniqueId());
                    if (mcRPGPlayerOptional.isPresent()) {
                        mcRPGPlayerOptional.get().getStatisticData().setValue(statistic.getStatisticKey(), parsedValue);
                    }

                    Map<String, String> placeholders = getStatisticPlaceholders(sender, sender, target, statistic, parsedValue);
                    placeholders.put("target", target.getName());
                    sender.sendMessage(localizationManager.getLocalizedMessageAsComponent(
                            sender, LocalizationKey.STATISTIC_SET_SUCCESS, placeholders));
                }));
    }

    /**
     * Parses a string value into the appropriate type for the given statistic.
     *
     * @param statistic   The statistic to parse the value for.
     * @param valueString The string value to parse.
     * @return The parsed value.
     * @throws NumberFormatException    If the value cannot be parsed as the expected numeric type.
     * @throws IllegalArgumentException If the statistic type does not support direct value setting.
     */
    @NotNull
    private static Object parseValue(@NotNull Statistic statistic, @NotNull String valueString) {
        return switch (statistic.getStatisticType()) {
            case INT -> Integer.parseInt(valueString);
            case LONG -> Long.parseLong(valueString);
            case DOUBLE -> Double.parseDouble(valueString);
            case STRING -> valueString;
            case TIMESTAMP -> throw new IllegalArgumentException("Timestamp statistics cannot be set via command");
            case SET_STRING -> throw new IllegalArgumentException("Set statistics cannot be set via command");
        };
    }
}
