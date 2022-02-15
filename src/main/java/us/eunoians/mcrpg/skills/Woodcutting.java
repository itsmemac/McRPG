package us.eunoians.mcrpg.skills;

import us.eunoians.mcrpg.abilities.BaseAbility;
import us.eunoians.mcrpg.players.McRPGPlayer;
import us.eunoians.mcrpg.types.GenericAbility;
import us.eunoians.mcrpg.types.Skills;

import java.util.Map;

public class Woodcutting extends Skill {

  public Woodcutting(int currentLevel, int currentExp, Map<GenericAbility, BaseAbility> map, McRPGPlayer player) {
    super(Skills.WOODCUTTING, map, currentLevel, currentExp, player);
  }
}
