package us.eunoians.mcrpg.event.ability;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.ability.Ability;

/**
 * This event is called whenever an {@link Ability} is unregistered from McRPG by using
 * {@link us.eunoians.mcrpg.ability.AbilityRegistry#unregisterAbility(NamespacedKey)}.
 */
public class AbilityUnregisterEvent extends AbilityEvent {

    public AbilityUnregisterEvent(@NotNull Ability ability) {
        super(ability);
    }
}
