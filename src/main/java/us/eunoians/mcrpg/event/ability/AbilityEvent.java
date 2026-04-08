package us.eunoians.mcrpg.event.ability;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.ability.Ability;

/**
 * This class serves as a generic event other events can extend if they relate to an {@link Ability} in
 * some manner.
 */
public abstract class AbilityEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Ability ability;

    public AbilityEvent(@NotNull Ability ability) {
        this.ability = ability;
    }

    /**
     * Gets the {@link Ability} associated with this event
     *
     * @return The {@link Ability} associated with this event
     */
    @NotNull
    public Ability getAbility() {
        return this.ability;
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
