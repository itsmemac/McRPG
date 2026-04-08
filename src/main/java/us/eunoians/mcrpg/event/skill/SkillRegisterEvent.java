package us.eunoians.mcrpg.event.skill;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.skill.Skill;

/**
 * This event is fired whenever a {@link Skill} is registered to the
 * {@link us.eunoians.mcrpg.skill.SkillRegistry} and is available for use.
 */
public class SkillRegisterEvent extends SkillEvent {

    public SkillRegisterEvent(@NotNull Skill skill) {
        this(skill.getSkillKey());
    }

    public SkillRegisterEvent(@NotNull NamespacedKey skillKey) {
        super(skillKey);
    }
}