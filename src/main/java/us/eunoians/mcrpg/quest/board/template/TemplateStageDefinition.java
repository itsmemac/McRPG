package us.eunoians.mcrpg.quest.board.template;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.eunoians.mcrpg.quest.board.template.condition.TemplateCondition;

import java.util.List;
import java.util.Optional;

/**
 * A stage within a {@link TemplatePhaseDefinition}, containing one or more
 * objective definitions with potentially unresolved variable references.
 *
 * @param objectives          the objectives in this stage
 * @param condition           optional condition evaluated during generation to include/exclude this stage
 * @param objectiveSelection  optional configuration for weighted random objective selection
 */
public record TemplateStageDefinition(
        @NotNull List<TemplateObjectiveDefinition> objectives,
        @Nullable TemplateCondition condition,
        @Nullable ObjectiveSelectionConfig objectiveSelection
) {

    /** Canonical constructor — makes {@code objectives} immutable. */
    public TemplateStageDefinition {
        objectives = List.copyOf(objectives);
    }

    /**
     * Backward-compatible constructor without condition and selection config.
     */
    public TemplateStageDefinition(@NotNull List<TemplateObjectiveDefinition> objectives) {
        this(objectives, null, null);
    }

    /**
     * Returns the optional generation-time condition that must evaluate to {@code true}
     * for this stage to be included in the generated quest.
     *
     * @return the condition, or empty if this stage is unconditional
     */
    @NotNull
    public Optional<TemplateCondition> getCondition() {
        return Optional.ofNullable(condition);
    }

    /**
     * Returns the optional weighted objective selection config. When empty, all
     * objectives in the stage are included ({@link ObjectiveSelectionConfig.ObjectiveSelectionMode#ALL}).
     *
     * @return the selection config, or empty for full inclusion
     */
    @NotNull
    public Optional<ObjectiveSelectionConfig> getObjectiveSelection() {
        return Optional.ofNullable(objectiveSelection);
    }

    /**
     * Returns a copy of this stage with the given objectives list, preserving all
     * other fields (condition, objective selection config).
     *
     * @param newObjectives the replacement objectives list
     * @return a new {@link TemplateStageDefinition} with the updated objectives
     */
    @NotNull
    public TemplateStageDefinition withObjectives(@NotNull List<TemplateObjectiveDefinition> newObjectives) {
        return new TemplateStageDefinition(newObjectives, condition, objectiveSelection);
    }
}
