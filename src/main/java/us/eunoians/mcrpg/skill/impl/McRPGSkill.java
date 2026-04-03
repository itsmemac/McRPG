package us.eunoians.mcrpg.skill.impl;

import com.diamonddagger590.mccore.statistic.SimpleStatistic;
import com.diamonddagger590.mccore.statistic.Statistic;
import com.diamonddagger590.mccore.statistic.StatisticType;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.expansion.McRPGExpansion;
import us.eunoians.mcrpg.skill.BaseSkill;

import java.util.Optional;
import java.util.Set;

/**
 * A type of skill that is used by native McRPG skills.
 */
public abstract class McRPGSkill extends BaseSkill {

    public McRPGSkill(@NotNull NamespacedKey skillKey) {
        super(skillKey);
    }

    @NotNull
    @Override
    public Optional<NamespacedKey> getExpansionKey() {
        return Optional.of(McRPGExpansion.EXPANSION_KEY);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Native McRPG skills provide per-skill experience and max level statistics by default.
     * Concrete skills can override to add additional custom statistics alongside the defaults.
     */
    @Override
    @NotNull
    public Set<Statistic> getDefaultStatistics() {
        return Set.of(
                new SimpleStatistic(getExperienceStatisticKey(), StatisticType.LONG, 0L,
                        getName() + " Experience", "Total " + getName() + " XP earned"),
                new SimpleStatistic(getMaxLevelStatisticKey(), StatisticType.INT, 0,
                        getName() + " Max Level", "Highest " + getName() + " level reached")
        );
    }
}
