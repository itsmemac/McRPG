package us.eunoians.mcrpg.gui.board.slot;

import com.diamonddagger590.mccore.registry.RegistryAccess;
import com.diamonddagger590.mccore.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import us.eunoians.mcrpg.McRPGBaseTest;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.entity.player.McRPGPlayerExtension;
import us.eunoians.mcrpg.gui.board.QuestBoardGui;
import us.eunoians.mcrpg.quest.board.BoardOffering;
import us.eunoians.mcrpg.quest.board.OfferingAcceptResult;
import us.eunoians.mcrpg.quest.board.QuestBoardManager;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(McRPGPlayerExtension.class)
class BoardOfferingSlotTest extends McRPGBaseTest {

    private QuestBoardManager mockBoardManager;

    @BeforeEach
    void registerBoardManager() {
        if (!RegistryAccess.registryAccess().registry(RegistryKey.MANAGER).registered(McRPGManagerKey.QUEST_BOARD)) {
            mockBoardManager = mock(QuestBoardManager.class);
            RegistryAccess.registryAccess().registry(RegistryKey.MANAGER).register(mockBoardManager);
        } else {
            mockBoardManager = RegistryAccess.registryAccess().registry(RegistryKey.MANAGER)
                    .manager(McRPGManagerKey.QUEST_BOARD);
        }
    }

    @Test
    @DisplayName("getValidGuiTypes returns only QuestBoardGui for a board offering")
    void getValidGuiTypes_returnsOnlyQuestBoardGui_whenBoardOfferingProvided() {
        BoardOffering offering = new BoardOffering(
                UUID.randomUUID(), UUID.randomUUID(),
                new NamespacedKey("mcrpg", "shared_daily"), 0,
                new NamespacedKey("mcrpg", "test_quest"),
                new NamespacedKey("mcrpg", "common"),
                null, Duration.ofHours(24));

        assertEquals(Set.of(QuestBoardGui.class), new BoardOfferingSlot(offering).getValidGuiTypes());
    }

    @Test
    @DisplayName("onClick returns true without throwing for an offline player")
    void onClick_returnsTrue_whenPlayerIsOffline(McRPGPlayer mcRPGPlayer) {
        BoardOffering offering = new BoardOffering(
                UUID.randomUUID(), UUID.randomUUID(),
                new NamespacedKey("mcrpg", "shared_daily"), 0,
                new NamespacedKey("mcrpg", "test_quest"),
                new NamespacedKey("mcrpg", "common"),
                null, Duration.ofHours(24));

        assertTrue(new BoardOfferingSlot(offering).onClick(mcRPGPlayer, ClickType.LEFT));
    }

    @Test
    @DisplayName("onClick delegates to QuestBoardManager.acceptOffering when the player is online")
    void onClick_callsAcceptOffering_whenPlayerIsOnline(McRPGPlayer mcRPGPlayer) {
        PlayerMock playerMock = addPlayerToServer(mcRPGPlayer);
        UUID offeringId = UUID.randomUUID();
        BoardOffering offering = new BoardOffering(
                offeringId, UUID.randomUUID(),
                new NamespacedKey("mcrpg", "shared_daily"), 0,
                new NamespacedKey("mcrpg", "test_quest"),
                new NamespacedKey("mcrpg", "common"),
                null, Duration.ofHours(24));

        when(mockBoardManager.acceptOffering(any(), any())).thenReturn(OfferingAcceptResult.NOT_AVAILABLE);

        new BoardOfferingSlot(offering).onClick(mcRPGPlayer, ClickType.LEFT);

        verify(mockBoardManager).acceptOffering(playerMock, offeringId);
    }
}
