package us.eunoians.mcrpg.quest.board.template.condition;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.expansion.McRPGExpansion;
import us.eunoians.mcrpg.quest.board.rarity.QuestRarity;
import us.eunoians.mcrpg.util.McRPGMethods;

import java.util.Map;
import java.util.Optional;

/**
 * A {@link TemplateCondition} that gates a template element behind a minimum rarity tier.
 * The condition passes when the rolled rarity is <em>at least as rare</em> as the configured
 * minimum. Rarity is compared by weight: a lower weight value means a rarer tier, so the
 * condition evaluates as {@code rolledWeight <= minimumWeight}.
 *
 * <p><b>Example:</b> setting {@code min-rarity: RARE} will include the element only when the
 * board generates a RARE, EPIC, or LEGENDARY quest — not COMMON or UNCOMMON.
 *
 * <p><b>Context requirements:</b> requires both {@code rolledRarity} and {@code rarityRegistry}
 * to be present in the {@link ConditionContext}. If either is absent the condition returns
 * {@code true} (pass-through), so elements are not inadvertently hidden in contexts that have
 * no rarity information (e.g. manual quest sources).
 *
 * <p><b>YAML — shorthand (recommended):</b>
 * <pre>{@code
 * condition:
 *   rarity-at-least: RARE          # bare key: auto-namespaced to mcrpg:rare
 *   # or fully-qualified:
 *   rarity-at-least: mcrpg:rare
 * }</pre>
 *
 * <p><b>YAML — explicit type:</b>
 * <pre>{@code
 * condition:
 *   type: mcrpg:rarity_gate
 *   min-rarity: mcrpg:rare
 * }</pre>
 *
 * <p>This condition is context-independent with respect to the player — it only depends on
 * the rolled rarity, so it works equally for both shared and personal offering generation.
 * Use {@link ConditionContext#forTemplateGeneration} or {@link ConditionContext#forPersonalGeneration}
 * as appropriate.
 *
 * @see TemplateCondition
 * @see ConditionContext
 * @see us.eunoians.mcrpg.quest.board.rarity.QuestRarity
 */
public final class RarityCondition implements TemplateCondition {

    public static final NamespacedKey KEY = new NamespacedKey(McRPGMethods.getMcRPGNamespace(), "rarity_gate");

    private final NamespacedKey minimumRarity;

    /**
     * Creates an unconfigured prototype instance for registry registration.
     * The stored rarity key is a placeholder; this instance is never evaluated.
     * Use {@link #fromConfig} to produce a live, configured condition.
     */
    public RarityCondition() {
        this.minimumRarity = KEY;
    }

    /**
     * Creates a new rarity gate condition.
     *
     * @param minimumRarity the key of the least-rare rarity that must be rolled to pass
     *                      (lower weight = rarer, so this is the weight ceiling)
     */
    public RarityCondition(@NotNull NamespacedKey minimumRarity) {
        this.minimumRarity = minimumRarity;
    }

    @Override
    public boolean evaluate(@NotNull ConditionContext context) {
        if (context.rolledRarity() == null || context.rarityRegistry() == null) {
            return true;
        }
        Optional<QuestRarity> rolled = context.rarityRegistry().get(context.rolledRarity());
        Optional<QuestRarity> minimum = context.rarityRegistry().get(minimumRarity);
        if (rolled.isEmpty() || minimum.isEmpty()) {
            return false;
        }
        return rolled.get().getWeight() <= minimum.get().getWeight();
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
        String rarityStr = section.getString("min-rarity");
        if (rarityStr == null || rarityStr.isBlank()) {
            throw new IllegalArgumentException("Missing 'min-rarity' in rarity_gate condition");
        }
        NamespacedKey rarityKey = McRPGMethods.parseNamespacedKey(rarityStr);
        if (rarityKey == null) {
            throw new IllegalArgumentException("Invalid rarity key: " + rarityStr);
        }
        return new RarityCondition(rarityKey);
    }

    @NotNull
    @Override
    public Map<String, Object> serializeConfig() {
        return Map.of("min-rarity", minimumRarity.toString());
    }

    /**
     * Returns the key of the minimum rarity required for this condition to pass.
     *
     * @return the minimum rarity key
     */
    @NotNull
    public NamespacedKey getMinimumRarity() {
        return minimumRarity;
    }
}
