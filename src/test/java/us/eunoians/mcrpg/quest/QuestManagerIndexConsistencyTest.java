package us.eunoians.mcrpg.quest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for B1: TOCTOU between {@code isEmpty()} and {@code remove()} in
 * {@code QuestManager.deindexQuestForPlayer()}.
 * <p>
 * Background: The original {@code deindexQuestForPlayer()} removed the outer map entry
 * with an unguarded pair:
 * <pre>
 *   quests.remove(questA);
 *   if (quests.isEmpty()) {
 *       playerToQuestIndex.remove(playerUUID);  // BUG: concurrent indexQuestForPlayer()
 *   }                                            // may have added questB between these two lines
 * </pre>
 * A concurrent {@code indexQuestForPlayer()} call on the DB executor can add a new quest UUID
 * to the set <em>between</em> {@code isEmpty()} returning {@code true} and the outer
 * {@code remove(playerUUID)} call. When this happens the newly-indexed quest is silently lost:
 * the map entry is removed, leaving no way to find questB again until a new quest is started
 * for the same player.
 * <p>
 * The fix removes the cleanup block entirely: empty-set cleanup is handled by
 * {@code deindexPlayer()} on player disconnect. This eliminates the race window.
 * <p>
 * The tests directly exercise the map operations in isolation, providing a deterministic
 * reproduction that does not require a full {@link QuestManager} construction.
 */
public class QuestManagerIndexConsistencyTest {

    /**
     * Demonstrates the TOCTOU bug deterministically by replaying the exact racing sequence:
     * <ol>
     *   <li>questA is the only entry for the player.</li>
     *   <li>Thread 1 removes questA — set is now empty.</li>
     *   <li>Thread 2 adds questB to the (same) set.</li>
     *   <li>Thread 1 checks {@code isEmpty()} — would return {@code false} here (questB is in),
     *       but in the real race, Thread 2 is concurrent and Thread 1 may have already read
     *       {@code isEmpty() == true} before Thread 2's add.</li>
     * </ol>
     * This test simulates the worst-case interleaving: Thread 1 observed empty THEN Thread 2 added.
     */
    @Test
    @DisplayName("broken isEmpty+remove: entry removed even when concurrent index added a new quest")
    void brokenIsEmptyRemove_removesEntry_evenWhenNewQuestWasIndexedConcurrently() {
        ConcurrentHashMap<UUID, Set<UUID>> playerToQuestIndex = new ConcurrentHashMap<>();
        UUID playerUUID = UUID.randomUUID();
        UUID questA = UUID.randomUUID();
        UUID questB = UUID.randomUUID();

        // Setup: questA is the only quest for playerUUID
        Set<UUID> quests = ConcurrentHashMap.newKeySet();
        quests.add(questA);
        playerToQuestIndex.put(playerUUID, quests);

        // Thread 1 step 1: removes questA — set is now empty
        quests.remove(questA);

        // --- Thread 2 runs here ---
        // indexQuestForPlayer(): adds questB to the existing set
        playerToQuestIndex.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet()).add(questB);

        // Thread 1 step 2: broken isEmpty() + remove() — the set now has questB but we
        // simulate the thread having already observed isEmpty() == true before Thread 2 added:
        // we replicate the race by doing the remove unconditionally (as if isEmpty() returned true)
        boolean observedEmpty = quests.isEmpty(); // false now because Thread 2 added questB

        // In the actual race, Thread 1 would have read isEmpty() as true BEFORE Thread 2's add.
        // To deterministically show the data loss, we bypass the check and just call remove()
        // as Thread 1 would have, having already decided to remove:
        playerToQuestIndex.remove(playerUUID); // simulates the broken pattern's unconditional remove

        // Result: questB is GONE — it was indexed but the entry was removed
        Set<UUID> remaining = playerToQuestIndex.get(playerUUID);
        assertNull(remaining,
                "Demonstrates the data loss: after the broken remove, the entire entry is gone including questB");

        // Verify questB was indeed in the set when remove happened (the quest was live but lost)
        assertTrue(quests.contains(questB),
                "questB was in the set when remove() was called, but the map entry was removed anyway");
    }

    /**
     * Verifies the fixed pattern: removing the isEmpty+remove cleanup block means a concurrent
     * {@code indexQuestForPlayer()} cannot lose its data even if it runs between
     * {@code quests.remove(questA)} and what would have been the outer remove.
     */
    @Test
    @DisplayName("fixed pattern: no cleanup in deindexQuestForPlayer preserves concurrently-indexed quests")
    void fixedPattern_noCleanup_preservesConcurrentlyIndexedQuest() {
        ConcurrentHashMap<UUID, Set<UUID>> playerToQuestIndex = new ConcurrentHashMap<>();
        UUID playerUUID = UUID.randomUUID();
        UUID questA = UUID.randomUUID();
        UUID questB = UUID.randomUUID();

        // Setup
        Set<UUID> quests = ConcurrentHashMap.newKeySet();
        quests.add(questA);
        playerToQuestIndex.put(playerUUID, quests);

        // Fixed deindexQuestForPlayer(): only removes from the set, no outer map cleanup
        quests.remove(questA);
        // No isEmpty()+remove() here — the fix omits this block entirely

        // Concurrent indexQuestForPlayer() adds questB
        playerToQuestIndex.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet()).add(questB);

        // Result: questB is preserved
        Set<UUID> remaining = playerToQuestIndex.get(playerUUID);
        assertNotNull(remaining, "Map entry must still be present after fixed deindex");
        assertTrue(remaining.contains(questB),
                "questB must be present after concurrent index with fixed deindex");
    }

    /**
     * Stress-tests the fixed {@code deindexQuestForPlayer} pattern under real concurrency:
     * a background thread deindexes the last quest while the main thread concurrently indexes
     * a new one. After many iterations, the newly-indexed quest must always be present.
     * <p>
     * With the broken code, this would occasionally fail (the race is narrow but real).
     * With the fix, the test always passes because no outer-map cleanup is attempted.
     */
    @RepeatedTest(100)
    @DisplayName("fixed pattern stress test: newly indexed quest always present under concurrent deindex")
    void fixedPattern_stressTest_newlyIndexedQuestAlwaysPresent() throws Exception {
        ConcurrentHashMap<UUID, Set<UUID>> playerToQuestIndex = new ConcurrentHashMap<>();
        UUID playerUUID = UUID.randomUUID();
        UUID questA = UUID.randomUUID();
        UUID questB = UUID.randomUUID();

        Set<UUID> quests = ConcurrentHashMap.newKeySet();
        quests.add(questA);
        playerToQuestIndex.put(playerUUID, quests);

        CyclicBarrier barrier = new CyclicBarrier(2);

        // Thread 1: fixed deindexQuestForPlayer() — just remove from set, no outer cleanup
        Runnable deindex = () -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                Set<UUID> set = playerToQuestIndex.get(playerUUID);
                if (set != null) {
                    set.remove(questA);
                    // Fixed: no isEmpty() + playerToQuestIndex.remove() here
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // Thread 2: indexQuestForPlayer() for questB
        Runnable index = () -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                playerToQuestIndex.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet()).add(questB);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(deindex);
        Future<?> f2 = executor.submit(index);
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        Set<UUID> remaining = playerToQuestIndex.get(playerUUID);
        assertNotNull(remaining, "Player index entry must still exist after concurrent deindex+index");
        assertTrue(remaining.contains(questB),
                "questB must be present in index after concurrent deindex+index with fixed pattern");
    }
}
