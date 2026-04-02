# Board Integration — Integration Guide

This document explains how the quest board system works in McRPG: sources, scopes, rarities, distribution types, the offering lifecycle, events, and how to extend the board with custom integrations.

**Audience:** Plugin developers and AI agents working on board features or third-party integrations.

---

## 1. Overview

The quest board is a rotation-based system that presents quest offerings to players. It supports three offering types:

| Type | Visibility | Key characteristic |
|------|-----------|-------------------|
| **Shared** | Same offerings for all players | Generated from hand-crafted definitions or templates at rotation time |
| **Personal** | Unique per player | Generated deterministically from templates using player-specific seeds |
| **Scoped** | Per scope entity (e.g. land/guild) | Generated for group entities via `ScopedBoardAdapter` |

The single entry point is `QuestBoardManager`, accessed via `McRPGManagerKey.QUEST_BOARD`.

---

## 2. Quest Sources

A `QuestSource` identifies how a quest was started. Sources are stored in the database and determine behavior like whether the quest is abandonable.

**Interface:**

```java
public interface QuestSource extends McRPGContent {
    @NotNull NamespacedKey getKey();
    boolean isAbandonable();
}
```

**Built-in sources:**

| Source | Key | Abandonable | Used for |
|--------|-----|------------|----------|
| `BoardPersonalQuestSource` | `mcrpg:board_personal` | yes | Player accepting a shared board offering |
| `BoardLandQuestSource` | `mcrpg:board_land` | yes | Land/group board quests (reserved) |
| `ManualQuestSource` | `mcrpg:manual` | no | Admin/API started quests |
| `AbilityUpgradeQuestSource` | `mcrpg:ability_upgrade` | no | Ability upgrade quests |

**Registration:** via `QuestSourceContentPack` in a `ContentExpansion`.

---

## 3. Quest Scope Providers

A `QuestScopeProvider` determines the scope of a quest — who participates and how contributions are tracked.

**Built-in providers:**

| Provider | Key | Description |
|----------|-----|-------------|
| `SinglePlayerQuestScopeProvider` | `mcrpg:single_player_scope` | Single player scope — the default |
| `PermissionQuestScopeProvider` | Configurable | Scope based on a Bukkit permission node; uses Vault for offline checks |
| `LandQuestScopeProvider` | Land-specific | Scope based on Lands API membership; registered by `LandsHook` |

**Registration:** via `QuestScopeProviderContentPack` or direct registry registration.

---

## 4. Quest Rarities

Rarities define tiers of quest offerings with different weights, difficulty multipliers, and reward multipliers.

**`QuestRarity` fields:**

| Field | Description |
|-------|-------------|
| `key` | `NamespacedKey` (e.g. `mcrpg:common`) |
| `weight` | Selection weight — lower weight = rarer |
| `difficultyMultiplier` | Scales template variable ranges |
| `rewardMultiplier` | Scales template reward amounts |

Rarities are configured in `quest-board/board.yml` under the `rarities` route and loaded by `ReloadableRarityConfig`. The registry also supports programmatic registration via `QuestRarityContentPack`.

**Rarity rolling:** `QuestRarityRegistry.rollRarity(Random)` performs weighted random selection.

---

## 5. Reward Distribution Types

Distribution types determine which players qualify for a reward tier in scoped quests. See [`REWARDS.md`](REWARDS.md) section 4b for how tiers work.

**Built-in types:**

| Type | Key | Who qualifies |
|------|-----|---------------|
| `TopPlayersDistributionType` | `mcrpg:top_players` | Top N contributors by total contribution |
| `ContributionThresholdDistributionType` | `mcrpg:contribution_threshold` | Players above a minimum contribution percentage |
| `ParticipatedDistributionType` | `mcrpg:participated` | Any player who contributed at least once |
| `MembershipDistributionType` | `mcrpg:membership` | All scope entity members (even non-contributors) |
| `QuestAcceptorDistributionType` | `mcrpg:quest_acceptor` | Only the player who accepted the quest |

**Registration:** via `RewardDistributionTypeContentPack`.

---

## 6. Board Offering Lifecycle

A `BoardOffering` tracks a single quest slot through its lifecycle via a state machine:

```
VISIBLE → ACCEPTED → COMPLETED
                   → EXPIRED
                   → ABANDONED
         → EXPIRED (if rotation ends without acceptance)
```

**Creation flow:**

1. Rotation triggers (scheduled or manual via `triggerRotation`)
2. `QuestPool` selects hand-crafted definitions and/or templates for each slot
3. Templates are generated via `QuestTemplateEngine`
4. `BoardOfferingGenerateEvent` fires (offerings can be modified)
5. Offerings are cached and persisted to DB

**Acceptance flow:**

1. Player clicks offering in GUI → `QuestBoardManager.acceptOffering()`
2. Per-offering lock prevents concurrent accepts
3. Validates: offering is `VISIBLE`, player has available slots
4. `BoardOfferingAcceptEvent` fires (cancellable)
5. Definition resolved (hand-crafted lookup or JSON deserialization)
6. `QuestManager.startQuest()` with `BoardPersonalQuestSource`
7. Offering transitions to `ACCEPTED`

**Expiration:** Previous rotation's offerings are expired when a new rotation saves. Orphaned `ACCEPTED` offerings (missing quest instance) are repaired to `EXPIRED` by `validateOfferingStates()`.

---

## 7. Board Events

| Event | When | Cancellable | Key data |
|-------|------|-------------|----------|
| `BoardRotationEvent` | After rotation completes | no | New `BoardRotation` + offerings list |
| `BoardOfferingGenerateEvent` | After shared offerings generated | no | Modifiable offerings list |
| `PersonalOfferingGenerateEvent` | After personal offerings generated for a player | no | Player UUID + offerings |
| `BoardOfferingAcceptEvent` | Before a player accepts an offering | **yes** | Player + offering |
| `BoardOfferingExpireEvent` | When previous rotation's offerings expire | no | Expired offerings |
| `TemplateQuestGenerateEvent` | After template engine produces a definition | **yes** | Template key + generated definition (cancel drops the slot) |

---

## 8. Board Categories

Board slot categories control how offering slots are allocated across the board:

**`BoardSlotCategory` fields:**

| Field | Description |
|-------|-------------|
| `visibility` | `SHARED`, `PERSONAL`, or `SCOPED` |
| `refreshTypeKey` | Which rotation type owns this category (e.g. `DAILY`, `WEEKLY`) |
| `refreshInterval` | How often this category refreshes |
| `min` / `max` | Slot count range |
| `chancePerSlot` | Probability of each slot being filled |
| `priority` | Higher priority categories fill first |
| `requiredPermission` | Optional permission check for visibility |
| `maxActivePerEntity` | Cap on active quests from this category per scope entity |

Categories are configured in `quest-board/categories.yml` and managed by `BoardSlotCategoryRegistry`.

---

## 9. Quest Pool

`QuestPool` is responsible for selecting which definitions and templates fill each board slot.

**Selection process:**

1. Eligible hand-crafted definitions filtered by `BoardMetadata` (board eligible, supported rarities, refresh type)
2. Eligible templates filtered by `QuestTemplateRegistry.getEligibleTemplates`
3. Per-slot: weighted choice between hand-crafted (`hcWeight`) vs template (`templateWeight`)
4. Rarity rolled via `QuestRarityRegistry.rollRarity()`
5. For templates: `QuestTemplateEngine.generate()` called
6. `TemplateQuestGenerateEvent` fired (can cancel to drop the slot)
7. Duplicate definition keys within a rotation are excluded

**Personal offerings:** Same process but with player-specific `Random` seed (`computeSeed(playerUUID, rotationEpoch, slotIndex)`) and `forPersonalGeneration` condition context.

---

## 10. Scoped Board Adapters

A `ScopedBoardAdapter` bridges group-entity systems (like Lands) to the board:

**Key methods:**

| Method | Purpose |
|--------|---------|
| `getScopeProviderKey()` | Links to the matching `QuestScopeProvider` |
| `getAllActiveEntities()` | Returns all entity IDs that should receive scoped offerings |
| `getEntityDisplayName(entityId)` | Human-readable name for GUI display |
| `getPlayerEntities(playerUUID)` | Which scope entities a player belongs to |

**Built-in:** `LandScopedBoardAdapter` (registered by `LandsHook`).

**Registration:** via `ScopedBoardAdapterContentPack` or direct `ScopedBoardAdapterRegistry.register()`.

---

## 11. Extending the Board

### Adding a custom quest source

```java
public class NpcQuestSource implements QuestSource {
    public static final NamespacedKey KEY = new NamespacedKey("myplugin", "npc");

    @NotNull @Override public NamespacedKey getKey() { return KEY; }
    @Override public boolean isAbandonable() { return true; }
    @NotNull @Override
    public Optional<NamespacedKey> getExpansionKey() {
        return Optional.of(MyExpansion.EXPANSION_KEY);
    }
}

// In your ContentExpansion:
QuestSourceContentPack pack = new QuestSourceContentPack(this);
pack.addContent(new NpcQuestSource());
```

### Adding a custom distribution type

```java
public class GuildRankDistributionType implements RewardDistributionType {
    public static final NamespacedKey KEY = new NamespacedKey("myplugin", "guild_officers");

    @NotNull @Override public NamespacedKey getKey() { return KEY; }

    @NotNull @Override
    public Set<UUID> resolve(@NotNull ContributionSnapshot snapshot,
                             @NotNull DistributionTierConfig tier) {
        // Return UUIDs of qualifying guild officers
        return snapshot.groupMembers().stream()
                .filter(uuid -> MyGuildAPI.isOfficer(uuid))
                .collect(Collectors.toSet());
    }

    @NotNull @Override
    public Optional<NamespacedKey> getExpansionKey() {
        return Optional.of(MyExpansion.EXPANSION_KEY);
    }
}

// In your ContentExpansion:
RewardDistributionTypeContentPack pack = new RewardDistributionTypeContentPack(this);
pack.addContent(new GuildRankDistributionType());
```

### Adding group board support

To add board offerings for a new group system (e.g. guilds):

1. Implement `QuestScopeProvider` for your group system
2. Implement `ScopedBoardAdapter` with matching `getScopeProviderKey()`
3. Register both via content packs or direct registry calls
4. Add a `SCOPED` category in `categories.yml` referencing your scope provider

---

## 12. Configuration Files

| File | Purpose |
|------|---------|
| `quest-board/board.yml` | Board settings, rarity definitions, rotation config, source weights |
| `quest-board/categories.yml` | Board slot category definitions |
| `quest-board/templates/*.yml` | Quest template definitions |
| `quests/**/*.yml` | Hand-crafted quest definitions (board-eligible via `board-metadata`) |

---

## Key Files Reference

| File | Purpose |
|------|---------|
| [`QuestBoardManager`](board/QuestBoardManager.java) | Central board orchestrator |
| [`QuestBoard`](board/QuestBoard.java) | Per-board config and rotation state |
| [`BoardOffering`](board/BoardOffering.java) | Offering state machine |
| [`BoardRotation`](board/BoardRotation.java) | Rotation metadata |
| [`BoardMetadata`](board/BoardMetadata.java) | Quest-level board config |
| [`QuestPool`](board/generation/QuestPool.java) | Template/definition selection |
| [`PersonalOfferingGenerator`](board/generation/PersonalOfferingGenerator.java) | Per-player offering generation |
| [`QuestSource`](source/QuestSource.java) | Source interface |
| [`QuestSourceRegistry`](source/QuestSourceRegistry.java) | Source registry |
| [`QuestScopeProvider`](impl/scope/QuestScopeProvider.java) | Scope provider abstract class |
| [`QuestScopeProviderRegistry`](impl/scope/QuestScopeProviderRegistry.java) | Scope provider registry |
| [`ScopedBoardAdapter`](board/scope/ScopedBoardAdapter.java) | Group entity bridge |
| [`QuestRarity`](board/rarity/QuestRarity.java) | Rarity definition |
| [`QuestRarityRegistry`](board/rarity/QuestRarityRegistry.java) | Rarity registry + rolling |
| [`RewardDistributionType`](board/distribution/RewardDistributionType.java) | Distribution type interface |
| [`RewardDistributionTypeRegistry`](board/distribution/RewardDistributionTypeRegistry.java) | Distribution type registry |
| [`BoardSlotCategory`](board/category/BoardSlotCategory.java) | Slot category definition |
| [`BoardSlotCategoryRegistry`](board/category/BoardSlotCategoryRegistry.java) | Category registry |
| [`QuestBoardGui`](../gui/board/QuestBoardGui.java) | Board GUI |
