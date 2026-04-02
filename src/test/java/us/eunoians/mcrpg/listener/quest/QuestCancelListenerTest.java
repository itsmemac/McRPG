package us.eunoians.mcrpg.listener.quest;

import com.diamonddagger590.mccore.registry.RegistryAccess;
import com.diamonddagger590.mccore.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.eunoians.mcrpg.McRPGBaseTest;
import us.eunoians.mcrpg.event.quest.QuestCancelEvent;
import us.eunoians.mcrpg.quest.QuestManager;
import us.eunoians.mcrpg.quest.QuestTestHelper;
import us.eunoians.mcrpg.quest.definition.QuestDefinition;
import us.eunoians.mcrpg.quest.definition.QuestDefinitionRegistry;
import us.eunoians.mcrpg.quest.impl.QuestInstance;
import us.eunoians.mcrpg.registry.McRPGRegistryKey;
import us.eunoians.mcrpg.quest.board.QuestBoardTerminator;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class QuestCancelListenerTest extends McRPGBaseTest {

    private QuestManager mockQuestManager;

    @AfterEach
    public void tearDown() {
        HandlerList.unregisterAll(mcRPG);
    }

    @BeforeEach
    public void setup() {
        HandlerList.unregisterAll(mcRPG);
        server.getPluginManager().clearEvents();
        server.getPluginManager().registerEvents(new QuestCancelListener(new QuestBoardTerminator(mcRPG)), mcRPG);

        mockQuestManager = RegistryAccess.registryAccess().registry(RegistryKey.MANAGER).manager(McRPGManagerKey.QUEST);
    }

    @DisplayName("Given a cancelled quest, when QuestCancelEvent fires, then retireQuest is called")
    @Test
    public void onQuestCancel_retiresQuest() {
        QuestDefinition def = QuestTestHelper.singlePhaseQuest("cancel_listener_test");
        QuestInstance quest = QuestTestHelper.startedQuestInstance(def);

        server.getPluginManager().callEvent(new QuestCancelEvent(quest));
        verify(mockQuestManager).retireQuest(any(QuestInstance.class));
    }

    @DisplayName("Given a quest with a gen_ prefix key registered in the definition registry, when QuestCancelEvent fires, then the ephemeral definition is deregistered")
    @Test
    public void onQuestCancel_deregistersEphemeralDefinition_whenKeyHasGenPrefix() {
        QuestDefinitionRegistry definitionRegistry = RegistryAccess.registryAccess()
                .registry(McRPGRegistryKey.QUEST_DEFINITION);
        // Quest key "gen_ephemeral_cancel" — local part starts with "gen_" as required by the guard
        QuestDefinition ephemeralDef = QuestTestHelper.singlePhaseQuest("gen_ephemeral_cancel");
        definitionRegistry.register(ephemeralDef);

        QuestInstance quest = QuestTestHelper.startedQuestInstance(ephemeralDef);
        server.getPluginManager().callEvent(new QuestCancelEvent(quest));

        assertFalse(
                definitionRegistry.get(new NamespacedKey("mcrpg", "gen_ephemeral_cancel")).isPresent(),
                "Ephemeral definition must be removed from the registry on quest cancellation");
    }

    @DisplayName("Given a quest whose key does not start with gen_, when QuestCancelEvent fires, then the definition remains registered")
    @Test
    public void onQuestCancel_doesNotDeregisterNonEphemeralDefinition() {
        QuestDefinitionRegistry definitionRegistry = RegistryAccess.registryAccess()
                .registry(McRPGRegistryKey.QUEST_DEFINITION);
        QuestDefinition normalDef = QuestTestHelper.singlePhaseQuest("normal_cancel_test");
        definitionRegistry.register(normalDef);

        QuestInstance quest = QuestTestHelper.startedQuestInstance(normalDef);
        server.getPluginManager().callEvent(new QuestCancelEvent(quest));

        assertTrue(
                definitionRegistry.get(new NamespacedKey("mcrpg", "normal_cancel_test")).isPresent(),
                "Non-ephemeral definition must remain in the registry after quest cancellation");
    }
}
