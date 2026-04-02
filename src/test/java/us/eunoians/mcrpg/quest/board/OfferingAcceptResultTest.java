package us.eunoians.mcrpg.quest.board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.eunoians.mcrpg.McRPGBaseTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OfferingAcceptResultTest extends McRPGBaseTest {

    @DisplayName("ACCEPTED is the only result that isAccepted() returns true for")
    @Test
    public void accepted_isAccepted() {
        assertTrue(OfferingAcceptResult.ACCEPTED.isAccepted());
    }

    @DisplayName("QUEST_START_FAILED is not an accepted result")
    @Test
    public void questStartFailed_isNotAccepted() {
        assertFalse(OfferingAcceptResult.QUEST_START_FAILED.isAccepted());
    }

    @DisplayName("All non-ACCEPTED variants return false for isAccepted()")
    @Test
    public void allNonAcceptedVariants_returnFalseForIsAccepted() {
        for (OfferingAcceptResult result : OfferingAcceptResult.values()) {
            if (result != OfferingAcceptResult.ACCEPTED) {
                assertFalse(result.isAccepted(),
                        result.name() + " should not be accepted");
            }
        }
    }
}
