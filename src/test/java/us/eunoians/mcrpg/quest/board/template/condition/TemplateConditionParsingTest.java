package us.eunoians.mcrpg.quest.board.template.condition;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ConditionParser} shorthand and explicit-type parsing.
 * Registry-dependent explicit-type parsing is tested via {@link TemplateConditionRegistryTest}.
 */
class TemplateConditionParsingTest {

    private final ConditionParser parser = new ConditionParser(new TemplateConditionRegistry());

    private static Section sectionFrom(String yaml) {
        try {
            return YamlDocument.create(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse test YAML", e);
        }
    }

    @Nested
    @DisplayName("parseConditionBlock — no condition block")
    class NoConditionBlock {

        @Test
        @DisplayName("missing condition block returns null (unconditional)")
        void missingConditionBlockReturnsNull() throws IOException {
            YamlDocument doc = YamlDocument.create(new ByteArrayInputStream(
                    "other-key: value\n".getBytes(StandardCharsets.UTF_8)));
            assertNull(parser.parseConditionBlock(doc));
        }
    }

    @Nested
    @DisplayName("Shorthand: rarity-at-least")
    class RarityShorthand {

        @Test
        @DisplayName("parses rarity-at-least shorthand into RarityCondition")
        void parsesRarityShorthand() {
            Section section = sectionFrom("rarity-at-least: RARE\n");
            TemplateCondition condition = parser.parseSingle(section);
            assertInstanceOf(RarityCondition.class, condition);
            RarityCondition rarityCondition = (RarityCondition) condition;
            assertEquals(NamespacedKey.fromString("mcrpg:rare"), rarityCondition.getMinimumRarity());
        }

        @Test
        @DisplayName("rarity key without namespace defaults to mcrpg namespace")
        void rarityKeyWithoutNamespace() {
            Section section = sectionFrom("rarity-at-least: legendary\n");
            RarityCondition condition = (RarityCondition) parser.parseSingle(section);
            assertEquals("mcrpg", condition.getMinimumRarity().namespace());
            assertEquals("legendary", condition.getMinimumRarity().value());
        }
    }

    @Nested
    @DisplayName("Shorthand: chance")
    class ChanceShorthand {

        @Test
        @DisplayName("parses chance shorthand into ChanceCondition")
        void parsesChanceShorthand() {
            Section section = sectionFrom("chance: 0.3\n");
            TemplateCondition condition = parser.parseSingle(section);
            assertInstanceOf(ChanceCondition.class, condition);
            ChanceCondition chanceCondition = (ChanceCondition) condition;
            assertEquals(0.3, chanceCondition.getChance(), 1e-9);
        }

        @Test
        @DisplayName("parses chance of 1.0")
        void parsesChanceOne() {
            Section section = sectionFrom("chance: 1.0\n");
            ChanceCondition condition = (ChanceCondition) parser.parseSingle(section);
            assertEquals(1.0, condition.getChance(), 1e-9);
        }
    }

    @Nested
    @DisplayName("Shorthand: variable check")
    class VariableShorthand {

        @Test
        @DisplayName("parses variable contains-any into VariableCondition with ContainsAny")
        void parsesVariableContainsAny() {
            String yaml = "variable:\n  name: target_blocks\n  contains-any:\n    - DIAMOND_ORE\n    - EMERALD_ORE\n";
            Section section = sectionFrom(yaml);
            TemplateCondition condition = parser.parseSingle(section);
            assertInstanceOf(VariableCondition.class, condition);
            VariableCondition variableCondition = (VariableCondition) condition;
            assertEquals("target_blocks", variableCondition.getVariableName());
            assertInstanceOf(VariableCheck.ContainsAny.class, variableCondition.getCheck());
            VariableCheck.ContainsAny containsAny = (VariableCheck.ContainsAny) variableCondition.getCheck();
            assertEquals(List.of("DIAMOND_ORE", "EMERALD_ORE"), containsAny.values());
        }

        @Test
        @DisplayName("parses variable greater-than into VariableCondition with NumericComparison")
        void parsesVariableGreaterThan() {
            String yaml = "variable:\n  name: block_count\n  greater-than: 50.0\n";
            Section section = sectionFrom(yaml);
            VariableCondition condition = (VariableCondition) parser.parseSingle(section);
            assertInstanceOf(VariableCheck.NumericComparison.class, condition.getCheck());
            VariableCheck.NumericComparison comparison = (VariableCheck.NumericComparison) condition.getCheck();
            assertEquals(ComparisonOperator.GREATER_THAN, comparison.operator());
            assertEquals(50.0, comparison.threshold(), 1e-9);
        }

        @Test
        @DisplayName("parses variable at-least into GREATER_THAN_OR_EQUAL comparison")
        void parsesVariableAtLeast() {
            String yaml = "variable:\n  name: difficulty\n  at-least: 2.5\n";
            Section section = sectionFrom(yaml);
            VariableCondition condition = (VariableCondition) parser.parseSingle(section);
            VariableCheck.NumericComparison comparison = (VariableCheck.NumericComparison) condition.getCheck();
            assertEquals(ComparisonOperator.GREATER_THAN_OR_EQUAL, comparison.operator());
        }

        @Test
        @DisplayName("parses variable at-most into LESS_THAN_OR_EQUAL comparison")
        void parsesVariableAtMost() {
            String yaml = "variable:\n  name: difficulty\n  at-most: 5.0\n";
            Section section = sectionFrom(yaml);
            VariableCondition condition = (VariableCondition) parser.parseSingle(section);
            VariableCheck.NumericComparison comparison = (VariableCheck.NumericComparison) condition.getCheck();
            assertEquals(ComparisonOperator.LESS_THAN_OR_EQUAL, comparison.operator());
        }
    }

    @Nested
    @DisplayName("Shorthand: permission")
    class PermissionShorthand {

        @Test
        @DisplayName("parses permission shorthand into PermissionCondition")
        void parsesPermissionShorthand() {
            Section section = sectionFrom("permission: mcrpg.title.hero\n");
            TemplateCondition condition = parser.parseSingle(section);
            assertInstanceOf(PermissionCondition.class, condition);
            assertEquals("mcrpg.title.hero", ((PermissionCondition) condition).getPermission());
        }
    }

    @Nested
    @DisplayName("Shorthand: min-completions")
    class CompletionPrerequisiteShorthand {

        @Test
        @DisplayName("parses min-completions into CompletionPrerequisiteCondition")
        void parsesMinCompletions() {
            Section section = sectionFrom("min-completions: 5\n");
            TemplateCondition condition = parser.parseSingle(section);
            assertInstanceOf(CompletionPrerequisiteCondition.class, condition);
            assertEquals(5, ((CompletionPrerequisiteCondition) condition).getMinCompletions());
        }

        @Test
        @DisplayName("parses min-completions with category filter")
        void parsesWithCategoryFilter() {
            String yaml = "min-completions: 10\ncategory: mcrpg:personal_daily\n";
            Section section = sectionFrom(yaml);
            CompletionPrerequisiteCondition condition =
                    (CompletionPrerequisiteCondition) parser.parseSingle(section);
            assertEquals(10, condition.getMinCompletions());
            assertEquals(NamespacedKey.fromString("mcrpg:personal_daily"),
                    condition.getCategoryKey().orElse(null));
        }

        @Test
        @DisplayName("parses min-completions with min-rarity filter")
        void parsesWithRarityFilter() {
            String yaml = "min-completions: 3\nmin-rarity: mcrpg:rare\n";
            Section section = sectionFrom(yaml);
            CompletionPrerequisiteCondition condition =
                    (CompletionPrerequisiteCondition) parser.parseSingle(section);
            assertEquals(3, condition.getMinCompletions());
            assertEquals(NamespacedKey.fromString("mcrpg:rare"),
                    condition.getMinRarity().orElse(null));
        }
    }

    @Nested
    @DisplayName("Compound conditions")
    class CompoundConditions {

        @Test
        @DisplayName("parses all: block into CompoundCondition with ALL mode")
        void parsesAllBlock() {
            String yaml = "all:\n  rarity-check:\n    rarity-at-least: RARE\n  chance-check:\n    chance: 0.5\n";
            Section section = sectionFrom(yaml);
            TemplateCondition condition = parser.parseSingle(section);
            assertInstanceOf(CompoundCondition.class, condition);
            CompoundCondition compound = (CompoundCondition) condition;
            assertEquals(CompoundCondition.LogicMode.ALL, compound.getMode());
            assertEquals(2, compound.getConditions().size());
        }

        @Test
        @DisplayName("parses any: block into CompoundCondition with ANY mode")
        void parsesAnyBlock() {
            String yaml = "any:\n  first:\n    chance: 0.3\n  second:\n    chance: 0.7\n";
            Section section = sectionFrom(yaml);
            CompoundCondition condition = (CompoundCondition) parser.parseSingle(section);
            assertEquals(CompoundCondition.LogicMode.ANY, condition.getMode());
        }

        @Test
        @DisplayName("compound children are parsed as correct types")
        void compoundChildrenParsedCorrectly() {
            String yaml = "all:\n  rarity-check:\n    rarity-at-least: COMMON\n  perm-check:\n    permission: mcrpg.vip\n";
            Section section = sectionFrom(yaml);
            CompoundCondition compound = (CompoundCondition) parser.parseSingle(section);
            assertNotNull(compound.getConditions().get("rarity-check"));
            assertNotNull(compound.getConditions().get("perm-check"));
            assertInstanceOf(RarityCondition.class, compound.getConditions().get("rarity-check"));
            assertInstanceOf(PermissionCondition.class, compound.getConditions().get("perm-check"));
        }
    }

    @Nested
    @DisplayName("Unrecognized condition format")
    class UnrecognizedFormat {

        @Test
        @DisplayName("section with no recognized key throws IllegalArgumentException")
        void unrecognizedFormatThrows() {
            Section section = sectionFrom("unknown-key: value\n");
            assertThrows(IllegalArgumentException.class, () -> parser.parseSingle(section));
        }
    }
}
