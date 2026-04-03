package us.eunoians.mcrpg.statistic;

import com.diamonddagger590.mccore.statistic.StatisticType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.eunoians.mcrpg.McRPGBaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class McRPGStatisticTest extends McRPGBaseTest {

    @DisplayName("Given static statistics, when checking ALL_STATIC_STATISTICS, then it contains global stats but not per-skill stats")
    @Test
    public void allStaticStatistics_containsGlobalStats() {
        assertTrue(McRPGStatistic.ALL_STATIC_STATISTICS.contains(McRPGStatistic.BLOCKS_MINED));
        assertTrue(McRPGStatistic.ALL_STATIC_STATISTICS.contains(McRPGStatistic.MOBS_KILLED));
        assertTrue(McRPGStatistic.ALL_STATIC_STATISTICS.contains(McRPGStatistic.DAMAGE_DEALT));
        assertTrue(McRPGStatistic.ALL_STATIC_STATISTICS.contains(McRPGStatistic.DAMAGE_TAKEN));
        assertTrue(McRPGStatistic.ALL_STATIC_STATISTICS.contains(McRPGStatistic.TOTAL_SKILL_EXPERIENCE));
        assertTrue(McRPGStatistic.ALL_STATIC_STATISTICS.contains(McRPGStatistic.TOTAL_SKILL_LEVELS_GAINED));
        assertTrue(McRPGStatistic.ALL_STATIC_STATISTICS.contains(McRPGStatistic.ABILITIES_ACTIVATED));
    }

    @DisplayName("Given ALL_STATIC_STATISTICS, when checking contents, then it is not empty and is unmodifiable")
    @Test
    public void allStaticStatistics_isNotEmptyAndUnmodifiable() {
        assertFalse(McRPGStatistic.ALL_STATIC_STATISTICS.isEmpty());
        assertNotNull(McRPGStatistic.ALL_STATIC_STATISTICS);
        assertThrows(UnsupportedOperationException.class, () -> McRPGStatistic.ALL_STATIC_STATISTICS.add(McRPGStatistic.BLOCKS_MINED));
    }

    @DisplayName("Given DAMAGE_DEALT, when checking type, then it is DOUBLE")
    @Test
    public void damageDealt_isDoubleType() {
        assertEquals(StatisticType.DOUBLE, McRPGStatistic.DAMAGE_DEALT.getStatisticType());
        assertEquals(0.0, McRPGStatistic.DAMAGE_DEALT.getDefaultValue());
    }

    @DisplayName("Given BLOCKS_MINED, when checking type, then it is LONG")
    @Test
    public void blocksMined_isLongType() {
        assertEquals(StatisticType.LONG, McRPGStatistic.BLOCKS_MINED.getStatisticType());
        assertEquals(0L, McRPGStatistic.BLOCKS_MINED.getDefaultValue());
    }
}
