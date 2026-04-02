package us.eunoians.mcrpg.quest.board.template.condition;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.eunoians.mcrpg.expansion.McRPGExpansion;
import us.eunoians.mcrpg.util.McRPGMethods;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link TemplateCondition} that gates a template element behind a quest completion
 * history threshold. The condition passes when the player has completed at least
 * {@code minCompletions} quests, with optional filters to count only completions from a
 * specific board category and/or at or above a minimum rarity. This enables progressive
 * content unlocks — for example, showing advanced weekly quests only to players who have
 * already completed a certain number of dailies.
 *
 * <p><b>Player-dependent:</b> this condition requires both a player UUID and a
 * {@link QuestCompletionHistory} in the {@link ConditionContext}. It returns {@code false}
 * when either is absent (e.g. during shared offering generation via
 * {@link ConditionContext#forTemplateGeneration}). Use this condition only in templates
 * targeting personal offerings where history is available via
 * {@link ConditionContext#forPersonalGeneration}.
 *
 * <p><b>YAML — shorthand (recommended):</b>
 * <pre>{@code
 * condition:
 *   min-completions: 10                    # any quest, any rarity
 * }</pre>
 *
 * <pre>{@code
 * condition:
 *   min-completions: 5
 *   category: mcrpg:personal_daily         # only personal-daily completions count
 *   min-rarity: mcrpg:rare                 # only RARE or rarer completions count
 * }</pre>
 *
 * <p><b>YAML — explicit type:</b>
 * <pre>{@code
 * condition:
 *   type: mcrpg:completion_prerequisite
 *   min-completions: 3
 *   category: mcrpg:personal_weekly
 * }</pre>
 *
 * <p>Category and rarity keys support both fully-qualified form ({@code mcrpg:personal_daily})
 * and bare form ({@code personal_daily}), which is auto-namespaced under {@code mcrpg:}.
 * Omitting {@code category} counts completions across all categories; omitting {@code min-rarity}
 * counts completions at all rarities.
 *
 * @see QuestCompletionHistory
 * @see TemplateCondition
 * @see ConditionContext
 */
public final class CompletionPrerequisiteCondition implements TemplateCondition {

    public static final NamespacedKey KEY = new NamespacedKey(McRPGMethods.getMcRPGNamespace(), "completion_prerequisite");

    private final int minCompletions;
    @Nullable
    private final NamespacedKey categoryKey;
    @Nullable
    private final NamespacedKey minRarity;

    /**
     * Creates an unconfigured prototype instance for registry registration.
     * The stored values are placeholders; this instance is never evaluated.
     * Use {@link #fromConfig} to produce a live, configured condition.
     */
    public CompletionPrerequisiteCondition() {
        this.minCompletions = 1;
        this.categoryKey = null;
        this.minRarity = null;
    }

    /**
     * Creates a new completion prerequisite condition.
     *
     * @param minCompletions the minimum number of completed quests required; must be {@code >= 1}
     * @param categoryKey    if non-null, only completions from this board category count
     * @param minRarity      if non-null, only completions at or above this rarity count
     * @throws IllegalArgumentException if {@code minCompletions < 1}
     */
    public CompletionPrerequisiteCondition(int minCompletions,
                                           @Nullable NamespacedKey categoryKey,
                                           @Nullable NamespacedKey minRarity) {
        if (minCompletions < 1) {
            throw new IllegalArgumentException("minCompletions must be >= 1, got: " + minCompletions);
        }
        this.minCompletions = minCompletions;
        this.categoryKey = categoryKey;
        this.minRarity = minRarity;
    }

    @Override
    public boolean evaluate(@NotNull ConditionContext context) {
        if (context.playerUUID() == null || context.completionHistory() == null) {
            return false;
        }
        int completed = context.completionHistory().countCompletedQuests(
                context.playerUUID(), categoryKey, minRarity);
        return completed >= minCompletions;
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
    @Override
    public Map<String, Object> serializeConfig() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("min-completions", minCompletions);
        if (categoryKey != null) {
            map.put("category", categoryKey.toString());
        }
        if (minRarity != null) {
            map.put("min-rarity", minRarity.toString());
        }
        return map;
    }

    /**
     * Returns the minimum number of quest completions the player must have.
     *
     * @return the minimum completion count
     */
    public int getMinCompletions() {
        return minCompletions;
    }

    /**
     * Returns the optional board category filter applied to completion counting.
     *
     * @return the category key, or empty if all categories count
     */
    @NotNull
    public Optional<NamespacedKey> getCategoryKey() {
        return Optional.ofNullable(categoryKey);
    }

    /**
     * Returns the optional minimum rarity filter applied to completion counting.
     *
     * @return the minimum rarity key, or empty if all rarities count
     */
    @NotNull
    public Optional<NamespacedKey> getMinRarity() {
        return Optional.ofNullable(minRarity);
    }
}
