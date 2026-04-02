# Upgrade Quest Design Principles

This is a living steering guide for McRPG ability upgrade quests. It captures the design intent behind the tier progression model, per-ability theming, and count scaling philosophy. Reference it when adding new tiers, new abilities, or modifying existing upgrade quests. It is **not** a design doc — it describes guiding principles, not final decisions.

---

## Tier Progression Model

Each tierable ability has one quest per upgrade tier (T2 through T5). The intent for each tier:

| Tier | Structure | Intent |
|------|-----------|--------|
| T2 | Single objective | Entry-level challenge. Introduces the ability's core thematic targets at a moderate count. Should feel accessible to a player who just unlocked the ability. |
| T3 | Single objective | Expanding repertoire. Adds harder or rarer targets to the list alongside the T2 core targets. Higher count. Requires some progression into the ability's domain. |
| T4 | Two independent objectives (both must complete) | Branching challenge. A **primary** objective targets the ability's established mob/block theme; a **secondary** objective introduces a distinctly harder target group tracked on its own separate progress bar. Both bars must reach 100% before the stage completes. |
| T5 | Two independent objectives (both must complete) | Mastery-level challenge. Same two-bar pattern as T4 but with higher counts and the hardest targets in the secondary objective. The primary may incorporate targets introduced at T3. |

### Why Multi-Objective at T4/T5?

A single expanded mob/block list at T4/T5 would produce one large progress bar that is visually and mechanically identical to T3 — just a bigger number. Two separate bars:

- Give the player clear sub-goals to work toward simultaneously.
- Allow the secondary target to be significantly harder or rarer without diluting the primary count.
- Create a natural "main task + challenge" rhythm at higher tiers without requiring multi-stage quests.

### YAML Pattern for T4/T5

```yaml
mcrpg:example_ability_tier4:
  scope: mcrpg:single_player
  repeat-mode: ONCE
  rewards:
    upgrade:
      type: mcrpg:ability_upgrade
      ability: mcrpg:example_ability
      tier: 4
  phases:
    phase:
      completion-mode: ALL
      stages:
        stage:
          key: mcrpg:example_ability_tier4_stage
          objectives:
            primary:
              key: mcrpg:example_ability_tier4_primary
              type: mcrpg:mob_kill
              required-progress: 150
              config:
                entities: [SPIDER, CAVE_SPIDER, WITCH]
            secondary:
              key: mcrpg:example_ability_tier4_secondary
              type: mcrpg:mob_kill
              required-progress: 35
              config:
                entities: [WITHER_SKELETON]
```

The map keys under `objectives:` (`primary`, `secondary`) are labels only — the canonical identity is the nested `key:` field.

---

## Per-Ability Theming Rationale

Upgrade quest targets should reinforce the **fantasy** of the ability being upgraded. The player should feel like they are practicing or mastering the ability's core concept through the mobs they fight or the blocks they break.

### Swords — all abilities use `mcrpg:mob_kill`

| Ability | Theme | Rationale |
|---------|-------|-----------|
| Enhanced Bleed | Venom / damage over time | Spiders and Cave Spiders deal poison. Witches throw poison potions. Wither Skeletons at T4/T5 escalate to the wither effect — the most punishing DoT in vanilla. |
| Deeper Wound | Piercing / ranged threats | Skeletons and Strays fire piercing arrows. Bogged (the mossy swamp skeleton) adds biome variety. Wither Skeletons at T5 represent the hardest non-boss ranged/melee threat. |
| Vampire | Undead / life drain | Core undead mobs (Zombies, Drowned, Husks) thematically match feeding on the walking dead. Zombie Villagers and Phantoms add variety at T4; Zoglins represent aggressive undead-adjacent creatures at T5. |
| Serrated Strikes | AoE / explosive | Creepers and Slimes deal burst or splash damage that mirrors the ability's multi-target nature. Magma Cubes and Blazes escalate to fiery Nether AoE threats. Ghasts at T5 demand a long-range dangerous fight. |
| Rage Spike | Nether aggression | Endermen and Piglins embody the attack-on-provocation aggression that fits the ability's spike fantasy. Zombified Piglins add horde aggression. Hoglins and Ghasts at T4/T5 push the player deep into hostile Nether territory. |

### Mining — all abilities use `mcrpg:block_break`

Each ability targets a different rung on the ore value ladder so their progression paths feel distinct.

| Ability | Theme | Rationale |
|---------|-------|-----------|
| It's A Triple | Common ore multiplication | Starts with the most accessible ores (Coal, Copper) and climbs to mid-tier (Iron, Gold). The T4/T5 payoff of breaking rare ores mirrors the ability's best use case at late game. |
| Remote Transfer | Precious metal pipeline | Focused on Iron → Gold → Nether Gold → Lapis/Redstone. The remote-delivery fantasy suits mid-value, high-volume ore veins rather than rare single-block targets. |
| Ore Scanner | Rare ore detection | Begins at Diamond/Emerald — the ability's primary use case — and escalates through Nether Quartz, Nether Gold, and ultimately Ancient Debris, the rarest minable block in vanilla. |

### Woodcutting — all abilities use `mcrpg:block_break`

Each ability targets a different biome log family so forests feel meaningfully different per ability.

| Ability | Theme | Rationale |
|---------|-------|-----------|
| Heavy Swing | Raw power / hardwood deforestation | Starts with common temperate logs (Oak, Spruce, Birch), escalates to dense canopy logs (Dark Oak, Jungle, Acacia), and culminates in the exotic Mangrove, Cherry, and nether fungal stems. |
| Dryad's Gift | Tropical / nature attunement | Focused on the most "alive" biome woods: Jungle, Acacia, Mangrove. Cherry and Bamboo Block represent the lush, rare, or delicate side of nature. |
| Nymph's Vitality | Cold / mystic forests | Birch and Spruce evoke cool-climate wilderness; Dark Oak adds depth; Mangrove and the fungal Crimson/Warped Stems represent the boundary between nature and the otherworldly. |

### Herbalism — all abilities use `mcrpg:block_break`

Each ability targets a different category of harvestable plants.

| Ability | Theme | Rationale |
|---------|-------|-----------|
| Verdant Surge | Basic crop acceleration | Starts with the Overworld's core farm crops (Wheat, Carrots, Potatoes) and adds varied produce (Beetroots, Melons, Pumpkins). Nether Wart and Cocoa Beans at T5 represent rare or exotic crops requiring non-trivial farming setups. |
| Mass Harvest | Bulk variety harvesting | Covers a wider variety including Sugar Cane and Sweet Berry Bush — plants that aren't standard crops but reward bulk collection. Nether Wart and Chorus Plant at T5 represent the rarest and most unusual harvestable plants. |

---

## Count Scaling Philosophy

Progress counts are tuned **per skill group** rather than by a universal formula. The original `20 * tier^2` formula applied identically to every ability, which produced the same curve regardless of how difficult the targets were. Goals:

- **Swords counts are lower** — mob kills require active player engagement and are slower than block breaks.
- **Mining/Woodcutting/Herbalism counts are higher** — block breaks are often done in bulk and are quicker per action.
- **Secondary objectives at T4/T5 have noticeably lower counts** — they represent a focused challenge against harder targets, not a grind.

### Reference Counts

| Tier | Swords primary | Mining primary | Woodcutting primary | Herbalism primary | T4/T5 secondary |
|------|----------------|----------------|---------------------|-------------------|-----------------|
| T2   | 60             | 80             | 80                  | 80                | —               |
| T3   | 100            | 120            | 120                 | 120               | —               |
| T4   | 150            | 150            | 150                 | 160               | 35–45           |
| T5   | 200            | 200            | 200–220             | 220               | 45–60           |

These values are defaults in the bundled YAML. Server owners can freely adjust them; this table records the design intent, not a constraint.

---

## Generic Fallback Quests

Each ability has a **generic fallback** quest in `generic_ability_upgrades.yml` (e.g., `mcrpg:enhanced_bleed_upgrade`). These quests:

- Use `mcrpg:ability_upgrade_next_tier` — upgrades to current tier + 1 rather than a fixed tier.
- Use a formula-based `required-progress` expression (e.g., `"15*(tier^2)"`) that scales with `{tier}` at start time.
- Use the **superset** of all targets from T2 through T5 — the ability's full thematic mob or block list.
- Are **not used for tiers 2–5** in the default config because per-tier overrides in the skill YAML take precedence.
- Exist as a **safety net** for future tiers (6+) or as a simpler starting point for server owners who prefer repeatable quests.

If a tier beyond 5 is added:
1. The generic fallback activates automatically — no new quest definition required.
2. For a curated experience, add a dedicated `*_tierN` quest in the appropriate per-skill file and reference it from the skill config.

---

## Adding Upgrade Quests for a New Ability

1. Add four per-tier quest definitions (`*_tier2` through `*_tier5`) to the appropriate per-skill file (`swords_upgrades.yml`, `mining_upgrades.yml`, etc.). Follow the T2/T3 single-objective and T4/T5 multi-objective patterns above.
2. Add a generic fallback definition (`*_upgrade`) to `generic_ability_upgrades.yml` using `mcrpg:ability_upgrade_next_tier` with a formula-based count and the full target superset.
3. Reference both in the skill YAML config:
   ```yaml
   all-tiers:
     upgrade-quest: "mcrpg:<ability>_upgrade"   # generic fallback
   tier-2:
     upgrade-quest: "mcrpg:<ability>_tier2"     # per-tier override
   tier-3:
     upgrade-quest: "mcrpg:<ability>_tier3"
   tier-4:
     upgrade-quest: "mcrpg:<ability>_tier4"
   tier-5:
     upgrade-quest: "mcrpg:<ability>_tier5"
   ```
4. Choose thematic targets that reinforce the ability's fantasy (see per-ability rationale above as a reference).
5. Use counts consistent with the reference table for the ability's skill group.
