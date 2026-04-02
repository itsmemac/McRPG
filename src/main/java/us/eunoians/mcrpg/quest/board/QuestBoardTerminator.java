package us.eunoians.mcrpg.quest.board;

import com.diamonddagger590.mccore.database.Database;
import com.diamonddagger590.mccore.registry.RegistryAccess;
import com.diamonddagger590.mccore.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.database.table.board.PlayerBoardStateDAO;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.quest.definition.QuestDefinitionRegistry;
import us.eunoians.mcrpg.quest.impl.QuestInstance;
import us.eunoians.mcrpg.quest.source.builtin.BoardPersonalQuestSource;
import us.eunoians.mcrpg.registry.McRPGRegistryKey;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;

import java.sql.Connection;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Shared helper that handles the common board-slot teardown logic triggered when a quest
 * transitions to a terminal state (completed or cancelled).
 * <p>
 * Eliminates duplication between {@link us.eunoians.mcrpg.listener.quest.QuestCompleteListener}
 * and {@link us.eunoians.mcrpg.listener.quest.QuestCancelListener}.
 */
public class QuestBoardTerminator {

    private final McRPG plugin;

    public QuestBoardTerminator(@NotNull McRPG plugin) {
        this.plugin = plugin;
    }

    /**
     * Decrements the in-memory board-quest slot counter for each online player in scope
     * of the terminated quest. Only applies to quests sourced from the personal board.
     *
     * @param questInstance the quest whose slot should be freed
     */
    public void decrementBoardCount(@NotNull QuestInstance questInstance) {
        if (!questInstance.getQuestSource().getKey().equals(BoardPersonalQuestSource.KEY)) {
            return;
        }
        questInstance.getQuestScope().ifPresent(scope -> {
            for (UUID playerUUID : scope.getCurrentPlayersInScope()) {
                RegistryAccess.registryAccess().registry(RegistryKey.MANAGER)
                        .manager(McRPGManagerKey.PLAYER)
                        .<McRPGPlayer>getPlayer(playerUUID)
                        .ifPresent(p -> p.asQuestHolder().decrementBoardQuestCount());
            }
        });
    }

    /**
     * Deregisters a template-generated quest definition (identified by the {@code gen_} key prefix)
     * from the definition registry once the quest reaches a terminal state. No-op for hand-crafted
     * quest definitions.
     *
     * @param questKey the quest definition key to check and potentially deregister
     */
    public void deregisterEphemeralDefinition(@NotNull NamespacedKey questKey) {
        if (questKey.getKey().startsWith("gen_")) {
            QuestDefinitionRegistry definitionRegistry = RegistryAccess.registryAccess()
                    .registry(McRPGRegistryKey.QUEST_DEFINITION);
            if (definitionRegistry.deregister(questKey)) {
                plugin.getLogger().fine("Deregistered ephemeral definition " + questKey);
            }
        }
    }

    /**
     * Updates the player board state from {@code ACCEPTED} to the given terminal state so the
     * board quest counter is freed up for new quests. The update is submitted asynchronously
     * on the database executor.
     *
     * @param questInstance the quest instance to release
     * @param newState      the terminal state string (e.g. {@code "COMPLETED"}, {@code "CANCELLED"})
     */
    public void releaseBoardSlot(@NotNull QuestInstance questInstance, @NotNull String newState) {
        var dbManager = RegistryAccess.registryAccess().registry(RegistryKey.MANAGER)
                .manager(McRPGManagerKey.DATABASE);
        if (dbManager == null) {
            return;
        }
        Database database = dbManager.getDatabase();
        UUID questUUID = questInstance.getQuestUUID();
        database.getDatabaseExecutorService().submit(() -> {
            try (Connection connection = database.getConnection()) {
                PlayerBoardStateDAO.updateStateByQuestInstanceUUID(connection, questUUID, newState);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to release board slot for quest " + questUUID, e);
            }
        });
    }
}
