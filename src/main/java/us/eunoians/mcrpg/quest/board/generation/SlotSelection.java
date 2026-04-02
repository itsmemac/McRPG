package us.eunoians.mcrpg.quest.board.generation;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.eunoians.mcrpg.quest.board.BoardOffering;
import us.eunoians.mcrpg.quest.board.BoardRotation;
import us.eunoians.mcrpg.quest.board.category.BoardSlotCategory;
import us.eunoians.mcrpg.quest.board.template.GeneratedQuestResult;

import java.util.UUID;

/**
 * Represents the outcome of the unified quest source selection for a single board slot.
 * Either a hand-crafted definition was chosen or a template-generated definition was produced.
 */
public sealed interface SlotSelection permits SlotSelection.HandCrafted, SlotSelection.TemplateGenerated {

    /**
     * Converts this selection into a {@link BoardOffering} for the given rotation, category, and slot.
     *
     * @param rotation       the current board rotation
     * @param category       the category this slot belongs to
     * @param slotIndex      the positional index within the offering list
     * @param ownerIdentifier the owner identifier for personal or scoped offerings
     *                       ({@code null} for shared offerings, player UUID string for personal,
     *                       entity ID for scoped)
     * @return the constructed board offering
     */
    @NotNull
    default BoardOffering toOffering(@NotNull BoardRotation rotation,
                                     @NotNull BoardSlotCategory category,
                                     int slotIndex,
                                     @Nullable String ownerIdentifier) {
        return switch (this) {
            case HandCrafted hc -> new BoardOffering(
                    UUID.randomUUID(),
                    rotation.getRotationId(),
                    category.getKey(),
                    slotIndex,
                    hc.definitionKey(),
                    hc.rarityKey(),
                    ownerIdentifier,
                    category.getCompletionTime()
            );
            case TemplateGenerated tmpl -> new BoardOffering(
                    UUID.randomUUID(),
                    rotation.getRotationId(),
                    category.getKey(),
                    slotIndex,
                    tmpl.result().definition().getQuestKey(),
                    tmpl.rarityKey(),
                    ownerIdentifier,
                    category.getCompletionTime(),
                    tmpl.result().templateKey(),
                    tmpl.result().serializedDefinition()
            );
        };
    }

    /**
     * A hand-crafted quest definition was selected from the {@link us.eunoians.mcrpg.quest.definition.QuestDefinitionRegistry}.
     *
     * @param definitionKey the key of the selected hand-crafted definition
     * @param rarityKey     the rarity that was rolled for this slot
     */
    record HandCrafted(@NotNull NamespacedKey definitionKey,
                       @NotNull NamespacedKey rarityKey) implements SlotSelection {}

    /**
     * A quest was generated from a template via the {@link us.eunoians.mcrpg.quest.board.template.QuestTemplateEngine}.
     *
     * @param result    the generated quest result containing the definition, template key, and serialized JSON
     * @param rarityKey the rarity that was rolled for this slot
     */
    record TemplateGenerated(@NotNull GeneratedQuestResult result,
                             @NotNull NamespacedKey rarityKey) implements SlotSelection {}
}
