package us.eunoians.mcrpg.event.ability;

import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.ability.Ability;

/**
 * This event is called whenever an {@link Ability} is registered to McRPG by using
 * {@link us.eunoians.mcrpg.ability.AbilityRegistry#register(Ability)}.
 */
public class AbilityRegisterEvent extends AbilityEvent {

    public AbilityRegisterEvent(@NotNull Ability ability){
        super(ability);
    }
}
