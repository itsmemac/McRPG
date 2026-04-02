package us.eunoians.mcrpg.quest.board.template.condition;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.expansion.McRPGExpansion;
import us.eunoians.mcrpg.util.McRPGMethods;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link TemplateCondition} that combines multiple child conditions using
 * {@link LogicMode#ALL} (AND) or {@link LogicMode#ANY} (OR) logic. This allows complex
 * gating that no single condition type can express on its own — for example, requiring
 * both a minimum rarity <em>and</em> a player permission, or passing when <em>either</em>
 * of two independent chance rolls succeeds.
 *
 * <p>Each child condition is assigned an arbitrary human-readable label (the YAML key) that
 * has no semantic effect on evaluation — labels exist only to make the config readable.
 * Child conditions can themselves be compound, enabling arbitrary nesting depth.
 *
 * <p><b>Context requirements:</b> delegates fully to its children. If a child is
 * player-dependent (e.g. {@link PermissionCondition}), this condition is also player-dependent.
 * See each child condition's documentation for its individual context requirements.
 *
 * <p><b>YAML — {@code ALL} (AND) mode:</b>
 * <pre>{@code
 * condition:
 *   all:
 *     rarity-check:
 *       rarity-at-least: RARE
 *     permission-check:
 *       permission: mcrpg.quest.veteran
 * }</pre>
 *
 * <p><b>YAML — {@code ANY} (OR) mode:</b>
 * <pre>{@code
 * condition:
 *   any:
 *     lucky-roll:
 *       chance: 0.25
 *     vip-override:
 *       permission: mcrpg.quest.vip
 * }</pre>
 *
 * <p><b>YAML — explicit type (for use nested inside another compound):</b>
 * <pre>{@code
 * type: mcrpg:compound
 * all:
 *   ...
 * }</pre>
 *
 * <p>Note: the {@code all:} or {@code any:} shorthand takes priority over the explicit
 * {@code type:} key in {@link ConditionParser#parseSingle}, so the {@code type:} key is
 * only needed when a compound condition appears as a child of another compound condition
 * and is serialized via {@link #serializeConfig}.
 *
 * @see LogicMode
 * @see TemplateCondition
 * @see ConditionContext
 */
public final class CompoundCondition implements TemplateCondition {

    public static final NamespacedKey KEY = new NamespacedKey(McRPGMethods.getMcRPGNamespace(), "compound");

    private final Map<String, TemplateCondition> conditions;
    private final LogicMode mode;

    /** Determines whether all children must pass ({@code ALL}) or at least one ({@code ANY}). */
    public enum LogicMode { ALL, ANY }

    /**
     * Creates an unconfigured prototype instance for registry registration.
     * The stored conditions map is empty and mode is a placeholder; this instance is never evaluated.
     * Use {@link #fromConfig} to produce a live, configured condition.
     */
    public CompoundCondition() {
        this.conditions = Map.of();
        this.mode = LogicMode.ALL;
    }

    /**
     * Creates a new compound condition.
     *
     * @param conditions a non-empty map of human-readable label to child condition
     * @param mode       {@link LogicMode#ALL} for AND logic; {@link LogicMode#ANY} for OR logic
     * @throws IllegalArgumentException if {@code conditions} is empty
     */
    public CompoundCondition(@NotNull Map<String, TemplateCondition> conditions, @NotNull LogicMode mode) {
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("CompoundCondition requires at least one child condition");
        }
        this.conditions = Map.copyOf(conditions);
        this.mode = mode;
    }

    @Override
    public boolean evaluate(@NotNull ConditionContext context) {
        return switch (mode) {
            case ALL -> conditions.values().stream().allMatch(c -> c.evaluate(context));
            case ANY -> conditions.values().stream().anyMatch(c -> c.evaluate(context));
        };
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
        LogicMode parsedMode = section.contains("all") ? LogicMode.ALL : LogicMode.ANY;
        String key = parsedMode == LogicMode.ALL ? "all" : "any";
        Section children = section.getSection(key);
        Map<String, TemplateCondition> parsed = new LinkedHashMap<>();
        for (String label : children.getRoutesAsStrings(false)) {
            parsed.put(label, parser.parseSingle(children.getSection(label)));
        }
        return new CompoundCondition(parsed, parsedMode);
    }

    /**
     * Returns the child conditions keyed by their human-readable labels.
     *
     * @return an unmodifiable map of label to condition
     */
    @NotNull
    public Map<String, TemplateCondition> getConditions() {
        return conditions;
    }

    @NotNull
    @Override
    public Map<String, Object> serializeConfig() {
        String modeKey = mode == LogicMode.ALL ? "all" : "any";
        Map<String, Object> children = new LinkedHashMap<>();
        for (Map.Entry<String, TemplateCondition> entry : conditions.entrySet()) {
            // Inject "type" first so ConditionParser can dispatch third-party conditions via the
            // explicit-type path. Built-in conditions are also matched by their shorthand keys,
            // which take priority in parseSingle — the extra "type" entry is harmless.
            Map<String, Object> childMap = new LinkedHashMap<>();
            childMap.put("type", entry.getValue().getKey().toString());
            childMap.putAll(entry.getValue().serializeConfig());
            children.put(entry.getKey(), childMap);
        }
        return Map.of(modeKey, children);
    }

    /**
     * Returns the logic mode that governs how child conditions are combined.
     *
     * @return {@link LogicMode#ALL} for AND, {@link LogicMode#ANY} for OR
     */
    @NotNull
    public LogicMode getMode() {
        return mode;
    }
}
