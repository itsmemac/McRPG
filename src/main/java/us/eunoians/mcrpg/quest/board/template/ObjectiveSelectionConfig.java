package us.eunoians.mcrpg.quest.board.template;

import org.jetbrains.annotations.NotNull;

/**
 * Configures how objectives are selected within a template stage.
 * The default mode ({@code ALL}) preserves existing behavior.
 * {@code WEIGHTED_RANDOM} selects a random subset by weight without replacement.
 *
 * @param mode     the selection mode
 * @param minCount the minimum number of objectives to select
 * @param maxCount the maximum number of objectives to select
 */
public record ObjectiveSelectionConfig(
        @NotNull ObjectiveSelectionMode mode,
        int minCount,
        int maxCount
) {

    /**
     * Canonical constructor — validates that {@code minCount >= 1} and
     * {@code maxCount >= minCount}.
     *
     * @throws IllegalArgumentException if either constraint is violated
     */
    public ObjectiveSelectionConfig {
        if (minCount < 1) {
            throw new IllegalArgumentException("minCount must be >= 1, got: " + minCount);
        }
        if (maxCount < minCount) {
            throw new IllegalArgumentException(
                    "maxCount must be >= minCount, got: max=" + maxCount + ", min=" + minCount);
        }
    }

    /**
     * Controls how objectives within a template stage are selected during generation.
     * <ul>
     *   <li>{@link #ALL} — include every objective; preserves behaviour before weighted selection.</li>
     *   <li>{@link #WEIGHTED_RANDOM} — select a random subset by weight, without replacement,
     *       using the count bounds from the enclosing {@link ObjectiveSelectionConfig}.</li>
     * </ul>
     */
    public enum ObjectiveSelectionMode {
        ALL,
        WEIGHTED_RANDOM
    }
}
