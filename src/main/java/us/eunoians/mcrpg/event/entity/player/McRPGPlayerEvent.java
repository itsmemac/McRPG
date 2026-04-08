package us.eunoians.mcrpg.event.entity.player;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;

/**
 * A template event that signifies some sort of action happening to or with
 * a {@link McRPGPlayer}.
 */
public abstract class McRPGPlayerEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final McRPGPlayer mcRPGPlayer;

    public McRPGPlayerEvent(@NotNull McRPGPlayer mcRPGPlayer) {
        this.mcRPGPlayer = mcRPGPlayer;
    }

    /**
     * Gets the {@link McRPGPlayer} that's the subject of this event.
     *
     * @return The {@link McRPGPlayer} that's the subject of this event.
     */
    @NotNull
    public final McRPGPlayer getMcRPGPlayer() {
        return mcRPGPlayer;
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
