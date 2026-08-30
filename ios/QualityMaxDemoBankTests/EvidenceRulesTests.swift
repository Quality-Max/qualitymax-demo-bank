import XCTest
@testable import QualityMaxDemoBank

final class EvidenceRulesTests: XCTestCase {
    func testAcceptedNegativeTransferReproducesVulnerability() {
        XCTAssertEqual(
            EvidenceRules.outcome(
                amount: -100,
                accepted: true,
                statusCode: 200,
                balanceBefore: 5_420.50,
                balanceAfter: 5_520.50
            ),
            .vulnerabilityReproduced
        )
    }

    func testRejectedNegativeTransferPreventsCrisis() {
        XCTAssertEqual(
            EvidenceRules.outcome(
                amount: -100,
                accepted: false,
                statusCode: 400,
                balanceBefore: 5_420.50,
                balanceAfter: 5_420.50
            ),
            .crisisPrevented
        )
    }

    func testRejectedTransferWithMutatedBalanceIsIncompleteEvidence() {
        XCTAssertEqual(
            EvidenceRules.outcome(
                amount: -100,
                accepted: false,
                statusCode: 400,
                balanceBefore: 5_420.50,
                balanceAfter: 5_520.50
            ),
            .evidenceIncomplete
        )
        XCTAssertEqual(
            EvidenceRules.outcome(
                amount: 100,
                accepted: false,
                statusCode: 400,
                balanceBefore: 5_420.50,
                balanceAfter: 5_420.50
            ),
            .evidenceIncomplete
        )
    }
}
