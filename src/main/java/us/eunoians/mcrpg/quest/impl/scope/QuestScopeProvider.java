package us.eunoians.mcrpg.quest.impl.scope;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.expansion.content.McRPGContent;
import us.eunoians.mcrpg.quest.QuestManager;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract provider responsible for creating and loading {@link QuestScope} instances
 * of a specific type. Each provider is registered in the
 * {@link QuestScopeProviderRegistry} under a unique {@link NamespacedKey}.
 * <p>
 * Providers also support rescoping: finding all active quests where a given player
 * is currently in scope. This is used at player login and when scope membership
 * changes at runtime (e.g., joining a party or land).
 * <p>
 * Implements {@link McRPGContent} so that third-party scope providers can be
 * distributed via content expansion packs.
 *
 * @param <P> the concrete scope type this provider manages
 */
public abstract class QuestScopeProvider<P extends QuestScope> implements McRPGContent {

    /**
     * Gets the unique key identifying this scope provider type.
     *
     * @return the provider's namespaced key
     */
    @NotNull
    public abstract NamespacedKey getKey();

    /**
     * Creates a new scope instance for a quest that is just starting.
     *
     * @param questUUID the UUID of the quest instance
     * @return a new scope instance
     */
    @NotNull
    public abstract P createNewScope(@NotNull UUID questUUID);

    /**
     * Loads an existing scope instance from persistent storage synchronously, reusing an
     * already-open database connection. Intended for use on the DB executor thread where a
     * connection is already available (e.g. during quest tree loading at startup or rescoping).
     * <p>
     * For callers that do not have an open connection, use {@link #loadScope(UUID, UUID)} instead.
     *
     * @param connection the open database connection to use
     * @param questUUID  the UUID of the quest instance whose scope to load
     * @return the fully populated scope instance
     */
    @NotNull
    public abstract P loadScope(@NotNull Connection connection, @NotNull UUID questUUID);

    /**
     * Loads an existing scope instance from persistent storage asynchronously, opening its
     * own connection on the DB executor thread. Use this from the main thread or any context
     * without an existing connection. For DB-thread callers with an open connection, prefer
     * {@link #loadScope(Connection, UUID)}.
     *
     * @param questUUID the UUID of the quest instance
     * @param scopeUUID the UUID of the scope record (if applicable)
     * @return a future that completes with the loaded scope
     */
    @NotNull
    public abstract CompletableFuture<P> loadScope(@NotNull UUID questUUID, @NotNull UUID scopeUUID);

    /**
     * Finds all active quest UUIDs where the given player is currently in scope
     * according to this provider's membership rules. Called from the database
     * executor thread during player login or scope-change rescoping.
     * <p>
     * Implementations typically query their scope table JOINed with the quest
     * instances table, then filter by membership (which may involve runtime
     * checks like Lands API membership or permission checks).
     *
     * @param playerUUID the player to resolve quests for
     * @param connection the database connection (already on the DB executor thread)
     * @return a list of active quest UUIDs where the player is in scope
     */
    @NotNull
    public abstract List<UUID> resolveActiveQuestUUIDs(@NotNull UUID playerUUID, @NotNull Connection connection);

    /**
     * Registers any Bukkit or plugin event listeners that detect scope membership
     * changes for this provider type. When such a change is detected, the listener
     * should trigger a rescope for the affected player via
     * {@link QuestManager#rescopePlayer(UUID, QuestScopeProvider)}.
     * <p>
     * The default implementation does nothing, suitable for scope types whose
     * membership never changes at runtime (e.g., single-player scopes).
     *
     * @param questManager the quest manager to use for rescoping callbacks
     */
    public void registerScopeChangeListeners(@NotNull QuestManager questManager) {
        // Default no-op; override in providers whose scope membership can change at runtime
    }

    @NotNull
    @Override
    public Optional<NamespacedKey> getExpansionKey() {
        return Optional.empty();
    }
}
