package us.eunoians.mcrpg.quest.board.distribution;

import com.diamonddagger590.mccore.registry.RegistryAccess;
import com.diamonddagger590.mccore.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import us.eunoians.mcrpg.McRPGBaseTest;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.entity.player.McRPGPlayerExtension;
import us.eunoians.mcrpg.localization.McRPGLocalizationManager;
import us.eunoians.mcrpg.quest.board.distribution.builtin.ContributionThresholdDistributionType;
import us.eunoians.mcrpg.quest.board.distribution.builtin.MembershipDistributionType;
import us.eunoians.mcrpg.quest.board.distribution.builtin.ParticipatedDistributionType;
import us.eunoians.mcrpg.quest.board.distribution.builtin.QuestAcceptorDistributionType;
import us.eunoians.mcrpg.quest.board.distribution.builtin.TopPlayersDistributionType;
import us.eunoians.mcrpg.quest.board.rarity.QuestRarityRegistry;
import us.eunoians.mcrpg.quest.reward.QuestRewardType;
import us.eunoians.mcrpg.registry.manager.McRPGManagerKey;
import us.eunoians.mcrpg.util.McRPGMethods;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DistributionPreviewResolver} and {@link DistributionPreviewEntry}.
 *
 * <p>{@link DistributionPreviewResolver#buildPreview} resolves qualification
 * logic purely from the contribution snapshot and registered distribution types.
 * The localization manager (accessed statically via {@code RegistryAccess}) is
 * already mocked by {@link McRPGBaseTest}; we stub it to return a simple
 * component so that reward-preview lines don't require a live server.
 */
@ExtendWith(McRPGPlayerExtension.class)
public class DistributionPreviewResolverTest extends McRPGBaseTest {

    private static final NamespacedKey REWARD_KEY =
            new NamespacedKey(McRPGMethods.getMcRPGNamespace(), "preview_reward");

    private QuestRarityRegistry rarityRegistry;
    private RewardDistributionTypeRegistry typeRegistry;

    @BeforeEach
    void setUp() {
        rarityRegistry = new QuestRarityRegistry();

        typeRegistry = new RewardDistributionTypeRegistry();
        typeRegistry.register(new TopPlayersDistributionType());
        typeRegistry.register(new ContributionThresholdDistributionType());
        typeRegistry.register(new ParticipatedDistributionType());
        typeRegistry.register(new MembershipDistributionType());
        typeRegistry.register(new QuestAcceptorDistributionType());

        McRPGLocalizationManager localization = RegistryAccess.registryAccess()
                .registry(RegistryKey.MANAGER)
                .manager(McRPGManagerKey.LOCALIZATION);
        lenient().when(localization.getLocalizedMessageAsComponent(
                        any(McRPGPlayer.class), any(), anyMap()))
                .thenReturn(Component.text("reward-preview"));
    }

    // -------------------------------------------------------------------------
    // 11.19  buildPreview — qualification logic
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("buildPreview — qualification logic")
    class QualificationLogic {

        @Test
        @DisplayName("top contributor qualifies for TOP_PLAYERS tier")
        void givenTopContributor_whenBuildingPreview_thenQualifiesForTopPlayersTier(McRPGPlayer player) {
            UUID topPlayer = UUID.randomUUID();
            UUID otherPlayer = UUID.randomUUID();

            Map<UUID, Long> contributions = Map.of(topPlayer, 80L, otherPlayer, 20L);
            var tier = new DistributionTierConfig(
                    "top-players",
                    TopPlayersDistributionType.KEY,
                    RewardSplitMode.INDIVIDUAL,
                    List.of(new DistributionRewardEntry(mockReward(), PotBehavior.ALL,
                            RemainderStrategy.DISCARD, 1, 1, null)),
                    Map.of(DistributionTierConfig.PARAM_TOP_PLAYER_COUNT, 1),
                    null, null);
            var config = new RewardDistributionConfig(List.of(tier));

            List<DistributionPreviewEntry> preview = new DistributionPreviewResolver().buildPreview(
                    player, config, topPlayer, contributions, null,
                    rarityRegistry, typeRegistry, Set.of(), null);

            assertEquals(1, preview.size());
            assertTrue(preview.get(0).qualifies(),
                    "Top contributor must qualify for TOP_PLAYERS tier");
        }

        @Test
        @DisplayName("lower contributor does not qualify for TOP_PLAYERS(count=1) tier")
        void givenLowerContributor_whenBuildingPreview_thenDoesNotQualifyForTopPlayersTier(McRPGPlayer player) {
            UUID topPlayer = UUID.randomUUID();
            UUID lowerPlayer = UUID.randomUUID();

            Map<UUID, Long> contributions = Map.of(topPlayer, 80L, lowerPlayer, 20L);
            var tier = new DistributionTierConfig(
                    "top-players",
                    TopPlayersDistributionType.KEY,
                    RewardSplitMode.INDIVIDUAL,
                    List.of(new DistributionRewardEntry(mockReward(), PotBehavior.ALL,
                            RemainderStrategy.DISCARD, 1, 1, null)),
                    Map.of(DistributionTierConfig.PARAM_TOP_PLAYER_COUNT, 1),
                    null, null);
            var config = new RewardDistributionConfig(List.of(tier));

            List<DistributionPreviewEntry> preview = new DistributionPreviewResolver().buildPreview(
                    player, config, lowerPlayer, contributions, null,
                    rarityRegistry, typeRegistry, Set.of(), null);

            assertFalse(preview.get(0).qualifies(),
                    "Player with lower contribution must not qualify for top-1 tier");
        }

        @Test
        @DisplayName("player with 50% contribution qualifies for CONTRIBUTION_THRESHOLD(20%) tier")
        void givenPlayerWith50PercentContribution_whenBuildingPreview_thenQualifiesForThresholdTier(McRPGPlayer player) {
            UUID playerUUID = UUID.randomUUID();
            UUID other = UUID.randomUUID();

            Map<UUID, Long> contributions = Map.of(playerUUID, 50L, other, 50L);
            var tier = new DistributionTierConfig(
                    "threshold",
                    ContributionThresholdDistributionType.KEY,
                    RewardSplitMode.INDIVIDUAL,
                    List.of(new DistributionRewardEntry(mockReward(), PotBehavior.ALL,
                            RemainderStrategy.DISCARD, 1, 1, null)),
                    Map.of(DistributionTierConfig.PARAM_MIN_CONTRIBUTION_PERCENT, 20.0),
                    null, null);
            var config = new RewardDistributionConfig(List.of(tier));

            List<DistributionPreviewEntry> preview = new DistributionPreviewResolver().buildPreview(
                    player, config, playerUUID, contributions, null,
                    rarityRegistry, typeRegistry, Set.of(), null);

            assertTrue(preview.get(0).qualifies(),
                    "Player with 50% contribution must qualify for 20%-threshold tier");
            assertEquals(50L, preview.get(0).currentContribution());
            assertEquals(50.0, preview.get(0).currentPercent(), 0.5);
        }

        @Test
        @DisplayName("player with no contribution does not qualify for PARTICIPATED tier")
        void givenPlayerWithNoContribution_whenBuildingPreview_thenDoesNotQualifyForParticipated(McRPGPlayer player) {
            UUID activePlayers = UUID.randomUUID();
            UUID absentPlayer = UUID.randomUUID();

            // absentPlayer has 0 contribution (not in map)
            Map<UUID, Long> contributions = Map.of(activePlayers, 100L);
            var tier = new DistributionTierConfig(
                    "participated",
                    ParticipatedDistributionType.KEY,
                    RewardSplitMode.INDIVIDUAL,
                    List.of(new DistributionRewardEntry(mockReward(), PotBehavior.ALL,
                            RemainderStrategy.DISCARD, 1, 1, null)),
                    Map.of(),
                    null, null);
            var config = new RewardDistributionConfig(List.of(tier));

            List<DistributionPreviewEntry> preview = new DistributionPreviewResolver().buildPreview(
                    player, config, absentPlayer, contributions, null,
                    rarityRegistry, typeRegistry, Set.of(), null);

            assertFalse(preview.get(0).qualifies(),
                    "Player with no contribution must not qualify for PARTICIPATED tier");
            assertEquals(0L, preview.get(0).currentContribution());
        }

        @Test
        @DisplayName("group member qualifies for MEMBERSHIP tier regardless of contribution")
        void givenGroupMember_whenBuildingPreview_thenQualifiesForMembershipTier(McRPGPlayer player) {
            UUID member = UUID.randomUUID();
            UUID nonMember = UUID.randomUUID();
            Set<UUID> groupMembers = Set.of(member, nonMember);

            // member has no contributions but is in the group
            Map<UUID, Long> contributions = Map.of();
            var tier = new DistributionTierConfig(
                    "membership",
                    MembershipDistributionType.KEY,
                    RewardSplitMode.INDIVIDUAL,
                    List.of(new DistributionRewardEntry(mockReward(), PotBehavior.ALL,
                            RemainderStrategy.DISCARD, 1, 1, null)),
                    Map.of(),
                    null, null);
            var config = new RewardDistributionConfig(List.of(tier));

            List<DistributionPreviewEntry> memberPreview = new DistributionPreviewResolver().buildPreview(
                    player, config, member, contributions, null,
                    rarityRegistry, typeRegistry, groupMembers, null);
            assertTrue(memberPreview.get(0).qualifies(),
                    "Group member must qualify for MEMBERSHIP tier");
        }

        @Test
        @DisplayName("quest acceptor qualifies for QUEST_ACCEPTOR tier; non-acceptor does not")
        void givenQuestAcceptor_whenBuildingPreview_thenQualifiesAndNonAcceptorDoesNot(McRPGPlayer player) {
            UUID acceptor = UUID.randomUUID();
            UUID participant = UUID.randomUUID();
            Map<UUID, Long> contributions = Map.of(acceptor, 60L, participant, 40L);

            var tier = new DistributionTierConfig(
                    "acceptor-bonus",
                    QuestAcceptorDistributionType.KEY,
                    RewardSplitMode.INDIVIDUAL,
                    List.of(new DistributionRewardEntry(mockReward(), PotBehavior.ALL,
                            RemainderStrategy.DISCARD, 1, 1, null)),
                    Map.of(),
                    null, null);
            var config = new RewardDistributionConfig(List.of(tier));

            List<DistributionPreviewEntry> acceptorPreview = new DistributionPreviewResolver().buildPreview(
                    player, config, acceptor, contributions, null,
                    rarityRegistry, typeRegistry, Set.of(), acceptor);
            assertTrue(acceptorPreview.get(0).qualifies(),
                    "Quest acceptor must qualify for QUEST_ACCEPTOR tier");

            List<DistributionPreviewEntry> participantPreview = new DistributionPreviewResolver().buildPreview(
                    player, config, participant, contributions, null,
                    rarityRegistry, typeRegistry, Set.of(), acceptor);
            assertFalse(participantPreview.get(0).qualifies(),
                    "Non-acceptor participant must not qualify for QUEST_ACCEPTOR tier");
        }

        @Test
        @DisplayName("preview reflects correct contribution amounts and percentages")
        void givenPlayerWith30PercentContribution_whenBuildingPreview_thenContributionDataIsAccurate(McRPGPlayer player) {
            UUID playerUUID = UUID.randomUUID();
            Map<UUID, Long> contributions = Map.of(playerUUID, 30L, UUID.randomUUID(), 70L);

            var tier = new DistributionTierConfig(
                    "participated",
                    ParticipatedDistributionType.KEY,
                    RewardSplitMode.INDIVIDUAL,
                    List.of(new DistributionRewardEntry(mockReward(), PotBehavior.ALL,
                            RemainderStrategy.DISCARD, 1, 1, null)),
                    Map.of(), null, null);
            var config = new RewardDistributionConfig(List.of(tier));

            List<DistributionPreviewEntry> preview = new DistributionPreviewResolver().buildPreview(
                    player, config, playerUUID, contributions, null,
                    rarityRegistry, typeRegistry, Set.of(), null);

            assertEquals(30L, preview.get(0).currentContribution());
            assertEquals(30.0, preview.get(0).currentPercent(), 0.5);
        }
    }

    // -------------------------------------------------------------------------
    // DistributionPreviewEntry — record structure (preserved from prior revision)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DistributionPreviewEntry record")
    class PreviewEntryRecord {

        @Test
        @DisplayName("entry stores all fields verbatim")
        void entryStoresData() {
            List<Component> rewards = List.of(Component.text("50 exp"));
            DistributionPreviewEntry entry = new DistributionPreviewEntry(
                    "top-tier", true, 75L, 75.0, rewards);

            assertEquals("top-tier", entry.tierKey());
            assertTrue(entry.qualifies());
            assertEquals(75L, entry.currentContribution());
            assertEquals(75.0, entry.currentPercent(), 0.001);
            assertEquals(1, entry.projectedRewards().size());
        }

        @Test
        @DisplayName("non-qualifying entry reports false and empty rewards")
        void nonQualifyingEntryHasNoRewards() {
            DistributionPreviewEntry entry = new DistributionPreviewEntry(
                    "some-tier", false, 0L, 0.0, List.of());

            assertFalse(entry.qualifies());
            assertEquals(0L, entry.currentContribution());
            assertEquals(0.0, entry.currentPercent(), 0.001);
            assertTrue(entry.projectedRewards().isEmpty());
        }

        @Test
        @DisplayName("projectedRewards list is non-null for qualifying entry")
        void qualifyingEntryHasNonNullRewards() {
            DistributionPreviewEntry entry = new DistributionPreviewEntry(
                    "tier", true, 100L, 100.0, List.of(Component.text("100 coins")));

            assertNotNull(entry.projectedRewards());
            assertFalse(entry.projectedRewards().isEmpty());
        }

        @Test
        @DisplayName("tierKey is preserved exactly")
        void tierKeyPreserved() {
            DistributionPreviewEntry entry = new DistributionPreviewEntry(
                    "my_custom_tier_key", true, 1L, 50.0, List.of());

            assertEquals("my_custom_tier_key", entry.tierKey());
        }

        @Test
        @DisplayName("contribution percentage is independent of qualification")
        void percentageStoredRegardlessOfQualification() {
            DistributionPreviewEntry qualified = new DistributionPreviewEntry(
                    "t", true, 50L, 50.0, List.of());
            DistributionPreviewEntry notQualified = new DistributionPreviewEntry(
                    "t", false, 50L, 50.0, List.of());

            assertEquals(50.0, qualified.currentPercent(), 0.001);
            assertEquals(50.0, notQualified.currentPercent(), 0.001);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private QuestRewardType mockReward() {
        QuestRewardType reward = mock(QuestRewardType.class);
        when(reward.getKey()).thenReturn(REWARD_KEY);
        when(reward.getNumericAmount()).thenReturn(OptionalLong.of(100L));
        return reward;
    }
}
