package us.eunoians.mcrpg.quest.board.template.condition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import us.eunoians.mcrpg.McRPGBaseTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionConditionTest extends McRPGBaseTest {

    @Test
    @DisplayName("online player with permission returns true")
    void playerWithPermissionReturnsTrue() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(mcRPG, "mcrpg.title.hero", true);

        PermissionCondition condition = new PermissionCondition("mcrpg.title.hero");
        ConditionContext ctx = new ConditionContext(null, null, null, null, player.getUniqueId(), null);
        assertTrue(condition.evaluate(ctx));
    }

    @Test
    @DisplayName("online player without permission returns false")
    void playerWithoutPermissionReturnsFalse() {
        PlayerMock player = server.addPlayer();

        PermissionCondition condition = new PermissionCondition("mcrpg.title.hero");
        ConditionContext ctx = new ConditionContext(null, null, null, null, player.getUniqueId(), null);
        assertFalse(condition.evaluate(ctx));
    }

    @Test
    @DisplayName("null player UUID in context returns false (safe default)")
    void nullPlayerUUIDReturnsFalse() {
        PermissionCondition condition = new PermissionCondition("mcrpg.title.hero");
        ConditionContext ctx = new ConditionContext(null, null, null, null, null, null);
        assertFalse(condition.evaluate(ctx));
    }

    @Test
    @DisplayName("offline player (UUID present but not online) returns false")
    void offlinePlayerReturnsFalse() {
        UUID offlineUUID = UUID.randomUUID();
        PermissionCondition condition = new PermissionCondition("mcrpg.title.hero");
        ConditionContext ctx = new ConditionContext(null, null, null, null, offlineUUID, null);
        assertFalse(condition.evaluate(ctx));
    }

    @Test
    @DisplayName("blank permission string throws IllegalArgumentException at construction")
    void blankPermissionThrows() {
        assertThrows(IllegalArgumentException.class, () -> new PermissionCondition(""));
    }

    @Test
    @DisplayName("whitespace-only permission string throws IllegalArgumentException at construction")
    void whitespacePermissionThrows() {
        assertThrows(IllegalArgumentException.class, () -> new PermissionCondition("   "));
    }
}
