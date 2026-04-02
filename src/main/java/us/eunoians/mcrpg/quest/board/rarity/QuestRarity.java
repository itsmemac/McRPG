package us.eunoians.mcrpg.quest.board.rarity;

import com.diamonddagger590.mccore.builder.item.impl.ItemBuilder;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.eunoians.mcrpg.expansion.content.McRPGContent;

import java.util.Optional;

/**
 * Immutable data class representing a single rarity tier for board quests.
 * <p>
 * Loaded from {@code board.yml} config (auto-namespaced under {@code mcrpg:}) or
 * registered programmatically by third-party plugins via content packs.
 * <p>
 * Icon configuration is driven by the {@code display:} section of the rarity's config
 * entry. The section is applied to an {@link ItemBuilder} via
 * {@link ItemBuilder#from(Section, ItemBuilder)}, so any key supported by
 * {@link com.diamonddagger590.mccore.builder.item.ItemBuilderConfigurationKeys} (such as
 * {@code material}, {@code custom-model-data}, {@code settings.glowing}) can be used.
 * The {@code name-color} key is a McRPG extension and is extracted separately.
 */
public final class QuestRarity implements McRPGContent {

    private final NamespacedKey key;
    private final int weight;
    private final double difficultyMultiplier;
    private final double rewardMultiplier;
    private final NamespacedKey expansionKey;
    @Nullable
    private final Section iconSection;
    @Nullable
    private final String nameColor;

    /**
     * Minimal constructor for rarities without icon configuration (e.g. test fixtures,
     * programmatically registered rarities without display metadata).
     *
     * @param key                  the unique key identifying this rarity
     * @param weight               the selection weight (higher = more common)
     * @param difficultyMultiplier the quest difficulty scaling factor
     * @param rewardMultiplier     the reward scaling factor
     * @param expansionKey         the key of the content expansion that owns this rarity
     */
    public QuestRarity(@NotNull NamespacedKey key,
                       int weight,
                       double difficultyMultiplier,
                       double rewardMultiplier,
                       @NotNull NamespacedKey expansionKey) {
        this(key, weight, difficultyMultiplier, rewardMultiplier, expansionKey, null, null);
    }

    /**
     * Full constructor for rarities with an icon section and name color.
     *
     * @param key                  the unique key identifying this rarity
     * @param weight               the selection weight (higher = more common)
     * @param difficultyMultiplier the quest difficulty scaling factor
     * @param rewardMultiplier     the reward scaling factor
     * @param expansionKey         the key of the content expansion that owns this rarity
     * @param iconSection          the YAML {@code display:} section used to configure offering
     *                             icon appearance via {@link ItemBuilder#from(Section, ItemBuilder)};
     *                             may be {@code null} for rarities without custom icons
     * @param nameColor            a MiniMessage color tag (e.g. {@code "<gold>"}) prepended to
     *                             offering item names; may be {@code null} to use the default
     */
    public QuestRarity(@NotNull NamespacedKey key,
                       int weight,
                       double difficultyMultiplier,
                       double rewardMultiplier,
                       @NotNull NamespacedKey expansionKey,
                       @Nullable Section iconSection,
                       @Nullable String nameColor) {
        this.key = key;
        this.weight = weight;
        this.difficultyMultiplier = difficultyMultiplier;
        this.rewardMultiplier = rewardMultiplier;
        this.expansionKey = expansionKey;
        this.iconSection = iconSection;
        this.nameColor = nameColor;
    }

    /**
     * Returns the unique key identifying this rarity.
     *
     * @return the rarity key
     */
    @NotNull
    public NamespacedKey getKey() {
        return key;
    }

    /**
     * Returns the selection weight for this rarity. Higher weight means more common.
     *
     * @return the weight value
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Returns the difficulty multiplier applied to quests of this rarity.
     *
     * @return the difficulty multiplier
     */
    public double getDifficultyMultiplier() {
        return difficultyMultiplier;
    }

    /**
     * Returns the reward multiplier applied to rewards of quests with this rarity.
     *
     * @return the reward multiplier
     */
    public double getRewardMultiplier() {
        return rewardMultiplier;
    }

    /**
     * Applies this rarity's icon configuration to the given {@link ItemBuilder}.
     * If no icon section is configured, the builder is returned unchanged.
     * <p>
     * Uses {@link ItemBuilder#from(Section, ItemBuilder)} so any key from
     * {@link com.diamonddagger590.mccore.builder.item.ItemBuilderConfigurationKeys}
     * present in the display section (e.g. {@code material}, {@code custom-model-data},
     * {@code settings.glowing}) will be applied.
     *
     * @param builder the builder to configure
     * @return the same builder, possibly with icon settings applied
     */
    @NotNull
    public ItemBuilder configureIcon(@NotNull ItemBuilder builder) {
        if (iconSection != null) {
            return ItemBuilder.from(iconSection, builder);
        }
        return builder;
    }

    /**
     * Returns the MiniMessage color tag to prepend to offering item names for this rarity.
     * For example, {@code "<gold>"} for legendary quests.
     *
     * @return the name color tag, or empty to use the localization default
     */
    @NotNull
    public Optional<String> getNameColor() {
        return Optional.ofNullable(nameColor);
    }

    @NotNull
    @Override
    public Optional<NamespacedKey> getExpansionKey() {
        return Optional.of(expansionKey);
    }
}
