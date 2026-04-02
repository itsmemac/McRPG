package us.eunoians.mcrpg.quest.board.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ObjectiveSelectionConfig} construction validation and
 * {@link ObjectiveSelectionConfig.ObjectiveSelectionMode} enumeration.
 * End-to-end YAML parsing is covered by the {@code QuestTemplateConfigLoader}.
 */
class ObjectiveSelectionConfigParsingTest {

    @Nested
    @DisplayName("ObjectiveSelectionConfig construction")
    class ConstructionValidation {

        @Test
        @DisplayName("valid WEIGHTED_RANDOM config with min and max")
        void validWeightedRandom() {
            ObjectiveSelectionConfig config = new ObjectiveSelectionConfig(
                    ObjectiveSelectionConfig.ObjectiveSelectionMode.WEIGHTED_RANDOM, 2, 3);
            assertEquals(ObjectiveSelectionConfig.ObjectiveSelectionMode.WEIGHTED_RANDOM, config.mode());
            assertEquals(2, config.minCount());
            assertEquals(3, config.maxCount());
        }

        @Test
        @DisplayName("valid ALL config with min equal to max")
        void validAllModeMinEqualsMax() {
            ObjectiveSelectionConfig config = new ObjectiveSelectionConfig(
                    ObjectiveSelectionConfig.ObjectiveSelectionMode.ALL, 1, 1);
            assertEquals(ObjectiveSelectionConfig.ObjectiveSelectionMode.ALL, config.mode());
        }

        @Test
        @DisplayName("min-count of 1 is the valid lower bound")
        void minCountOne() {
            ObjectiveSelectionConfig config = new ObjectiveSelectionConfig(
                    ObjectiveSelectionConfig.ObjectiveSelectionMode.WEIGHTED_RANDOM, 1, 5);
            assertEquals(1, config.minCount());
        }

        @Test
        @DisplayName("min-count below 1 throws IllegalArgumentException")
        void minCountBelowOnethrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    new ObjectiveSelectionConfig(ObjectiveSelectionConfig.ObjectiveSelectionMode.WEIGHTED_RANDOM, 0, 3));
        }

        @Test
        @DisplayName("max-count less than min-count throws IllegalArgumentException")
        void maxCountLessThanMinThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    new ObjectiveSelectionConfig(ObjectiveSelectionConfig.ObjectiveSelectionMode.WEIGHTED_RANDOM, 3, 2));
        }

        @Test
        @DisplayName("min-count equal to max-count is valid (select exactly N)")
        void minEqualsMaxIsValid() {
            ObjectiveSelectionConfig config = new ObjectiveSelectionConfig(
                    ObjectiveSelectionConfig.ObjectiveSelectionMode.WEIGHTED_RANDOM, 2, 2);
            assertEquals(2, config.minCount());
            assertEquals(2, config.maxCount());
        }
    }

    @Nested
    @DisplayName("ObjectiveSelectionMode enum")
    class ModeEnum {

        @Test
        @DisplayName("ALL mode is parseable from string")
        void allModeFromString() {
            ObjectiveSelectionConfig.ObjectiveSelectionMode mode =
                    ObjectiveSelectionConfig.ObjectiveSelectionMode.valueOf("ALL");
            assertEquals(ObjectiveSelectionConfig.ObjectiveSelectionMode.ALL, mode);
        }

        @Test
        @DisplayName("WEIGHTED_RANDOM mode is parseable from string")
        void weightedRandomFromString() {
            ObjectiveSelectionConfig.ObjectiveSelectionMode mode =
                    ObjectiveSelectionConfig.ObjectiveSelectionMode.valueOf("WEIGHTED_RANDOM");
            assertEquals(ObjectiveSelectionConfig.ObjectiveSelectionMode.WEIGHTED_RANDOM, mode);
        }

        @Test
        @DisplayName("unknown mode string throws IllegalArgumentException")
        void unknownModeThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    ObjectiveSelectionConfig.ObjectiveSelectionMode.valueOf("INVALID_MODE"));
        }
    }
}
