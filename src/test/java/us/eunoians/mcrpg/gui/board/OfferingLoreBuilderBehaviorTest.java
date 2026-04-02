package us.eunoians.mcrpg.gui.board;

import com.diamonddagger590.mccore.registry.RegistryAccess;
import com.diamonddagger590.mccore.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import us.eunoians.mcrpg.McRPGBaseTest;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.entity.player.McRPGPlayerExtension;
import us.eunoians.mcrpg.localization.McRPGLocalizationManager;
import us.eunoians.mcrpg.quest.board.rarity.QuestRarity;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link OfferingLoreBuilder} against the localization manager
 * (mocked via {@link McRPGBaseTest} registry setup).
 */
@ExtendWith(McRPGPlayerExtension.class)
class OfferingLoreBuilderBehaviorTest extends McRPGBaseTest {

    @BeforeEach
    void stubLocalizationComponents() {
        McRPGLocalizationManager localization = RegistryAccess.registryAccess()
                .registry(RegistryKey.MANAGER)
                .manager(McRPGManagerKey.LOCALIZATION);
        lenient().when(localization.getLocalizedMessageAsComponent(
                        ArgumentMatchers.any(McRPGPlayer.class), any(), anyMap()))
                .thenReturn(Component.text("lore-line"));
        lenient().when(localization.getLocalizedMessageAsComponent(
                        ArgumentMatchers.any(McRPGPlayer.class), any()))
                .thenReturn(Component.text("lore-line"));
    }

    @Test
    @DisplayName("buildOfferingLore returns non-empty lore containing category and accept hint")
    void buildOfferingLore_returnsNonEmptyLore(McRPGPlayer player) {
        QuestRarity rarity = mock(QuestRarity.class);
        when(rarity.getNameColor()).thenReturn(Optional.empty());

        List<Component> lore = new OfferingLoreBuilder().buildOfferingLore(player, rarity, "daily");

        assertFalse(lore.isEmpty(), "Expected at least category line and accept hint");
    }

    @Test
    @DisplayName("buildTimerLine returns a component when remaining time is positive")
    void buildTimerLine_returnsComponent_whenRemainingTimeIsPositive(McRPGPlayer player) {
        Optional<Component> result = new OfferingLoreBuilder().buildTimerLine(player, 60_000L);

        assertTrue(result.isPresent(), "Expected a timer component for positive remaining time");
    }

    @Test
    @DisplayName("buildTimerLine returns empty when remaining time is zero")
    void buildTimerLine_returnsEmpty_whenRemainingTimeIsZero(McRPGPlayer player) {
        Optional<Component> result = new OfferingLoreBuilder().buildTimerLine(player, 0L);

        assertFalse(result.isPresent(), "Expected empty Optional when remaining time is zero");
    }

    @Test
    @DisplayName("buildTimerLine returns empty when remaining time is negative")
    void buildTimerLine_returnsEmpty_whenRemainingTimeIsNegative(McRPGPlayer player) {
        Optional<Component> result = new OfferingLoreBuilder().buildTimerLine(player, -1L);

        assertFalse(result.isPresent(), "Expected empty Optional when remaining time is negative");
    }
}
