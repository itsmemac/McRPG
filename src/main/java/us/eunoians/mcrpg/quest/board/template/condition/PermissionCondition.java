package us.eunoians.mcrpg.quest.board.template.condition;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.expansion.McRPGExpansion;
import us.eunoians.mcrpg.util.McRPGMethods;

import java.util.Map;
import java.util.Optional;

/**
 * A {@link TemplateCondition} that gates a template element behind a Bukkit permission node.
 * The condition evaluates to {@code true} only when the player identified by the context's
 * {@code playerUUID} is online and holds the specified permission. This enables VIP tiers,
 * role-based quest pools, or server-owner-controlled content unlocks.
 *
 * <p><b>Player-dependent:</b> this condition requires a player UUID in the
 * {@link ConditionContext}. It will always return {@code false} when the UUID is absent
 * (e.g. during shared offering generation via
 * {@link ConditionContext#forTemplateGeneration}) or when the player is offline at
 * generation time. Use this condition only in templates intended for personal offerings
 * where a player context is available via {@link ConditionContext#forPersonalGeneration}.
 *
 * <p><b>YAML — shorthand (recommended):</b>
 * <pre>{@code
 * condition:
 *   permission: mcrpg.quest.legendary
 * }</pre>
 *
 * <p><b>YAML — explicit type:</b>
 * <pre>{@code
 * condition:
 *   type: mcrpg:permission_check
 *   permission: mcrpg.quest.veteran
 * }</pre>
 *
 * <p>Permission nodes are checked via {@link org.bukkit.entity.Player#hasPermission} at
 * generation time, so any permission plugin (LuckPerms, PermissionsEx, etc.) integrates
 * automatically.
 *
 * @see TemplateCondition
 * @see ConditionContext
 */
public final class PermissionCondition implements TemplateCondition {

    public static final NamespacedKey KEY = new NamespacedKey(McRPGMethods.getMcRPGNamespace(), "permission_check");

    private final String permission;

    /**
     * Creates an unconfigured prototype instance for registry registration.
     * The stored permission is a placeholder; this instance is never evaluated.
     * Use {@link #fromConfig} to produce a live, configured condition.
     */
    public PermissionCondition() {
        this.permission = KEY.getKey();
    }

    /**
     * Creates a new permission condition.
     *
     * @param permission the Bukkit permission node to check (e.g. {@code "mcrpg.quest.legendary"})
     * @throws IllegalArgumentException if {@code permission} is blank
     */
    public PermissionCondition(@NotNull String permission) {
        if (permission.isBlank()) {
            throw new IllegalArgumentException("Permission string must not be blank");
        }
        this.permission = permission;
    }

    @Override
    public boolean evaluate(@NotNull ConditionContext context) {
        if (context.playerUUID() == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(context.playerUUID());
        if (player == null) {
            return false;
        }
        return player.hasPermission(permission);
    }

    @NotNull
    @Override
    public NamespacedKey getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public Optional<NamespacedKey> getExpansionKey() {
        return Optional.of(McRPGExpansion.EXPANSION_KEY);
    }

    @NotNull
    @Override
    public TemplateCondition fromConfig(@NotNull Section section, @NotNull ConditionParser parser) {
        String perm = section.getString("permission");
        if (perm == null || perm.isBlank()) {
            throw new IllegalArgumentException("Missing 'permission' in permission_check condition");
        }
        return new PermissionCondition(perm);
    }

    @NotNull
    @Override
    public Map<String, Object> serializeConfig() {
        return Map.of("permission", permission);
    }

    /**
     * Returns the Bukkit permission node this condition tests.
     *
     * @return the permission string
     */
    @NotNull
    public String getPermission() {
        return permission;
    }
}
