package us.eunoians.mcrpg.ability.impl.type;

import com.diamonddagger590.mccore.statistic.SimpleStatistic;
import com.diamonddagger590.mccore.statistic.Statistic;
import com.diamonddagger590.mccore.statistic.StatisticType;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.ability.Ability;

import java.util.Set;

/**
 * An interface that represents an {@link Ability} that requires some sort of
 * action by an {@link us.eunoians.mcrpg.entity.holder.AbilityHolder} in order to activate.
 */
public interface ActiveAbility extends Ability {

    @Override
    default boolean isPassive() {
        return false;
    }

    /**
     * Gets the {@link NamespacedKey} for the activation count {@link Statistic} tracked for this ability.
     * <p>
     * The key follows the convention {@code <namespace>:<ability_key>_activations}, e.g.
     * {@code mcrpg:bleed_activations}.
     *
     * @return The {@link NamespacedKey} for this ability's activation count statistic.
     */
    @NotNull
    default NamespacedKey getActivationStatisticKey() {
        return new NamespacedKey(getPlugin(), getAbilityKey().getKey() + "_activations");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Active abilities provide a default activation count statistic. Concrete abilities
     * can override this to add additional custom statistics alongside the default.
     */
    @Override
    @NotNull
    default Set<Statistic> getDefaultStatistics() {
        return Set.of(new SimpleStatistic(
                getActivationStatisticKey(),
                StatisticType.LONG,
                0L,
                getName() + " Activations",
                "Times " + getName() + " has been activated"
        ));
    }
}
