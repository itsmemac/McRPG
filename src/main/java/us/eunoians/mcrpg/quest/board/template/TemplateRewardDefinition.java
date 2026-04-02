package us.eunoians.mcrpg.quest.board.template;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A reward definition within a {@link us.eunoians.mcrpg.quest.board.template.QuestTemplate}.
 * <p>
 * Reward config values may contain expression strings referencing template variables
 * (e.g., {@code "block_count * 5 * difficulty"}). These are resolved via the
 * existing {@code Parser} system at generation time.
 *
 * @param typeKey the reward type key (e.g., {@code mcrpg:experience})
 * @param label   the reward's YAML section key (e.g., {@code "champion_title"}); used to derive
 *                the auto-generated localization route at generation time
 * @param config  raw config map, may contain expression strings with variable references
 */
public record TemplateRewardDefinition(
        @NotNull NamespacedKey typeKey,
        @NotNull String label,
        @NotNull Map<String, Object> config
) {

    /** Canonical constructor — makes {@code config} immutable. */
    public TemplateRewardDefinition {
        config = Map.copyOf(config);
    }
}
