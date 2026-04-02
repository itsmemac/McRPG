package us.eunoians.mcrpg.gui.quest.slot;

import com.diamonddagger590.mccore.registry.RegistryAccess;
import com.diamonddagger590.mccore.registry.RegistryKey;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import us.eunoians.mcrpg.McRPGBaseTest;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.entity.player.McRPGPlayerExtension;
import us.eunoians.mcrpg.gui.quest.QuestAbandonConfirmGui;
import us.eunoians.mcrpg.quest.QuestManager;
import us.eunoians.mcrpg.quest.impl.QuestInstance;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(McRPGPlayerExtension.class)
public class QuestAbandonConfirmSlotTest extends McRPGBaseTest {

    @Test
    @DisplayName("Given a slot for a quest instance, when clicked with an online player, then abandonQuest is called with the quest's UUID")
    void givenOnlinePlayer_whenClicked_thenQuestManagerAbandonQuestIsCalledWithCorrectUUID(McRPGPlayer mcRPGPlayer) {
        addPlayerToServer(mcRPGPlayer);

        UUID questUUID = UUID.randomUUID();
        QuestInstance questInstance = mock(QuestInstance.class);
        when(questInstance.getQuestUUID()).thenReturn(questUUID);

        new QuestAbandonConfirmSlot(questInstance, "Test Quest").onClick(mcRPGPlayer, ClickType.LEFT);

        QuestManager questManager = RegistryAccess.registryAccess()
                .registry(RegistryKey.MANAGER)
                .manager(McRPGManagerKey.QUEST);
        verify(questManager).abandonQuest(questUUID);
    }

    @Test
    @DisplayName("Given a slot, when onClick is called, then it returns true regardless of click type")
    void givenSlot_whenOnClick_thenAlwaysReturnsTrue(McRPGPlayer mcRPGPlayer) {
        addPlayerToServer(mcRPGPlayer);

        QuestInstance questInstance = mock(QuestInstance.class);
        when(questInstance.getQuestUUID()).thenReturn(UUID.randomUUID());

        boolean result = new QuestAbandonConfirmSlot(questInstance, "Test Quest").onClick(mcRPGPlayer, ClickType.RIGHT);
        assertTrue(result);
    }

    @Test
    @DisplayName("Given a slot, when getValidGuiTypes is called, then it returns only QuestAbandonConfirmGui")
    void givenAbandonConfirmSlot_whenGetValidGuiTypes_thenReturnsQuestAbandonConfirmGui() {
        QuestInstance questInstance = mock(QuestInstance.class);
        QuestAbandonConfirmSlot slot = new QuestAbandonConfirmSlot(questInstance, "Test Quest");
        assertEquals(Set.of(QuestAbandonConfirmGui.class), slot.getValidGuiTypes());
    }

    @Test
    @DisplayName("Given an offline player, when onClick is called, then abandonQuest is not invoked")
    void givenOfflinePlayer_whenClicked_thenAbandonQuestIsNotCalled(McRPGPlayer mcRPGPlayer) {
        // player is NOT added to server — getAsBukkitPlayer() will return empty
        UUID questUUID = UUID.randomUUID();
        QuestInstance questInstance = mock(QuestInstance.class);
        when(questInstance.getQuestUUID()).thenReturn(questUUID);

        new QuestAbandonConfirmSlot(questInstance, "Test Quest").onClick(mcRPGPlayer, ClickType.LEFT);

        QuestManager questManager = RegistryAccess.registryAccess()
                .registry(RegistryKey.MANAGER)
                .manager(McRPGManagerKey.QUEST);
        // Verify no interactions since the Bukkit player was absent
        org.mockito.Mockito.verifyNoInteractions(questManager);
    }
}
