package io.qualitymax.demobank;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class DemoBankRulesTest {
    @Test
    public void acceptedNegativeTransferIsAReproducedCrisis() {
        assertTrue(DemoBankRules.isCrisisReproduced(-100.0, true));
        assertFalse(DemoBankRules.isCrisisReproduced(100.0, true));
    }

    @Test
    public void rejectedNegativeTransferIsAPreventedCrisis() {
        assertTrue(DemoBankRules.isCrisisPrevented(-100.0, 400, 5_420.50, 5_420.50));
        assertFalse(DemoBankRules.isCrisisPrevented(-100.0, 500, 5_420.50, 5_420.50));
        assertFalse(DemoBankRules.isCrisisPrevented(-100.0, 400, 5_420.50, 5_520.50));
    }

    @Test
    public void rejectedTransferWithMutatedBalanceIsIncompleteEvidence() {
        assertTrue(DemoBankRules.isEvidenceIncomplete(
                -100.0,
                false,
                400,
                5_420.50,
                5_520.50
        ));
        assertTrue(DemoBankRules.isEvidenceIncomplete(100.0, false, 400, 5_420.50, 5_420.50));
    }
}
