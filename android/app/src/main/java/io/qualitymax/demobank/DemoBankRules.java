package io.qualitymax.demobank;

final class DemoBankRules {
    private static final double BALANCE_TOLERANCE = 0.005;

    private DemoBankRules() {}

    static boolean isCrisisReproduced(double requestedAmount, boolean accepted) {
        return requestedAmount <= 0 && accepted;
    }

    static boolean isCrisisPrevented(
            double requestedAmount,
            int statusCode,
            double balanceBefore,
            double balanceAfter
    ) {
        return requestedAmount <= 0
                && statusCode >= 400
                && statusCode < 500
                && balancesMatch(balanceBefore, balanceAfter);
    }

    static boolean isEvidenceIncomplete(
            double requestedAmount,
            boolean accepted,
            int statusCode,
            double balanceBefore,
            double balanceAfter
    ) {
        if (requestedAmount <= 0) {
            return !isCrisisReproduced(requestedAmount, accepted)
                    && !isCrisisPrevented(requestedAmount, statusCode, balanceBefore, balanceAfter);
        }
        return !accepted || statusCode < 200 || statusCode >= 300;
    }

    private static boolean balancesMatch(double balanceBefore, double balanceAfter) {
        return Math.abs(balanceBefore - balanceAfter) < BALANCE_TOLERANCE;
    }
}
