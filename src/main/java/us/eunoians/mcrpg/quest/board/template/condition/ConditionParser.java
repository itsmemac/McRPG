package us.eunoians.mcrpg.quest.board.template.condition;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.eunoians.mcrpg.util.McRPGMethods;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses {@link TemplateCondition} instances from YAML sections.
 * Supports both shorthand syntax for built-in conditions and explicit {@code type:}
 * key syntax for any registered condition type.
 * <p>
 * An instance of this class is created with an injected {@link TemplateConditionRegistry}
 * so that explicit-type lookups do not depend on the global {@code RegistryAccess} singleton.
 * The instance is also passed into {@link TemplateCondition#fromConfig} so that compound
 * or recursive conditions can delegate sub-condition parsing back through the same parser.
 */
public final class ConditionParser {

    private final TemplateConditionRegistry conditionRegistry;

    /**
     * Creates a new {@code ConditionParser} backed by the given registry.
     *
     * @param conditionRegistry the registry used to resolve explicit {@code type:} condition keys
     */
    public ConditionParser(@NotNull TemplateConditionRegistry conditionRegistry) {
        this.conditionRegistry = conditionRegistry;
    }

    /**
     * Parses a condition from a section containing a {@code condition:} block.
     * Returns null if no condition block is present.
     *
     * @param parent the YAML section that may contain a nested {@code condition:} key
     * @return the parsed condition, or {@code null} if none is present
     */
    @Nullable
    public TemplateCondition parseConditionBlock(@NotNull Section parent) {
        if (!parent.contains("condition")) {
            return null;
        }
        Section condSection = parent.getSection("condition");
        return parseSingle(condSection);
    }

    /**
     * Parses a prerequisite from a section containing a {@code prerequisite:} block.
     * Returns null if no prerequisite block is present.
     *
     * @param parent the YAML section that may contain a nested {@code prerequisite:} key
     * @return the parsed prerequisite condition, or {@code null} if none is present
     */
    @Nullable
    public TemplateCondition parsePrerequisiteBlock(@NotNull Section parent) {
        if (!parent.contains("prerequisite")) {
            return null;
        }
        Section prereqSection = parent.getSection("prerequisite");
        return parseSingle(prereqSection);
    }

    /**
     * Parses a single condition from a YAML section. Tries shorthand keys first,
     * then falls back to explicit {@code type:} key resolution.
     *
     * @param section the YAML section describing a single condition
     * @return the parsed condition
     * @throws IllegalArgumentException if the section does not match any recognized condition format
     */
    @NotNull
    public TemplateCondition parseSingle(@NotNull Section section) {
        // Compound conditions
        if (section.contains("all")) {
            return parseCompound(section, CompoundCondition.LogicMode.ALL);
        }
        if (section.contains("any")) {
            return parseCompound(section, CompoundCondition.LogicMode.ANY);
        }

        // Shorthand: rarity-at-least
        if (section.contains("rarity-at-least")) {
            NamespacedKey rarityKey = McRPGMethods.parseNamespacedKey(section.getString("rarity-at-least"));
            return new RarityCondition(rarityKey);
        }

        // Shorthand: chance
        if (section.contains("chance")) {
            return new ChanceCondition(section.getDouble("chance"));
        }

        // Shorthand: variable check
        if (section.contains("variable")) {
            Section varSection = section.getSection("variable");
            return parseVariableCondition(varSection);
        }

        // Shorthand: permission
        if (section.contains("permission")) {
            return new PermissionCondition(section.getString("permission"));
        }

        // Shorthand: min-completions (completion prerequisite)
        if (section.contains("min-completions")) {
            return parseCompletionPrerequisite(section);
        }

        // Explicit type: key
        if (section.contains("type")) {
            return parseExplicitType(section);
        }

        throw new IllegalArgumentException("Unrecognized condition format in section: " + section.getRouteAsString());
    }

    @NotNull
    private TemplateCondition parseCompound(@NotNull Section section, @NotNull CompoundCondition.LogicMode mode) {
        String key = mode == CompoundCondition.LogicMode.ALL ? "all" : "any";
        Section children = section.getSection(key);
        Map<String, TemplateCondition> parsed = new LinkedHashMap<>();
        for (String label : children.getRoutesAsStrings(false)) {
            parsed.put(label, parseSingle(children.getSection(label)));
        }
        return new CompoundCondition(parsed, mode);
    }

    @NotNull
    private VariableCondition parseVariableCondition(@NotNull Section section) {
        String name = section.getString("name");
        VariableCheck check = parseVariableCheck(section);
        return new VariableCondition(name, check);
    }

    /**
     * Parses a {@link VariableCheck} from a variable condition section.
     * Supports {@code contains-any}, {@code greater-than}, {@code less-than},
     * {@code at-least}, and {@code at-most} shorthand keys.
     * <p>
     * Static because it is a pure function with no dependency on registry state.
     *
     * @param section the YAML section containing the check definition
     * @return the parsed check
     * @throws IllegalArgumentException if no recognized check key is present
     */
    @NotNull
    static VariableCheck parseVariableCheck(@NotNull Section section) {
        if (section.contains("contains-any")) {
            return new VariableCheck.ContainsAny(section.getStringList("contains-any"));
        }
        if (section.contains("greater-than")) {
            return new VariableCheck.NumericComparison(ComparisonOperator.GREATER_THAN,
                    section.getDouble("greater-than"));
        }
        if (section.contains("less-than")) {
            return new VariableCheck.NumericComparison(ComparisonOperator.LESS_THAN,
                    section.getDouble("less-than"));
        }
        if (section.contains("at-least")) {
            return new VariableCheck.NumericComparison(ComparisonOperator.GREATER_THAN_OR_EQUAL,
                    section.getDouble("at-least"));
        }
        if (section.contains("at-most")) {
            return new VariableCheck.NumericComparison(ComparisonOperator.LESS_THAN_OR_EQUAL,
                    section.getDouble("at-most"));
        }
        throw new IllegalArgumentException("Variable condition has no recognized check at: " + section.getRouteAsString());
    }

    @NotNull
    private CompletionPrerequisiteCondition parseCompletionPrerequisite(@NotNull Section section) {
        int count = section.getInt("min-completions");
        NamespacedKey category = section.contains("category")
                ? McRPGMethods.parseNamespacedKey(section.getString("category"))
                : null;
        NamespacedKey rarity = section.contains("min-rarity")
                ? McRPGMethods.parseNamespacedKey(section.getString("min-rarity"))
                : null;
        return new CompletionPrerequisiteCondition(count, category, rarity);
    }

    @NotNull
    private TemplateCondition parseExplicitType(@NotNull Section section) {
        String typeStr = section.getString("type");
        NamespacedKey typeKey = NamespacedKey.fromString(typeStr.toLowerCase());
        if (typeKey == null) {
            throw new IllegalArgumentException("Invalid condition type key: " + typeStr);
        }
        TemplateCondition registered = conditionRegistry
                .get(typeKey)
                .orElseThrow(() -> new IllegalArgumentException("Unregistered condition type: " + typeKey));
        return registered.fromConfig(section, this);
    }

}
