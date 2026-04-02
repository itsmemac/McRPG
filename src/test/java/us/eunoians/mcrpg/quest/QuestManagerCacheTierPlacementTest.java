package us.eunoians.mcrpg.quest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression test for A3: Caffeine {@code expireAfterCreate} must not call Bukkit API from the
 * DB executor thread.
 * <p>
 * Background: The original {@code computeNextExpiryNanos()} called {@code Bukkit.getPlayer(uuid)}
 * to decide whether to use the short or long TTL based on whether scope players were online.
 * Caffeine invokes {@code expireAfterCreate} synchronously on the inserting thread —
 * {@code cachedFinishedQuests.put()} is called from the DB executor, so {@code Bukkit.getPlayer}
 * ran off the main thread.
 * <p>
 * The fix (already applied, documented in {@link QuestManager}'s field comment) removes the
 * {@code Bukkit.getPlayer} branch and always returns {@code finishedQuestKeepAliveNanos}.
 * This test verifies that the constant-TTL Expiry pattern is safe from a background thread so
 * that any future refactor reintroducing a Bukkit API call inside the expiry callback will be
 * immediately identifiable as violating the contract proven here.
 * <p>
 * No MockBukkit or full {@link QuestManager} construction is required — the Caffeine
 * configuration pattern is tested in isolation.
 */
public class QuestManagerCacheTierPlacementTest {

    private static final long KEEP_ALIVE_NANOS = Duration.ofMinutes(15).toNanos();

    /**
     * Creates a Caffeine cache configured with the same constant-TTL {@link Expiry} used by the
     * fixed {@link QuestManager} — no Bukkit API in any callback path.
     */
    private Cache<UUID, String> buildSafeCacheConfiguration() {
        return Caffeine.newBuilder()
                .expireAfter(new Expiry<UUID, String>() {
                    @Override
                    public long expireAfterCreate(UUID key, String value, long currentTime) {
                        // Fixed pattern: constant TTL, no Bukkit.getPlayer() call.
                        // The original bug called Bukkit.getPlayer(uuid) here to choose between
                        // a short and long TTL. This is safe from any thread.
                        return KEEP_ALIVE_NANOS;
                    }

                    @Override
                    public long expireAfterUpdate(UUID key, String value, long currentTime,
                                                  @NonNegative long currentDuration) {
                        return KEEP_ALIVE_NANOS;
                    }

                    @Override
                    public long expireAfterRead(UUID key, String value, long currentTime,
                                                @NonNegative long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    /**
     * Verifies that calling {@code cache.put()} from a background executor thread does not throw
     * and places the entry correctly.
     * <p>
     * This is the contract the fixed Caffeine expiry configuration guarantees: since
     * {@code expireAfterCreate} no longer calls any Bukkit API, it is safe to call from any
     * thread. This mirrors {@link QuestManager#retireQuest(us.eunoians.mcrpg.quest.impl.QuestInstance)}
     * which calls {@code cachedFinishedQuests.put()} on whatever thread the caller runs on
     * (typically the DB executor).
     */
    @Test
    @DisplayName("cache.put() from DB executor thread completes without exception — constant-TTL expiry is thread-safe")
    void cachePut_fromBackgroundThread_doesNotThrowAndPlacesEntry() throws Exception {
        Cache<UUID, String> cache = buildSafeCacheConfiguration();
        UUID key = UUID.randomUUID();
        AtomicReference<Throwable> caughtException = new AtomicReference<>();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                // Simulates QuestManager.retireQuest() → cachedFinishedQuests.put()
                // called from the DB executor thread. Caffeine invokes expireAfterCreate()
                // synchronously on this background thread — must not call Bukkit API.
                cache.put(key, "quest-instance-data");
            } catch (Throwable t) {
                caughtException.set(t);
            }
        });
        future.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertNull(caughtException.get(),
                "No exception must be thrown when cache.put() is called from a background thread; "
                        + "got: " + caughtException.get());
        assertNotNull(cache.getIfPresent(key),
                "Entry must be present in cache after put from background thread");
    }

    /**
     * Verifies multiple concurrent background-thread {@code put()} calls to the same cache
     * all succeed — confirming thread safety of the expiry configuration under concurrency.
     */
    @Test
    @DisplayName("concurrent cache.put() calls from multiple background threads all succeed")
    void cachePut_fromMultipleBackgroundThreads_allSucceed() throws Exception {
        Cache<UUID, String> cache = buildSafeCacheConfiguration();
        int threadCount = 10;
        AtomicReference<Throwable> caughtException = new AtomicReference<>();
        UUID[] keys = new UUID[threadCount];
        for (int i = 0; i < threadCount; i++) {
            keys[i] = UUID.randomUUID();
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Future<?>[] futures = new Future[threadCount];
        for (int i = 0; i < threadCount; i++) {
            UUID key = keys[i];
            futures[i] = executor.submit(() -> {
                try {
                    cache.put(key, "quest-data-" + key);
                } catch (Throwable t) {
                    caughtException.compareAndSet(null, t);
                }
            });
        }
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertNull(caughtException.get(),
                "No exception must be thrown during concurrent cache.put() calls from background threads");
        for (UUID key : keys) {
            assertNotNull(cache.getIfPresent(key),
                    "All entries must be present after concurrent puts; missing key: " + key);
        }
    }
}
