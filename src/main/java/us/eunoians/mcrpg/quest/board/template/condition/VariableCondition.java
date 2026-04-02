package us.eunoians.mcrpg.quest.board.template.condition;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.expansion.McRPGExpansion;
import us.eunoians.mcrpg.util.McRPGMethods;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link TemplateCondition} that evaluates a resolved template variable against a
 * {@link VariableCheck}. This enables data-driven template branching: phases, stages, and
 * objectives can be conditionally included based on what the variable engine rolled earlier
 * in the generation pipeline.
 *
 * <p>The variable must have been declared in the template's {@code variables:} section and
 * resolved before condition evaluation. If the named variable does not exist in the resolved
 * context the condition returns {@code false} — the element is excluded. If the entire
 * {@code resolvedVariables} context is absent (e.g. not a template generation context) the
 * condition returns {@code true} (pass-through).
 *
 * <p><b>Supported check types</b> (configured via the {@code variable:} shorthand sub-keys):
 * <ul>
 *   <li>{@code contains-any: [VAL1, VAL2]} — passes if the variable's list value contains any of the given strings</li>
 *   <li>{@code greater-than: N} — passes if the variable's numeric value is {@code > N}</li>
 *   <li>{@code less-than: N} — passes if the variable's numeric value is {@code < N}</li>
 *   <li>{@code at-least: N} — passes if the variable's numeric value is {@code >= N}</li>
 *   <li>{@code at-most: N} — passes if the variable's numeric value is {@code <= N}</li>
 * </ul>
 *
 * <p><b>YAML — shorthand (recommended):</b>
 * <pre>{@code
 * condition:
 *   variable:
 *     name: target_blocks
 *     contains-any:
 *       - DIAMOND_ORE
 *       - EMERALD_ORE
 * }</pre>
 *
 * <pre>{@code
 * condition:
 *   variable:
 *     name: difficulty
 *     at-least: 2.0
 * }</pre>
 *
 * <p><b>YAML — explicit type:</b>
 * <pre>{@code
 * condition:
 *   type: mcrpg:variable_check
 *   name: difficulty
 *   greater-than: 1.5
 * }</pre>
 *
 * <p>This condition depends only on resolved variables, not on the player, so it works for
 * both shared and personal offering generation.
 *
 * @see VariableCheck
 * @see TemplateCondition
 * @see ConditionContext
 */
public final class VariableCondition implements TemplateCondition {

    public static final NamespacedKey KEY = new NamespacedKey(McRPGMethods.getMcRPGNamespace(), "variable_check");

    private final String variableName;
    private final VariableCheck check;

    /**
     * Creates an unconfigured prototype instance for registry registration.
     * The stored variable name and check are placeholders; this instance is never evaluated.
     * Use {@link #fromConfig} to produce a live, configured condition.
     */
    public VariableCondition() {
        this.variableName = "";
        this.check = new VariableCheck.ContainsAny(List.of());
    }

    /**
     * Creates a new variable condition.
     *
     * @param variableName the name of the resolved template variable to test
     * @param check        the check to apply to the variable's resolved value
     */
    public VariableCondition(@NotNull String variableName, @NotNull VariableCheck check) {
        this.variableName = variableName;
        this.check = check;
    }

    @Override
    public boolean evaluate(@NotNull ConditionContext context) {
        if (context.resolvedVariables() == null) {
            return true;
        }
        Object value = context.resolvedVariables().resolvedValues().get(variableName);
        if (value == null) {
            return false;
        }
        return check.test(value);
    }

    @NotNull
    @Override
    public NamespacedKey getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public Optional<NamespacedKey> getExpansionKey() {
        return Optional.of(McRPGExpansion.EXPANSION_KEY);
    }

    @NotNull
    @Override
    public TemplateCondition fromConfig(@NotNull Section section, @NotNull ConditionParser parser) {
        String name = section.getString("name");
        VariableCheck parsedCheck = ConditionParser.parseVariableCheck(section);
        return new VariableCondition(name, parsedCheck);
    }

    /**
     * Returns the name of the template variable this condition tests.
     *
     * @return the variable name as used in the template YAML
     */
    @NotNull
    public String getVariableName() {
        return variableName;
    }

    @NotNull
    @Override
    public Map<String, Object> serializeConfig() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", variableName);
        switch (check) {
            case VariableCheck.ContainsAny ca -> map.put("contains-any", ca.values());
            case VariableCheck.NumericComparison nc ->
                    map.put(numericCheckKey(nc.operator()), nc.threshold());
        }
        return map;
    }

    /**
     * Returns the check that is applied to the variable's resolved value.
     *
     * @return the {@link VariableCheck} instance
     */
    @NotNull
    public VariableCheck getCheck() {
        return check;
    }

    @NotNull
    private static String numericCheckKey(@NotNull ComparisonOperator operator) {
        return switch (operator) {
            case GREATER_THAN -> "greater-than";
            case LESS_THAN -> "less-than";
            case GREATER_THAN_OR_EQUAL -> "at-least";
            case LESS_THAN_OR_EQUAL -> "at-most";
        };
    }
}
