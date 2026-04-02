package us.eunoians.mcrpg.quest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Regression tests for A5: non-atomic check-then-put in {@code QuestManager.getQuestInstance()}.
 * <p>
 * Background: The original {@code getQuestInstance()} used:
 * <pre>
 *   GetItemRequest existing = pendingRequests.get(questUUID);
 *   if (existing != null) return existing;
 *   // ... create new request ...
 *   pendingRequests.put(questUUID, request);
 * </pre>
 * Two concurrent callers can both observe a null result from {@code get()} (because neither has
 * called {@code put()} yet), build independent {@code CompletableFuture} instances, and then
 * both call {@code put()}. Only the second put's value wins. The first caller's future is orphaned
 * — it is never resolved, leaving that thread hanging indefinitely on the future.
 * <p>
 * The fix replaces the get+put pair with {@code putIfAbsent}: build the candidate request,
 * attempt {@code putIfAbsent}, and if the return value is non-null use the already-registered
 * request (the candidate is discarded). This is atomic: only one thread can "win" the insert.
 * <p>
 * These tests exercise the concurrent map operation patterns directly without requiring a full
 * {@link QuestManager} construction (which would require MockBukkit and plugin startup).
 */
public class QuestManagerGetInstanceDeduplicationTest {

    /**
     * Demonstrates the broken get+put pattern: when two threads race, both can observe a missing
     * key, create independent futures, and overwrite each other's put. The loser's future is
     * orphaned.
     * <p>
     * The {@link CyclicBarrier} forces both threads to complete the {@code get()} step before
     * either calls {@code put()}, reliably opening the race window.
     */
    @RepeatedTest(50)
    @DisplayName("broken get+put: two concurrent callers can both create independent futures — first one is orphaned")
    void brokenGetThenPut_twoCallersBothCreateFutures_firstIsOrphaned() throws Exception {
        ConcurrentHashMap<UUID, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
        UUID questUUID = UUID.randomUUID();
        AtomicInteger futuresCreated = new AtomicInteger(0);
        CyclicBarrier barrier = new CyclicBarrier(2);

        // Simulates the broken getQuestInstance() Tier 3 branch (two concurrent callers)
        Runnable brokenGet = () -> {
            try {
                // Step 1: both get() null concurrently
                CompletableFuture<String> existing = pendingRequests.get(questUUID);
                barrier.await(5, TimeUnit.SECONDS); // both pause after get(), before put()
                if (existing == null) {
                    CompletableFuture<String> newFuture = new CompletableFuture<>();
                    futuresCreated.incrementAndGet();
                    pendingRequests.put(questUUID, newFuture); // second put() silently wins
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(brokenGet);
        Future<?> f2 = executor.submit(brokenGet);
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Both threads created a future — the first one was overwritten and is now orphaned.
        // In production, the caller that got the overwritten future waits on it forever.
        assertEquals(2, futuresCreated.get(),
                "Broken pattern: both threads create futures; first is silently orphaned when second put() wins");
    }

    /**
     * Verifies the fixed {@code putIfAbsent} pattern: only one future is ever created for a given
     * UUID, regardless of how many threads race to create it.
     * <p>
     * {@code putIfAbsent} atomically inserts only when the key is absent and returns the
     * existing value if already present — preventing the double-create race.
     */
    @RepeatedTest(50)
    @DisplayName("fixed putIfAbsent: exactly one future created regardless of concurrency")
    void fixedPutIfAbsent_exactlyOneFutureCreated_underConcurrentLoad() throws Exception {
        ConcurrentHashMap<UUID, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
        UUID questUUID = UUID.randomUUID();
        AtomicInteger futuresCreated = new AtomicInteger(0);
        CyclicBarrier barrier = new CyclicBarrier(2);

        // Simulates the fixed getQuestInstance() Tier 3 branch
        Runnable fixedGet = () -> {
            try {
                barrier.await(5, TimeUnit.SECONDS); // both arrive before any putIfAbsent
                CompletableFuture<String> candidate = new CompletableFuture<>();
                CompletableFuture<String> winner = pendingRequests.putIfAbsent(questUUID, candidate);
                if (winner == null) {
                    // This thread won — it is the only one that submits the DB load
                    futuresCreated.incrementAndGet();
                    candidate.complete("quest-result"); // simulate DB load completion
                }
                // If winner != null, the DB load is already in progress; just use winner
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(fixedGet);
        Future<?> f2 = executor.submit(fixedGet);
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, futuresCreated.get(),
                "Fixed pattern: exactly one future must be created, preventing duplicate DB loads");
    }

    /**
     * Verifies that a caller arriving after the winner has already registered its future
     * receives exactly the same {@link CompletableFuture} instance — guaranteeing both callers
     * share the same in-flight load and will receive the same result.
     */
    @Test
    @DisplayName("fixed putIfAbsent: late caller receives the same future instance as the winner")
    void fixedPutIfAbsent_lateCaller_receivesSameFutureInstance() {
        ConcurrentHashMap<UUID, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
        UUID questUUID = UUID.randomUUID();

        // First caller (the "winner")
        CompletableFuture<String> winnerFuture = new CompletableFuture<>();
        CompletableFuture<String> existingForWinner = pendingRequests.putIfAbsent(questUUID, winnerFuture);
        assertEquals(null, existingForWinner, "Winner: putIfAbsent must return null (inserted successfully)");

        // Second caller (the "latecomer") creates its own candidate but putIfAbsent returns the winner's
        CompletableFuture<String> lateCandidate = new CompletableFuture<>();
        CompletableFuture<String> existingForLate = pendingRequests.putIfAbsent(questUUID, lateCandidate);

        assertSame(winnerFuture, existingForLate,
                "Latecomer must receive the winner's future instance, not its own candidate");

        // The map still holds the winner's future (not the latecomer's candidate)
        assertSame(winnerFuture, pendingRequests.get(questUUID),
                "Map must hold only the winner's future");
    }

    /**
     * Verifies that the fix scales correctly under higher concurrency: 10 threads racing
     * still produce exactly one DB load.
     */
    @RepeatedTest(20)
    @DisplayName("fixed putIfAbsent: exactly one future created under high concurrency (10 threads)")
    void fixedPutIfAbsent_exactlyOneFutureCreated_underHighConcurrency() throws Exception {
        int threadCount = 10;
        ConcurrentHashMap<UUID, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
        UUID questUUID = UUID.randomUUID();
        AtomicInteger futuresCreated = new AtomicInteger(0);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        Runnable fixedGet = () -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                CompletableFuture<String> candidate = new CompletableFuture<>();
                CompletableFuture<String> existing = pendingRequests.putIfAbsent(questUUID, candidate);
                if (existing == null) {
                    futuresCreated.incrementAndGet();
                    candidate.complete("quest-result");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Future<?>[] futures = new Future[threadCount];
        for (int i = 0; i < threadCount; i++) {
            futures[i] = executor.submit(fixedGet);
        }
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, futuresCreated.get(),
                "Fixed pattern: exactly one future created across " + threadCount + " concurrent threads");
    }
}
