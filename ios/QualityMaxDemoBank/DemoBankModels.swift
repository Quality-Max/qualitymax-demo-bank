import Foundation

struct SessionUser: Codable, Sendable {
    let id: Int?
    let name: String
    let email: String?
}

struct LoginResponse: Codable, Sendable {
    let success: Bool
    let user: SessionUser
}

struct BankAccount: Codable, Identifiable, Hashable, Sendable {
    let id: Int
    let userId: Int?
    let name: String
    let balance: Double
    let type: String?
    let number: String?

    var displayName: String {
        [name, number].compactMap { $0 }.joined(separator: "  ")
    }
}

struct AccountsResponse: Codable, Sendable {
    let accounts: [BankAccount]
}

struct BankTransaction: Codable, Identifiable, Hashable, Sendable {
    let id: Int
    let accountId: Int
    let type: String
    let amount: Double
    let description: String
    let date: String
    let category: String

    enum CodingKeys: String, CodingKey {
        case id
        case accountId
        case type
        case amount
        case description
        case date
        case category
    }

    var isCredit: Bool { type == "credit" }
}

struct TransactionsResponse: Codable, Sendable {
    let transactions: [BankTransaction]
    let total: Int
}

struct TransferTransaction: Codable, Sendable {
    let id: Int?
    let accountId: Int?
    let type: String?
    let amount: Double?
}

struct TransferResponse: Codable, Sendable {
    let success: Bool
    let transaction: TransferTransaction?
    let newBalance: Double

    enum CodingKeys: String, CodingKey {
        case success
        case transaction
        case newBalance = "new_balance"
    }
}

struct HealthResponse: Codable, Sendable {
    let status: String?
    let mode: String?
}

struct ErrorResponse: Codable, Sendable {
    let error: String
}

struct EvidenceReceipt: Equatable, Sendable {
    enum Outcome: Equatable, Sendable {
        case vulnerabilityReproduced
        case crisisPrevented
        case evidenceIncomplete
        case transferVerified
    }

    let outcome: Outcome
    let requestedAmount: Double
    let balanceBefore: Double
    let balanceAfter: Double
    let statusCode: Int
    let detail: String

    var isFailure: Bool {
        outcome == .vulnerabilityReproduced || outcome == .evidenceIncomplete
    }

    var eyebrow: String {
        switch outcome {
        case .vulnerabilityReproduced:
            return "CRITICAL FINDING"
        case .crisisPrevented:
            return "VERIFIED PROTECTION"
        case .evidenceIncomplete:
            return "EVIDENCE INCOMPLETE"
        case .transferVerified:
            return "EXPECTED BEHAVIOR"
        }
    }

    var title: String {
        switch outcome {
        case .vulnerabilityReproduced:
            return "Vulnerability reproduced."
        case .crisisPrevented:
            return "Crisis prevented."
        case .evidenceIncomplete:
            return "Protection not verified."
        case .transferVerified:
            return "Transfer verified."
        }
    }

    var result: String {
        switch outcome {
        case .vulnerabilityReproduced:
            return "FAIL · APPLICATION DEFECT"
        case .crisisPrevented:
            return "PASS · VERIFIED BY QUALITYMAX"
        case .evidenceIncomplete:
            return "FAIL · EVIDENCE INCOMPLETE"
        case .transferVerified:
            return "PASS · EXPECTED BEHAVIOR"
        }
    }

    var summary: String {
        switch outcome {
        case .vulnerabilityReproduced:
            if abs(balanceBefore - balanceAfter) >= 0.005 {
                return "The backend accepted a negative transfer and changed the source balance."
            }
            return "The backend accepted a forbidden negative transfer; the fresh balance read confirmed its final state."
        case .crisisPrevented:
            return "The backend rejected the dangerous transfer and a fresh balance read confirmed no change."
        case .evidenceIncomplete:
            if requestedAmount <= 0 {
                return "The response did not prove that the dangerous transfer left the balance unchanged."
            }
            return "The transfer outcome did not match the expected successful behavior."
        case .transferVerified:
            return "The backend completed the transfer with independently verified account evidence."
        }
    }

    var proof: String {
        switch outcome {
        case .vulnerabilityReproduced:
            if abs(balanceBefore - balanceAfter) >= 0.005 {
                return "QualityMax captured the exact request, response and verified balance mutation needed to block this release."
            }
            return "QualityMax captured the accepted forbidden request and independently verified the resulting balance."
        case .crisisPrevented:
            return "QualityMax independently verified that the fixed release rejects this exploit without mutating the balance."
        case .evidenceIncomplete:
            return "QualityMax will not issue a passing receipt until the response and refreshed balance agree."
        case .transferVerified:
            return "QualityMax verified the response against a fresh read from the account backend."
        }
    }
}

enum EvidenceRules {
    private static let balanceTolerance = 0.005

    static func outcome(
        amount: Double,
        accepted: Bool,
        statusCode: Int,
        balanceBefore: Double,
        balanceAfter: Double
    ) -> EvidenceReceipt.Outcome {
        if amount <= 0, accepted {
            return .vulnerabilityReproduced
        }
        if amount <= 0,
           (400..<500).contains(statusCode),
           abs(balanceBefore - balanceAfter) < balanceTolerance
        {
            return .crisisPrevented
        }
        if amount <= 0 {
            return .evidenceIncomplete
        }
        return accepted && (200..<300).contains(statusCode) ? .transferVerified : .evidenceIncomplete
    }
}

extension Double {
    var demoCurrency: String {
        formatted(
            .currency(code: "USD")
                .precision(.fractionLength(2))
                .locale(Locale(identifier: "en_US"))
        )
    }
}
