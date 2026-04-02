package us.eunoians.mcrpg.quest.board.template.condition;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.expansion.McRPGExpansion;
import us.eunoians.mcrpg.util.McRPGMethods;

import java.util.Map;
import java.util.Optional;

/**
 * A {@link TemplateCondition} that probabilistically includes a template element.
 * The condition evaluates to {@code true} with the configured probability on each generation
 * pass, producing optional content that does not always appear in the generated quest.
 *
 * <p>The probability is expressed as a decimal in {@code [0.0, 1.0]} where {@code 0.0} means
 * never included and {@code 1.0} means always included. The context's {@link java.util.Random}
 * instance is used so that templates seeded deterministically (e.g. via
 * {@code PersonalOfferingGenerator.computeSeed}) produce the same result for the same player
 * and rotation — a given player will see the same optional elements every time the board refreshes.
 *
 * <p><b>Context requirements:</b> requires {@code random} to be present in the
 * {@link ConditionContext}. If {@code random} is {@code null} the condition returns {@code true}
 * (pass-through), so elements are never hidden in non-generation contexts (e.g. reward grant
 * fallback evaluation).
 *
 * <p><b>YAML — shorthand (recommended):</b>
 * <pre>{@code
 * condition:
 *   chance: 0.5    # 50 % inclusion probability
 * }</pre>
 *
 * <p><b>YAML — explicit type:</b>
 * <pre>{@code
 * condition:
 *   type: mcrpg:chance
 *   chance: 0.75
 * }</pre>
 *
 * <p>This condition is context-independent with respect to the player — two different players
 * generated in the same rotation with the same seed will see the same result. It works for
 * both shared and personal offering generation.
 *
 * @see TemplateCondition
 * @see ConditionContext
 */
public final class ChanceCondition implements TemplateCondition {

    public static final NamespacedKey KEY = new NamespacedKey(McRPGMethods.getMcRPGNamespace(), "chance");

    private final double chance;

    /**
     * Creates an unconfigured prototype instance for registry registration.
     * The stored chance is a placeholder; this instance is never evaluated.
     * Use {@link #fromConfig} to produce a live, configured condition.
     */
    public ChanceCondition() {
        this.chance = 0.5;
    }

    /**
     * Creates a new chance condition.
     *
     * @param chance the probability threshold in the range {@code [0.0, 1.0]}; e.g. {@code 0.5} = 50 %
     * @throws IllegalArgumentException if {@code chance} is outside {@code [0.0, 1.0]}
     */
    public ChanceCondition(double chance) {
        if (chance < 0.0 || chance > 1.0) {
            throw new IllegalArgumentException("Chance must be between 0.0 and 1.0, got: " + chance);
        }
        this.chance = chance;
    }

    @Override
    public boolean evaluate(@NotNull ConditionContext context) {
        if (context.random() == null) {
            return true;
        }
        return context.random().nextDouble() < chance;
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
        return new ChanceCondition(section.getDouble("chance"));
    }

    @NotNull
    @Override
    public Map<String, Object> serializeConfig() {
        return Map.of("chance", chance);
    }

    /**
     * Returns the raw probability threshold for this condition.
     *
     * @return a value in {@code [0.0, 1.0]} representing the activation probability
     */
    public double getChance() {
        return chance;
    }
}
