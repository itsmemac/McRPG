package us.eunoians.mcrpg.listener.quest;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.quest.QuestManager;
import us.eunoians.mcrpg.quest.objective.type.builtin.BlockBreakQuestContext;

/**
 * Listens for {@link BlockBreakEvent} and drives quest objective progress for any
 * active block-break objectives the breaking player is contributing to.
 */
public class BlockBreakQuestProgressListener implements QuestProgressListener {

    private final QuestManager questManager;

    public BlockBreakQuestProgressListener(@NotNull QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        progressQuests(questManager, event.getPlayer().getUniqueId(), new BlockBreakQuestContext(event));
    }
}
