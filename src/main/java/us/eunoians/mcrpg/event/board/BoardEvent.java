package us.eunoians.mcrpg.event.board;

import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.quest.board.QuestBoard;

/**
 * Base event for all quest board events. Carries a reference to the
 * {@link QuestBoard} that the event pertains to.
 */
public abstract class BoardEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final QuestBoard board;

    /**
     * Creates a new board event.
     *
     * @param board the board associated with this event
     */
    public BoardEvent(@NotNull QuestBoard board) {
        this.board = board;
    }

    /**
     * Gets the {@link QuestBoard} associated with this event.
     *
     * @return the quest board
     */
    @NotNull
    public QuestBoard getBoard() {
        return board;
    }

    /**
     * Gets the board's key.
     *
     * @return the board key
     */
    @NotNull
    public NamespacedKey getBoardKey() {
        return board.getBoardKey();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
