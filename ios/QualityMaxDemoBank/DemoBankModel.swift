import Combine
import Foundation

@MainActor
final class DemoBankModel: ObservableObject {
    enum Screen {
        case login
        case dashboard
        case transfer
        case transactions
        case receipt
    }

    @Published var screen: Screen = .login
    @Published var username = "demo"
    @Published var password = "demo123"
    @Published var user: SessionUser?
    @Published var accounts: [BankAccount] = []
    @Published var transactions: [BankTransaction] = []
    @Published var backendMode = "connecting"
    @Published var qualityMaxTestMode = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var bankingNotice: String?
    @Published var receipt: EvidenceReceipt?

    let baseURLString = DemoBankAPI.configuredBaseURL
    private let api: DemoBankAPI

    init(api: DemoBankAPI? = nil) {
        self.api = api ?? DemoBankAPI()
        let arguments = ProcessInfo.processInfo.arguments
        let captureScreen: Screen?
        if arguments.contains("--qualitymax-capture-dashboard") {
            captureScreen = .dashboard
        } else if arguments.contains("--qualitymax-capture-transfer") {
            captureScreen = .transfer
        } else if arguments.contains("--qualitymax-capture-crisis") {
            captureScreen = .receipt
        } else {
            captureScreen = nil
        }

        if let captureScreen {
            Task { [weak self] in
                await self?.prepareCapture(screen: captureScreen)
            }
        }
    }

    var totalBalance: Double {
        accounts.reduce(0) { $0 + $1.balance }
    }

    func signIn() {
        isLoading = true
        errorMessage = nil
        Task {
            do {
                async let signedInUser = api.login(username: username, password: password)
                let loadedUser = try await signedInUser
                let loadedAccounts = try await api.accounts()
                let mode = await api.backendMode()
                user = loadedUser
                accounts = loadedAccounts
                if let firstAccount = loadedAccounts.first {
                    transactions = try await api.transactions(accountID: firstAccount.id)
                }
                backendMode = mode
                screen = .dashboard
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }

    func refreshAccounts() async {
        do {
            accounts = try await api.accounts()
            if let firstAccount = accounts.first {
                transactions = try await api.transactions(accountID: firstAccount.id)
            }
            backendMode = await api.backendMode()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func openTransfer(qualityMaxTest: Bool) {
        qualityMaxTestMode = qualityMaxTest
        errorMessage = nil
        bankingNotice = nil
        screen = .transfer
    }

    func loadTransactions(search: String = "") {
        guard let accountID = accounts.first?.id else { return }
        isLoading = true
        errorMessage = nil
        Task {
            do {
                transactions = try await api.transactions(accountID: accountID, search: search)
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }

    func signOut() {
        user = nil
        accounts = []
        transactions = []
        receipt = nil
        errorMessage = nil
        bankingNotice = nil
        screen = .login
    }

    func runTransfer(from source: BankAccount, to destination: BankAccount, amount: Double, description: String) {
        isLoading = true
        errorMessage = nil
        let balanceBefore = source.balance

        Task {
            do {
                _ = try await api.transfer(
                    from: source.id,
                    to: destination.id,
                    amount: amount,
                    description: description
                )
            } catch let error as DemoBankAPIError {
                if amount > 0 {
                    errorMessage = error.localizedDescription
                    isLoading = false
                    return
                }
                if let statusCode = error.statusCode {
                    await verifyRejectedTransfer(
                        source: source,
                        amount: amount,
                        balanceBefore: balanceBefore,
                        statusCode: statusCode,
                        detail: error.localizedDescription
                    )
                } else {
                    errorMessage = "Transfer request failed: \(error.localizedDescription)"
                }
                isLoading = false
                return
            } catch {
                errorMessage = "Transfer request failed: \(error.localizedDescription)"
                isLoading = false
                return
            }

            do {
                let balanceAfter = try await refreshedBalance(for: source.id)
                if amount > 0 {
                    if let firstAccount = accounts.first {
                        transactions = try await api.transactions(accountID: firstAccount.id)
                    }
                    bankingNotice = "Transfer completed successfully."
                    screen = .dashboard
                    isLoading = false
                    return
                }
                receipt = EvidenceReceipt(
                    outcome: EvidenceRules.outcome(
                        amount: amount,
                        accepted: true,
                        statusCode: 200,
                        balanceBefore: balanceBefore,
                        balanceAfter: balanceAfter
                    ),
                    requestedAmount: amount,
                    balanceBefore: balanceBefore,
                    balanceAfter: balanceAfter,
                    statusCode: 200,
                    detail: "Transfer accepted; balance independently re-read"
                )
                screen = .receipt
            } catch {
                errorMessage = "Transfer was accepted, but balance verification failed: \(error.localizedDescription)"
            }
            isLoading = false
        }
    }

    private func verifyRejectedTransfer(
        source: BankAccount,
        amount: Double,
        balanceBefore: Double,
        statusCode: Int,
        detail: String
    ) async {
        do {
            let balanceAfter = try await refreshedBalance(for: source.id)
            receipt = EvidenceReceipt(
                outcome: EvidenceRules.outcome(
                    amount: amount,
                    accepted: false,
                    statusCode: statusCode,
                    balanceBefore: balanceBefore,
                    balanceAfter: balanceAfter
                ),
                requestedAmount: amount,
                balanceBefore: balanceBefore,
                balanceAfter: balanceAfter,
                statusCode: statusCode,
                detail: detail
            )
            screen = .receipt
        } catch {
            errorMessage = "Transfer was rejected, but balance verification failed: \(error.localizedDescription)"
        }
    }

    private func refreshedBalance(for accountID: Int) async throws -> Double {
        let refreshedAccounts = try await api.accounts()
        guard let balance = refreshedAccounts.first(where: { $0.id == accountID })?.balance else {
            throw DemoBankAPIError.invalidResponse
        }
        accounts = refreshedAccounts
        return balance
    }

    func returnToDashboard() {
        Task {
            await refreshAccounts()
            screen = .dashboard
        }
    }

    func rerunTransfer() {
        Task {
            await refreshAccounts()
            openTransfer(qualityMaxTest: true)
        }
    }

    private func prepareCapture(screen destination: Screen) async {
        isLoading = true
        errorMessage = nil
        do {
            user = try await api.login(username: username, password: password)
            accounts = try await api.accounts()
            if let firstAccount = accounts.first {
                transactions = try await api.transactions(accountID: firstAccount.id)
            }
            backendMode = await api.backendMode()

            if destination == .receipt, accounts.count >= 2 {
                qualityMaxTestMode = true
                runTransfer(
                    from: accounts[0],
                    to: accounts[1],
                    amount: -100,
                    description: "QualityMax crisis-prevention test"
                )
            } else {
                qualityMaxTestMode = destination == .transfer
                screen = destination
                isLoading = false
            }
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
        }
    }
}
