package us.eunoians.mcrpg.event.skill;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.skill.Skill;

public class SkillUnregisterEvent extends SkillEvent {

    public SkillUnregisterEvent(@NotNull Skill skill) {
        this(skill.getSkillKey());
    }

    public SkillUnregisterEvent(@NotNull NamespacedKey skillKey) {
        super(skillKey);
    }
}
