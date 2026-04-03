package us.eunoians.mcrpg.listener.statistic;

import com.diamonddagger590.mccore.registry.RegistryKey;
import com.diamonddagger590.mccore.statistic.PlayerStatisticData;
import com.diamonddagger590.mccore.statistic.StatisticRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.event.skill.PostSkillGainLevelEvent;
import us.eunoians.mcrpg.event.skill.SkillGainExpEvent;
import us.eunoians.mcrpg.registry.McRPGRegistryKey;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;
import us.eunoians.mcrpg.skill.Skill;
import us.eunoians.mcrpg.skill.SkillRegistry;
import us.eunoians.mcrpg.skill.experience.context.McRPGGainReason;
import us.eunoians.mcrpg.skill.impl.herbalism.Herbalism;
import us.eunoians.mcrpg.skill.impl.mining.Mining;
import us.eunoians.mcrpg.skill.impl.woodcutting.WoodCutting;
import us.eunoians.mcrpg.statistic.McRPGStatistic;

import java.util.Optional;

/**
 * Listens to skill experience and level events to increment statistics.
 * <p>
 * Uses {@link EventPriority#MONITOR} to read final values after all other
 * listeners have modified or cancelled the event.
 * <p>
 * Per-skill statistics (experience and max level) are only incremented if the
 * corresponding statistic is registered. Native McRPG skills register their per-skill
 * statistics via {@link us.eunoians.mcrpg.skill.impl.McRPGSkill#getDefaultStatistics()}.
 * Third-party {@link us.eunoians.mcrpg.expansion.ContentExpansion} plugins that add custom
 * skills must register their own per-skill statistics to have them tracked here. The global
 * statistics ({@link McRPGStatistic#TOTAL_SKILL_EXPERIENCE} and
 * {@link McRPGStatistic#TOTAL_SKILL_LEVELS_GAINED}) are always incremented regardless
 * of skill origin.
 */
public class SkillStatisticListener implements Listener {

    /**
     * Tracks per-skill and total XP earned. Fires on the pre-event
     * so the XP amount is available (including overflow past max level).
     *
     * @param event The skill experience gain event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillGainExp(@NotNull SkillGainExpEvent event) {
        Optional<McRPGPlayer> playerOptional = McRPG.getInstance().registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.PLAYER)
                .getPlayer(event.getSkillHolder().getUUID());
        if (playerOptional.isEmpty()) {
            return;
        }

        PlayerStatisticData stats = playerOptional.get().getStatisticData();
        StatisticRegistry statisticRegistry = McRPG.getInstance().registryAccess().registry(RegistryKey.STATISTIC);
        SkillRegistry skillRegistry = McRPG.getInstance().registryAccess().registry(McRPGRegistryKey.SKILL);
        int experience = event.getExperience();

        // Per-skill XP — only if the third-party skill registered its statistic
        Skill skill = skillRegistry.getRegisteredSkill(event.getSkillKey());
        if (statisticRegistry.getStatistic(skill.getExperienceStatisticKey()).isPresent()) {
            stats.incrementLong(skill.getExperienceStatisticKey(), experience);
        }

        // Total XP across all skills (always tracked)
        stats.incrementLong(McRPGStatistic.TOTAL_SKILL_EXPERIENCE.getStatisticKey(), experience);

        // Block-based statistics — only count when XP came from actually breaking a block
        if (event.getGainReason() == McRPGGainReason.BLOCK_BREAK) {
            stats.incrementLong(McRPGStatistic.BLOCKS_MINED.getStatisticKey(), 1);

            if (event.getSkillKey().equals(Mining.MINING_KEY)) {
                stats.incrementLong(McRPGStatistic.ORES_MINED.getStatisticKey(), 1);
            } else if (event.getSkillKey().equals(WoodCutting.WOODCUTTING_KEY)) {
                stats.incrementLong(McRPGStatistic.TREES_CHOPPED.getStatisticKey(), 1);
            } else if (event.getSkillKey().equals(Herbalism.HERBALISM_KEY)) {
                stats.incrementLong(McRPGStatistic.CROPS_HARVESTED.getStatisticKey(), 1);
            }
        }
    }

    /**
     * Tracks per-skill max level and total levels gained.
     *
     * @param event The post-level-up event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillLevelUp(@NotNull PostSkillGainLevelEvent event) {
        Optional<McRPGPlayer> playerOptional = McRPG.getInstance().registryAccess()
                .registry(RegistryKey.MANAGER).manager(McRPGManagerKey.PLAYER)
                .getPlayer(event.getSkillHolder().getUUID());
        if (playerOptional.isEmpty()) {
            return;
        }

        PlayerStatisticData stats = playerOptional.get().getStatisticData();
        StatisticRegistry statisticRegistry = McRPG.getInstance().registryAccess().registry(RegistryKey.STATISTIC);
        Skill skill = McRPG.getInstance().registryAccess().registry(McRPGRegistryKey.SKILL)
                .getRegisteredSkill(event.getSkillKey());

        // Update per-skill max level — only if the third-party skill registered its statistic
        if (statisticRegistry.getStatistic(skill.getMaxLevelStatisticKey()).isPresent()) {
            stats.setMaxInt(skill.getMaxLevelStatisticKey(), event.getAfterLevel());
        }

        // Increment total levels gained (always tracked)
        int levelsGained = event.getAfterLevel() - event.getBeforeLevel();
        stats.incrementLong(McRPGStatistic.TOTAL_SKILL_LEVELS_GAINED.getStatisticKey(), levelsGained);
    }
}
