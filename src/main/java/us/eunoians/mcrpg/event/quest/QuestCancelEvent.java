package us.eunoians.mcrpg.event.quest;

import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.quest.impl.QuestInstance;

/**
 * Fired when a quest instance is cancelled (manually or due to expiration).
 */
public class QuestCancelEvent extends QuestEvent {

    /**
     * Creates a new quest cancel event.
     *
     * @param questInstance the quest instance that was cancelled
     */
    public QuestCancelEvent(@NotNull QuestInstance questInstance) {
        super(questInstance);
    }
}
