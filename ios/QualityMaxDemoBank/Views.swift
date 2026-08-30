import SwiftUI

private enum Brand {
    static let background = Color(red: 245 / 255, green: 247 / 255, blue: 250 / 255)
    static let surface = Color.white
    static let raised = Color(red: 243 / 255, green: 244 / 255, blue: 246 / 255)
    static let text = Color(red: 26 / 255, green: 26 / 255, blue: 46 / 255)
    static let muted = Color(red: 102 / 255, green: 102 / 255, blue: 112 / 255)
    static let violet = Color(red: 79 / 255, green: 70 / 255, blue: 229 / 255)
    static let danger = Color(red: 220 / 255, green: 38 / 255, blue: 38 / 255)
    static let success = Color(red: 22 / 255, green: 163 / 255, blue: 74 / 255)
}

struct RootView: View {
    @ObservedObject var model: DemoBankModel

    var body: some View {
        ZStack {
            Brand.background.ignoresSafeArea()
            switch model.screen {
            case .login:
                LoginView(model: model)
            case .dashboard:
                DashboardView(model: model)
            case .transfer:
                TransferView(model: model)
            case .transactions:
                TransactionsView(model: model)
            case .receipt:
                ReceiptView(model: model)
            }
        }
        .tint(Brand.violet)
        .animation(.easeInOut(duration: 0.25), value: model.screen)
    }
}

private struct LoginView: View {
    @ObservedObject var model: DemoBankModel

    var body: some View {
        ScreenScroll(context: "SECURE MOBILE BANKING") {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 38)
                Eyebrow("WELCOME", color: Brand.violet)
                HeroTitle("Sign in to Demo Bank")
                SupportingText("Access your accounts, transfers, and transaction history.")
                Spacer().frame(height: 26)

                BrandCard {
                    VStack(alignment: .leading, spacing: 14) {
                        Text("Secure sign in")
                            .font(.headline.weight(.bold))
                            .foregroundStyle(Brand.text)
                        Eyebrow("DEMO CREDENTIALS ARE PREFILLED", color: Brand.muted)

                        DemoField(label: "USERNAME") {
                            TextField("Username", text: $model.username)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                                .accessibilityIdentifier("login.username")
                        }

                        DemoField(label: "PASSWORD") {
                            SecureField("Password", text: $model.password)
                                .accessibilityIdentifier("login.password")
                        }

                        PrimaryButton(title: model.isLoading ? "Connecting…" : "Sign in to Demo Bank") {
                            model.signIn()
                        }
                        .disabled(model.isLoading)
                        .accessibilityIdentifier("login.submit")

                        if let error = model.errorMessage {
                            Text(error)
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(Brand.danger)
                                .accessibilityIdentifier("login.status")
                        }
                    }
                }

            }
        }
    }
}

private struct DashboardView: View {
    @ObservedObject var model: DemoBankModel

    var body: some View {
        ScreenScroll(context: "PERSONAL BANKING") {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 30)
                Eyebrow("WELCOME BACK", color: Brand.violet)
                HeroTitle("Dashboard")
                SupportingText(model.user.map { "\($0.name) · Your accounts and recent activity" } ?? "Your accounts and recent activity")
                Spacer().frame(height: 22)

                BrandCard(border: Brand.violet) {
                    VStack(alignment: .leading, spacing: 7) {
                        Eyebrow("TOTAL BALANCE", color: Brand.muted)
                        Text(model.totalBalance.demoCurrency)
                            .font(.system(size: 40, weight: .bold, design: .rounded))
                            .foregroundStyle(Brand.text)
                            .accessibilityIdentifier("dashboard.total_balance")
                        Text("Across \(model.accounts.count) connected accounts")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Brand.muted)
                    }
                }
                .padding(.bottom, 12)

                ForEach(model.accounts) { account in
                    AccountCard(account: account)
                        .padding(.bottom, 10)
                }

                HStack(spacing: 10) {
                    PrimaryButton(title: "Send money") {
                        model.openTransfer(qualityMaxTest: false)
                    }
                    SecondaryButton(title: "Transactions") {
                        model.loadTransactions()
                        model.screen = .transactions
                    }
                }
                .accessibilityIdentifier("dashboard.transfer")
                .padding(.top, 8)

                if let notice = model.bankingNotice {
                    Text(notice)
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Brand.success)
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Brand.success.opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
                        .padding(.top, 12)
                        .accessibilityIdentifier("dashboard.status")
                }

                Text("RECENT TRANSACTIONS")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Brand.muted)
                    .padding(.top, 24)
                    .padding(.bottom, 8)
                BrandCard {
                    VStack(spacing: 0) {
                        ForEach(Array(model.transactions.prefix(5).enumerated()), id: \.element.id) { index, transaction in
                            TransactionRow(transaction: transaction)
                            if index < min(model.transactions.count, 5) - 1 {
                                Divider()
                            }
                        }
                    }
                }

                HStack(spacing: 12) {
                    Button("Refresh") { Task { await model.refreshAccounts() } }
                    Spacer()
                    Button("Sign out") { model.signOut() }
                }
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Brand.muted)
                .padding(.top, 18)
            }
        }
    }
}

private struct TransactionsView: View {
    @ObservedObject var model: DemoBankModel
    @State private var search = ""

    var body: some View {
        ScreenScroll(context: "TRANSACTION HISTORY") {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 28)
                HeroTitle("Transactions")
                SupportingText("Activity for \(model.accounts.first?.displayName ?? "your account").")
                HStack(spacing: 8) {
                    TextField("Search transactions", text: $search)
                        .textFieldStyle(.plain)
                        .padding(.horizontal, 14)
                        .frame(height: 46)
                        .background(Brand.surface, in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Brand.muted.opacity(0.25)))
                        .accessibilityIdentifier("transactions.search")
                    Button("Search") { model.loadTransactions(search: search) }
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .frame(height: 46)
                        .background(Brand.violet, in: RoundedRectangle(cornerRadius: 12))
                        .accessibilityIdentifier("transactions.search_submit")
                }
                .padding(.vertical, 18)

                BrandCard {
                    VStack(spacing: 0) {
                        if model.transactions.isEmpty && !model.isLoading {
                            SupportingText("No transactions found.")
                                .padding(.vertical, 20)
                        } else {
                            ForEach(Array(model.transactions.enumerated()), id: \.element.id) { index, transaction in
                                TransactionRow(transaction: transaction)
                                    .accessibilityIdentifier("transactions.row.\(transaction.id)")
                                if index < model.transactions.count - 1 { Divider() }
                            }
                        }
                    }
                }

                SecondaryButton(title: "Back to dashboard") { model.screen = .dashboard }
                    .padding(.top, 16)
            }
        }
    }
}

private struct TransferView: View {
    @ObservedObject var model: DemoBankModel
    @State private var sourceID: Int
    @State private var destinationID: Int
    @State private var amount: String
    @State private var transferDescription: String

    init(model: DemoBankModel) {
        self.model = model
        let first = model.accounts.first?.id ?? 0
        let second = model.accounts.dropFirst().first?.id ?? first
        _sourceID = State(initialValue: first)
        _destinationID = State(initialValue: second)
        _amount = State(initialValue: model.qualityMaxTestMode ? "-100.00" : "250.00")
        _transferDescription = State(initialValue: "Transfer to Savings")
    }

    var body: some View {
        ScreenScroll(context: "MONEY TRANSFER") {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 28)
                Eyebrow("BANKING", color: Brand.violet)
                HeroTitle("Transfer Money")
                SupportingText("Move funds securely between your accounts.")
                Spacer().frame(height: 20)

                BrandCard {
                    VStack(alignment: .leading, spacing: 14) {
                        DemoField(label: "FROM ACCOUNT") {
                            Picker("From account", selection: $sourceID) {
                                ForEach(model.accounts) { account in
                                    Text(account.displayName).tag(account.id)
                                }
                            }
                            .pickerStyle(.menu)
                            .accessibilityIdentifier("transfer.from_account")
                        }

                        DemoField(label: "TO ACCOUNT") {
                            Picker("To account", selection: $destinationID) {
                                ForEach(model.accounts) { account in
                                    Text(account.displayName).tag(account.id)
                                }
                            }
                            .pickerStyle(.menu)
                            .accessibilityIdentifier("transfer.to_account")
                        }

                        DemoField(label: "AMOUNT") {
                            TextField("Amount", text: $amount)
                                .keyboardType(.numbersAndPunctuation)
                                .accessibilityIdentifier("transfer.amount")
                        }

                        DemoField(label: "DESCRIPTION") {
                            TextField("Description", text: $transferDescription)
                                .accessibilityIdentifier("transfer.description")
                        }

                        Button {
                            submit()
                        } label: {
                            Text(model.isLoading ? "Processing…" : "Send Transfer")
                                .font(.headline.weight(.bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(.white)
                        .background(Brand.violet, in: RoundedRectangle(cornerRadius: 15))
                        .disabled(model.isLoading)
                        .accessibilityIdentifier("transfer.submit")

                        if let error = model.errorMessage {
                            Text(error)
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(Brand.danger)
                                .accessibilityIdentifier("transfer.status")
                        }
                    }
                }

                SecondaryButton(title: "Back to accounts") {
                    model.screen = .dashboard
                }
                .padding(.top, 14)
            }
        }
    }

    private func submit() {
        guard
            let source = model.accounts.first(where: { $0.id == sourceID }),
            let destination = model.accounts.first(where: { $0.id == destinationID }),
            source.id != destination.id,
            let numericAmount = Double(amount)
        else {
            model.errorMessage = "Choose two accounts and enter a valid amount."
            return
        }
        model.runTransfer(
            from: source,
            to: destination,
            amount: numericAmount,
            description: transferDescription
        )
    }
}

private struct ReceiptView: View {
    @ObservedObject var model: DemoBankModel

    var body: some View {
        if let receipt = model.receipt {
            let stateColor = receipt.isFailure ? Brand.danger : Brand.success
            ScreenScroll(context: "QUALITYMAX EVIDENCE RECEIPT") {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 34)
                    Eyebrow(receipt.eyebrow, color: stateColor)
                    HeroTitle(receipt.title)
                        .accessibilityIdentifier("receipt.title")
                    SupportingText(receipt.summary)
                    Spacer().frame(height: 22)

                    BrandCard(border: stateColor) {
                        VStack(alignment: .leading, spacing: 0) {
                            EvidenceRow(label: "RESULT", value: receipt.result, color: stateColor)
                                .accessibilityIdentifier("receipt.status")
                            Divider().overlay(Brand.muted.opacity(0.25))
                            EvidenceRow(label: "REQUESTED TRANSFER", value: receipt.requestedAmount.demoCurrency)
                            Divider().overlay(Brand.muted.opacity(0.25))
                            EvidenceRow(label: "BALANCE BEFORE", value: receipt.balanceBefore.demoCurrency)
                                .accessibilityIdentifier("receipt.before_balance")
                            Divider().overlay(Brand.muted.opacity(0.25))
                            EvidenceRow(label: "BALANCE AFTER", value: receipt.balanceAfter.demoCurrency, color: stateColor)
                                .accessibilityIdentifier("receipt.after_balance")
                            Divider().overlay(Brand.muted.opacity(0.25))
                            EvidenceRow(label: "HTTP OUTCOME", value: "\(receipt.statusCode) · \(receipt.detail)", color: Brand.muted)
                        }
                    }

                    SupportingText(receipt.proof)
                        .padding(15)
                        .background(Brand.surface, in: RoundedRectangle(cornerRadius: 16))
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(stateColor.opacity(0.85), lineWidth: 1))
                        .padding(.top, 16)

                    PrimaryButton(title: receipt.isFailure ? "Verify fixed release" : "Re-run verification") {
                        model.rerunTransfer()
                    }
                    .padding(.top, 18)

                    SecondaryButton(title: "Return to accounts") {
                        model.returnToDashboard()
                    }
                    .padding(.top, 10)
                }
            }
        } else {
            ProgressView()
                .tint(Brand.violet)
        }
    }
}

private struct ScreenScroll<Content: View>: View {
    let context: String
    @ViewBuilder let content: Content

    init(context: String, @ViewBuilder content: () -> Content) {
        self.context = context
        self.content = content()
    }

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 0) {
                    HStack(alignment: .center) {
                        Text("DEMO BANK")
                            .font(.subheadline.weight(.black))
                            .tracking(1.5)
                            .foregroundStyle(.white)
                            .layoutPriority(1)
                        Spacer()
                        Text(context)
                            .font(.caption2.weight(.bold))
                            .tracking(0.8)
                            .foregroundStyle(.white.opacity(0.72))
                            .multilineTextAlignment(.trailing)
                            .lineLimit(1)
                            .minimumScaleFactor(0.65)
                            .allowsTightening(true)
                    }
                    .padding(.horizontal, 22)
                    .frame(height: 40)
                    .padding(.top, max(geometry.safeAreaInsets.top - 12, 0))
                    .background(Brand.text)
                    .padding(.horizontal, -22)
                    content
                }
                .padding(.horizontal, 22)
                .padding(.bottom, 36)
            }
            .ignoresSafeArea(edges: .top)
            .scrollDismissesKeyboard(.interactively)
            .background(Brand.background.ignoresSafeArea())
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct TransactionRow: View {
    let transaction: BankTransaction

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill((transaction.isCredit ? Brand.success : Brand.danger).opacity(0.11))
                    .frame(width: 38, height: 38)
                Image(systemName: transaction.isCredit ? "arrow.down.left" : "arrow.up.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(transaction.isCredit ? Brand.success : Brand.danger)
            }
            VStack(alignment: .leading, spacing: 3) {
                Text(transaction.description)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Brand.text)
                    .lineLimit(1)
                Text("\(transaction.date)  ·  \(transaction.category.capitalized)")
                    .font(.caption)
                    .foregroundStyle(Brand.muted)
            }
            Spacer(minLength: 8)
            Text("\(transaction.isCredit ? "+" : "−")\(abs(transaction.amount).demoCurrency)")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(transaction.isCredit ? Brand.success : Brand.text)
        }
        .padding(.vertical, 11)
    }
}

private struct BrandCard<Content: View>: View {
    let border: Color?
    @ViewBuilder let content: Content

    init(border: Color? = nil, @ViewBuilder content: () -> Content) {
        self.border = border
        self.content = content()
    }

    var body: some View {
        content
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Brand.surface, in: RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(border ?? .clear, lineWidth: border == nil ? 0 : 1)
            )
    }
}

private struct DemoField<Content: View>: View {
    let label: String
    @ViewBuilder let content: Content

    init(label: String, @ViewBuilder content: () -> Content) {
        self.label = label
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Eyebrow(label, color: Brand.muted)
            content
                .font(.body.weight(.medium))
                .foregroundStyle(Brand.text)
                .padding(.horizontal, 14)
                .frame(minHeight: 48)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Brand.raised, in: RoundedRectangle(cornerRadius: 13))
                .overlay(RoundedRectangle(cornerRadius: 13).stroke(Brand.muted.opacity(0.3), lineWidth: 1))
        }
    }
}

private struct AccountCard: View {
    let account: BankAccount

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(account.name)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Brand.text)
                Text(account.number ?? "")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Brand.muted)
            }
            Spacer()
            Text(account.balance.demoCurrency)
                .font(.headline.weight(.bold))
                .foregroundStyle(Brand.text)
        }
        .padding(16)
        .background(Brand.surface, in: RoundedRectangle(cornerRadius: 16))
    }
}

private struct EvidenceRow: View {
    let label: String
    let value: String
    var color = Brand.text

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Eyebrow(label, color: Brand.muted)
            Text(value)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(color)
                .lineLimit(3)
        }
        .padding(.vertical, 11)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct Eyebrow: View {
    let value: String
    let color: Color

    init(_ value: String, color: Color) {
        self.value = value
        self.color = color
    }

    var body: some View {
        Text(value)
            .font(.caption2.weight(.black))
            .tracking(1.1)
            .foregroundStyle(color)
    }
}

private struct HeroTitle: View {
    let value: String

    init(_ value: String) {
        self.value = value
    }

    var body: some View {
        Text(value)
            .font(.system(size: 36, weight: .bold, design: .rounded))
            .tracking(-0.7)
            .foregroundStyle(Brand.text)
            .padding(.top, 6)
            .padding(.bottom, 8)
    }
}

private struct SupportingText: View {
    let value: String

    init(_ value: String) {
        self.value = value
    }

    var body: some View {
        Text(value)
            .font(.body)
            .foregroundStyle(Brand.muted)
            .lineSpacing(4)
            .fixedSize(horizontal: false, vertical: true)
    }
}

private struct PrimaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.headline.weight(.bold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
        }
        .buttonStyle(.plain)
        .foregroundStyle(.white)
        .background(Brand.violet, in: RoundedRectangle(cornerRadius: 15))
    }
}

private struct SecondaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.headline.weight(.bold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
        }
        .buttonStyle(.plain)
        .foregroundStyle(Brand.text)
        .background(Brand.raised, in: RoundedRectangle(cornerRadius: 15))
        .overlay(RoundedRectangle(cornerRadius: 15).stroke(Brand.muted.opacity(0.3), lineWidth: 1))
    }
}
