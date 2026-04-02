package us.eunoians.mcrpg.gui.board;

import com.diamonddagger590.mccore.registry.RegistryAccess;
import com.diamonddagger590.mccore.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.configuration.file.localization.LocalizationKey;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.localization.McRPGLocalizationManager;
import us.eunoians.mcrpg.quest.board.rarity.QuestRarity;
import us.eunoians.mcrpg.quest.definition.QuestDefinition;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;
import us.eunoians.mcrpg.util.McRPGMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds rich, localized lore for board offering items.
 * All displayed text is resolved through {@link McRPGLocalizationManager} to respect
 * the player's locale chain.
 * <p>
 * Stateless — a new instance can be created whenever needed.
 */
public final class OfferingLoreBuilder {

    @NotNull
    private McRPGLocalizationManager localizationManager() {
        return RegistryAccess.registryAccess()
                .registry(RegistryKey.MANAGER)
                .manager(McRPGManagerKey.LOCALIZATION);
    }

    /**
     * Builds the full lore for a board offering item.
     *
     * @param player          the viewing player (for locale resolution)
     * @param rarity          the offering's quest rarity
     * @param categoryDisplay the display name for the category
     * @return ordered list of lore components
     */
    @NotNull
    public List<Component> buildOfferingLore(
            @NotNull McRPGPlayer player,
            @NotNull QuestRarity rarity,
            @NotNull String categoryDisplay) {

        McRPGLocalizationManager localization = localizationManager();
        List<Component> lore = new ArrayList<>();

        lore.add(localization.getLocalizedMessageAsComponent(player,
                LocalizationKey.QUEST_BOARD_OFFERING_CATEGORY,
                Map.of("category", categoryDisplay)));

        lore.add(Component.empty());
        lore.add(localization.getLocalizedMessageAsComponent(player, LocalizationKey.QUEST_BOARD_CLICK_TO_ACCEPT));

        return lore;
    }

    /**
     * Builds a timer line showing time remaining, localized for the given player.
     *
     * @param player      the viewing player (for locale resolution)
     * @param remainingMs remaining time in milliseconds
     * @return the timer component, or empty if no time remaining
     */
    @NotNull
    public Optional<Component> buildTimerLine(@NotNull McRPGPlayer player, long remainingMs) {
        if (remainingMs <= 0) {
            return Optional.empty();
        }
        McRPGLocalizationManager localization = localizationManager();
        return Optional.of(localization.getLocalizedMessageAsComponent(player,
                LocalizationKey.QUEST_BOARD_EXPIRES_IN,
                Map.of("time", McRPGMethods.formatDuration(remainingMs))));
    }

}
