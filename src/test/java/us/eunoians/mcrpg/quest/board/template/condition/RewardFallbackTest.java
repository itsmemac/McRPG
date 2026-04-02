package us.eunoians.mcrpg.quest.board.template.condition;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import us.eunoians.mcrpg.quest.reward.QuestRewardType;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RewardFallback} and {@link QuestRewardEntry} fallback resolution.
 */
class RewardFallbackTest {

    private static final NamespacedKey REWARD_KEY = NamespacedKey.fromString("mcrpg:test_reward");

    private QuestRewardType mockReward() {
        QuestRewardType r = mock(QuestRewardType.class);
        when(r.getKey()).thenReturn(REWARD_KEY);
        return r;
    }

    private TemplateCondition alwaysTrue() {
        TemplateCondition c = mock(TemplateCondition.class);
        when(c.evaluate(any())).thenReturn(true);
        return c;
    }

    private TemplateCondition alwaysFalse() {
        TemplateCondition c = mock(TemplateCondition.class);
        when(c.evaluate(any())).thenReturn(false);
        return c;
    }

    @Nested
    @DisplayName("RewardFallback.resolveReward()")
    class RewardFallbackResolution {

        @Test
        @DisplayName("condition false → primary reward returned")
        void conditionFalseReturnsPrimary() {
            QuestRewardType primary = mockReward();
            QuestRewardType fallback = mockReward();
            RewardFallback rf = new RewardFallback(alwaysFalse(), fallback);

            ConditionContext ctx = new ConditionContext(null, null, null, null, UUID.randomUUID(), null);
            assertSame(primary, rf.resolveReward(ctx, primary));
        }

        @Test
        @DisplayName("condition true → fallback reward returned")
        void conditionTrueReturnsFallback() {
            QuestRewardType primary = mockReward();
            QuestRewardType fallback = mockReward();
            RewardFallback rf = new RewardFallback(alwaysTrue(), fallback);

            ConditionContext ctx = new ConditionContext(null, null, null, null, UUID.randomUUID(), null);
            assertSame(fallback, rf.resolveReward(ctx, primary));
        }

        @Test
        @DisplayName("fallback reward can differ in type from primary")
        void fallbackCanDifferFromPrimary() {
            QuestRewardType primary = mockReward();
            QuestRewardType fallback = mockReward();
            RewardFallback rf = new RewardFallback(alwaysTrue(), fallback);

            ConditionContext ctx = new ConditionContext(null, null, null, null, UUID.randomUUID(), null);
            QuestRewardType resolved = rf.resolveReward(ctx, primary);
            assertSame(fallback, resolved);
        }
    }

    @Nested
    @DisplayName("QuestRewardEntry.resolveForPlayer()")
    class QuestRewardEntryResolution {

        @Test
        @DisplayName("null fallback always returns primary reward")
        void nullFallbackReturnsPrimary() {
            QuestRewardType primary = mockReward();
            QuestRewardEntry entry = new QuestRewardEntry(primary);

            ConditionContext ctx = new ConditionContext(null, null, null, null, UUID.randomUUID(), null);
            assertSame(primary, entry.resolveForPlayer(ctx));
        }

        @Test
        @DisplayName("fallback condition false → primary returned")
        void fallbackConditionFalseReturnsPrimary() {
            QuestRewardType primary = mockReward();
            QuestRewardType fallback = mockReward();
            QuestRewardEntry entry = new QuestRewardEntry(primary, new RewardFallback(alwaysFalse(), fallback));

            ConditionContext ctx = new ConditionContext(null, null, null, null, UUID.randomUUID(), null);
            assertSame(primary, entry.resolveForPlayer(ctx));
        }

        @Test
        @DisplayName("fallback condition true → fallback returned")
        void fallbackConditionTrueReturnsFallback() {
            QuestRewardType primary = mockReward();
            QuestRewardType fallback = mockReward();
            QuestRewardEntry entry = new QuestRewardEntry(primary, new RewardFallback(alwaysTrue(), fallback));

            ConditionContext ctx = new ConditionContext(null, null, null, null, UUID.randomUUID(), null);
            assertSame(fallback, entry.resolveForPlayer(ctx));
        }

        @Test
        @DisplayName("context uses forRewardGrant factory — player UUID present, generation fields null")
        void rewardGrantContextHasPlayerUUID() {
            UUID playerUUID = UUID.randomUUID();
            ConditionContext ctx = ConditionContext.forRewardGrant(playerUUID, null, null);

            QuestRewardType primary = mockReward();
            QuestRewardType fallback = mockReward();

            TemplateCondition playerCheck = mock(TemplateCondition.class);
            when(playerCheck.evaluate(any())).thenAnswer(invocation -> {
                ConditionContext c = invocation.getArgument(0);
                return c.playerUUID() != null && c.playerUUID().equals(playerUUID);
            });

            QuestRewardEntry entry = new QuestRewardEntry(primary, new RewardFallback(playerCheck, fallback));
            assertSame(fallback, entry.resolveForPlayer(ctx));
        }
    }

    @Nested
    @DisplayName("Compound condition as fallback trigger")
    class CompoundFallbackTrigger {

        @Test
        @DisplayName("ALL compound with both conditions true → fallback granted")
        void allCompoundBothTrueGrantsFallback() {
            QuestRewardType primary = mockReward();
            QuestRewardType fallback = mockReward();
            CompoundCondition compound = new CompoundCondition(
                    Map.of("a", alwaysTrue(), "b", alwaysTrue()),
                    CompoundCondition.LogicMode.ALL);
            QuestRewardEntry entry = new QuestRewardEntry(primary, new RewardFallback(compound, fallback));

            ConditionContext ctx = new ConditionContext(null, null, null, null, UUID.randomUUID(), null);
            assertSame(fallback, entry.resolveForPlayer(ctx));
        }

        @Test
        @DisplayName("ALL compound with one false condition → primary granted")
        void allCompoundOneFalseGrantsPrimary() {
            QuestRewardType primary = mockReward();
            QuestRewardType fallback = mockReward();
            CompoundCondition compound = new CompoundCondition(
                    Map.of("a", alwaysTrue(), "b", alwaysFalse()),
                    CompoundCondition.LogicMode.ALL);
            QuestRewardEntry entry = new QuestRewardEntry(primary, new RewardFallback(compound, fallback));

            ConditionContext ctx = new ConditionContext(null, null, null, null, UUID.randomUUID(), null);
            assertSame(primary, entry.resolveForPlayer(ctx));
        }
    }
}
