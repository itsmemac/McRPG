package us.eunoians.mcrpg.quest.definition;

/**
 * Defines how a quest may be repeated by a player once completed.
 */
public enum QuestRepeatMode {

    /**
     * The quest can only be completed once per player, ever.
     */
    ONCE,

    /**
     * The quest can be completed any number of times with no restrictions.
     */
    REPEATABLE,

    /**
     * The quest can be repeated after a configured cooldown period elapses
     * since the player's last completion.
     */
    COOLDOWN,

    /**
     * The quest can be repeated up to a configured maximum number of times
     * per player.
     */
    LIMITED,

    /**
     * The quest can be repeated up to a configured maximum number of times
     * per player, and each repeat requires a configured cooldown to elapse
     * since the player's last completion. Both constraints must be satisfied —
     * the player must be within their total completion limit <em>and</em> the
     * cooldown since the last completion must have passed.
     * <p>
     * Requires both {@code repeat-limit} and {@code repeat-cooldown} to be set.
     */
    COOLDOWN_LIMITED
}
