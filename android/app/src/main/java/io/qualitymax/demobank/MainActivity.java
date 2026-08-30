package io.qualitymax.demobank;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(245, 247, 250);
    private static final int SURFACE = Color.WHITE;
    private static final int SURFACE_RAISED = Color.rgb(243, 244, 246);
    private static final int TEXT = Color.rgb(26, 26, 46);
    private static final int MUTED = Color.rgb(102, 102, 112);
    private static final int NAVY = Color.rgb(26, 26, 46);
    private static final int VIOLET = Color.rgb(79, 70, 229);
    private static final int DANGER = Color.rgb(220, 38, 38);
    private static final int SUCCESS = Color.rgb(22, 163, 74);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
    private DemoBankApi api;
    private DemoBankModels.SessionUser currentUser;
    private DemoBankModels.Dashboard dashboard;
    private String dashboardMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        api = new DemoBankApi(BuildConfig.DEMO_BANK_BASE_URL);
        showLogin();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void showLogin() {
        LinearLayout content = screen();
        brandHeader(content, "NATIVE EVIDENCE CLIENT");
        content.addView(spacer(36));
        content.addView(eyebrow("QUALITYMAX DEMO BANK", VIOLET));
        content.addView(title("Banking, verified."));
        content.addView(body("A real native client for proving that dangerous releases are stopped before customers find them."));
        content.addView(spacer(26));

        LinearLayout card = card();
        card.addView(sectionTitle("Secure sign in"));
        card.addView(caption("DEMO CREDENTIALS ARE PREFILLED"));
        card.addView(spacer(18));

        EditText username = input("Username", "demo", InputType.TYPE_CLASS_TEXT);
        username.setId(R.id.login_username);
        username.setContentDescription("login.username");
        card.addView(username);
        card.addView(spacer(12));

        EditText password = input(
                "Password",
                "demo123",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        password.setId(R.id.login_password);
        password.setContentDescription("login.password");
        card.addView(password);
        card.addView(spacer(18));

        Button signIn = primaryButton("Sign in to Demo Bank");
        signIn.setId(R.id.login_submit);
        signIn.setContentDescription("login.submit");
        card.addView(signIn);

        TextView status = statusText();
        status.setId(R.id.login_status);
        status.setContentDescription("login.status");
        card.addView(status);
        content.addView(card);
        content.addView(spacer(18));
        content.addView(backendCaption());

        signIn.setOnClickListener(view -> {
            hideKeyboard();
            signIn.setEnabled(false);
            signIn.setText("Connecting…");
            status.setText("");
            String requestedUsername = username.getText().toString();
            String requestedPassword = password.getText().toString();
            executor.execute(() -> {
                try {
                    DemoBankModels.SessionUser user = api.login(
                            requestedUsername,
                            requestedPassword
                    );
                    DemoBankModels.Dashboard loaded = api.loadDashboard();
                    runOnUiThread(() -> {
                        currentUser = user;
                        dashboard = loaded;
                        showDashboard();
                    });
                } catch (Exception error) {
                    runOnUiThread(() -> {
                        signIn.setEnabled(true);
                        signIn.setText("Sign in to Demo Bank");
                        showError(status, error.getMessage());
                    });
                }
            });
        });

        setContentView(wrap(content));
    }

    private void showDashboard() {
        LinearLayout content = screen();
        brandHeader(content, modeLabel());
        content.addView(spacer(30));
        content.addView(eyebrow("WELCOME BACK", VIOLET));
        content.addView(title(currentUser == null ? "Your accounts" : currentUser.name));
        content.addView(body("Live balances from the shared Demo Bank backend."));
        content.addView(spacer(22));

        LinearLayout totalCard = cardWithBorder(VIOLET);
        totalCard.addView(caption("TOTAL BALANCE"));
        TextView total = amountText(currency.format(dashboard.totalBalance()));
        total.setId(R.id.dashboard_total_balance);
        total.setContentDescription("dashboard.total_balance");
        totalCard.addView(total);
        totalCard.addView(caption("Across " + dashboard.accounts.size() + " connected accounts"));
        content.addView(totalCard);
        content.addView(spacer(14));

        for (DemoBankModels.Account account : dashboard.accounts) {
            LinearLayout accountCard = compactCard();
            LinearLayout row = horizontal();
            LinearLayout names = new LinearLayout(this);
            names.setOrientation(LinearLayout.VERTICAL);
            names.addView(sectionTitle(account.name));
            names.addView(caption(account.number));
            row.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView balance = sectionTitle(currency.format(account.balance));
            balance.setGravity(Gravity.END);
            row.addView(balance);
            accountCard.addView(row);
            content.addView(accountCard);
            content.addView(spacer(10));
        }

        content.addView(spacer(10));
        Button transfer = primaryButton("Send money");
        transfer.setId(R.id.dashboard_transfer);
        transfer.setContentDescription("dashboard.transfer");
        transfer.setOnClickListener(view -> showTransfer(false));
        content.addView(transfer);

        content.addView(spacer(10));
        Button history = secondaryButton("View transaction history");
        history.setId(R.id.dashboard_transactions);
        history.setContentDescription("dashboard.transactions");
        history.setOnClickListener(view -> showTransactions(""));
        content.addView(history);

        TextView status = statusText();
        status.setId(R.id.dashboard_status);
        status.setContentDescription("dashboard.status");
        if (dashboardMessage != null) {
            status.setText(dashboardMessage);
            status.setTextColor(SUCCESS);
            dashboardMessage = null;
        }
        content.addView(status);

        content.addView(spacer(18));
        content.addView(eyebrow("RECENT TRANSACTIONS", MUTED));
        content.addView(spacer(8));
        LinearLayout recent = card();
        int recentCount = Math.min(5, dashboard.transactions.size());
        if (recentCount == 0) {
            recent.addView(body("No transactions yet."));
        }
        for (int index = 0; index < recentCount; index++) {
            recent.addView(transactionRow(dashboard.transactions.get(index)));
            if (index < recentCount - 1) {
                recent.addView(divider());
            }
        }
        content.addView(recent);

        content.addView(spacer(14));
        TextView proof = body("QUALITYMAX SAFETY TEST\nAttempt the negative-transfer exploit before this release reaches customers.");
        proof.setTextColor(VIOLET);
        proof.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        proof.setBackground(rounded(Color.rgb(238, 237, 255), VIOLET, 1));
        proof.setPadding(dp(16), dp(14), dp(16), dp(14));
        proof.setId(R.id.dashboard_qualitymax_test);
        proof.setContentDescription("dashboard.qualitymax_test");
        proof.setOnClickListener(view -> showTransfer(true));
        content.addView(proof);

        content.addView(spacer(14));
        Button signOut = secondaryButton("Sign out");
        signOut.setOnClickListener(view -> {
            currentUser = null;
            dashboard = null;
            showLogin();
        });
        content.addView(signOut);
        setContentView(wrap(content));
    }

    private void showTransactions(String initialSearch) {
        if (dashboard == null || dashboard.accounts.isEmpty()) {
            showDashboard();
            return;
        }
        renderTransactions(initialSearch, dashboard.transactions);
    }

    private void renderTransactions(String initialSearch, List<DemoBankModels.Transaction> transactions) {
        LinearLayout content = screen();
        brandHeader(content, "TRANSACTION HISTORY");
        content.addView(spacer(28));
        content.addView(title("Transactions"));
        content.addView(body("Activity for " + dashboard.accounts.get(0).displayName() + "."));
        content.addView(spacer(18));

        LinearLayout searchCard = card();
        EditText search = input("Search transactions", initialSearch, InputType.TYPE_CLASS_TEXT);
        search.setId(R.id.transactions_search);
        search.setContentDescription("transactions.search");
        searchCard.addView(search);
        searchCard.addView(spacer(10));
        Button searchButton = primaryButton("Search");
        searchButton.setId(R.id.transactions_search_submit);
        searchButton.setContentDescription("transactions.search_submit");
        searchCard.addView(searchButton);
        TextView status = statusText();
        searchCard.addView(status);
        content.addView(searchCard);
        content.addView(spacer(14));

        LinearLayout history = card();
        if (transactions.isEmpty()) {
            history.addView(body("No transactions found."));
        }
        for (int index = 0; index < transactions.size(); index++) {
            history.addView(transactionRow(transactions.get(index)));
            if (index < transactions.size() - 1) {
                history.addView(divider());
            }
        }
        content.addView(history);
        content.addView(spacer(14));
        Button back = secondaryButton("Back to dashboard");
        back.setOnClickListener(view -> showDashboard());
        content.addView(back);

        searchButton.setOnClickListener(view -> {
            hideKeyboard();
            String query = search.getText().toString();
            searchButton.setEnabled(false);
            searchButton.setText("Searching…");
            executor.execute(() -> {
                try {
                    List<DemoBankModels.Transaction> results = api.loadTransactions(
                            dashboard.accounts.get(0).id,
                            query
                    );
                    runOnUiThread(() -> renderTransactions(query, results));
                } catch (Exception error) {
                    runOnUiThread(() -> {
                        searchButton.setEnabled(true);
                        searchButton.setText("Search");
                        showError(status, error.getMessage());
                    });
                }
            });
        });

        setContentView(wrap(content));
    }

    private void showTransfer() {
        showTransfer(true);
    }

    private void showTransfer(boolean qualityMaxTest) {
        if (dashboard == null || dashboard.accounts.size() < 2) {
            showDashboard();
            return;
        }

        LinearLayout content = screen();
        brandHeader(content, qualityMaxTest ? "QUALITYMAX SAFETY TEST" : "MONEY TRANSFER");
        content.addView(spacer(28));
        content.addView(eyebrow(qualityMaxTest ? "HIGH-RISK JOURNEY" : "BANKING", qualityMaxTest ? DANGER : VIOLET));
        content.addView(title("Transfer Money"));
        content.addView(body(qualityMaxTest
                ? "QualityMax is testing the rule ordinary happy-path automation misses."
                : "Move funds securely between your accounts."));
        content.addView(spacer(20));

        if (qualityMaxTest) {
            TextView warning = body("TEST ASSERTION\nNegative transfers must be rejected and the source balance must not increase.");
            warning.setTextColor(DANGER);
            warning.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            warning.setBackground(rounded(Color.rgb(254, 242, 242), DANGER, 1));
            warning.setPadding(dp(16), dp(14), dp(16), dp(14));
            content.addView(warning);
            content.addView(spacer(16));
        }

        LinearLayout card = card();
        List<String> accountLabels = new ArrayList<>();
        for (DemoBankModels.Account account : dashboard.accounts) {
            accountLabels.add(account.displayName());
        }

        card.addView(fieldLabel("FROM ACCOUNT"));
        Spinner from = accountSpinner(accountLabels);
        from.setId(R.id.transfer_from_account);
        from.setContentDescription("transfer.from_account");
        card.addView(from);
        card.addView(spacer(14));

        card.addView(fieldLabel("TO ACCOUNT"));
        Spinner to = accountSpinner(accountLabels);
        to.setSelection(1);
        to.setId(R.id.transfer_to_account);
        to.setContentDescription("transfer.to_account");
        card.addView(to);
        card.addView(spacer(14));

        card.addView(fieldLabel("AMOUNT"));
        EditText amount = input(
                "Amount",
                qualityMaxTest ? "-100.00" : "250.00",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED
        );
        amount.setId(R.id.transfer_amount);
        amount.setContentDescription("transfer.amount");
        card.addView(amount);
        if (!qualityMaxTest) {
            TextView useNegativeAmount = caption("Use QualityMax test amount (−$100)");
            useNegativeAmount.setTextColor(VIOLET);
            useNegativeAmount.setPadding(0, dp(10), 0, 0);
            useNegativeAmount.setClickable(true);
            useNegativeAmount.setFocusable(true);
            useNegativeAmount.setContentDescription("transfer.use_negative_amount");
            useNegativeAmount.setOnClickListener(view -> amount.setText("-100.00"));
            card.addView(useNegativeAmount);
        }
        card.addView(spacer(14));

        card.addView(fieldLabel("DESCRIPTION"));
        EditText description = input(
                "Description",
                qualityMaxTest ? "QualityMax crisis-prevention test" : "Transfer to Savings",
                InputType.TYPE_CLASS_TEXT
        );
        description.setId(R.id.transfer_description);
        description.setContentDescription("transfer.description");
        card.addView(description);
        card.addView(spacer(18));

        Button submit = qualityMaxTest
                ? dangerButton("Attempt negative transfer")
                : primaryButton("Send Transfer");
        submit.setId(R.id.transfer_submit);
        submit.setContentDescription("transfer.submit");
        card.addView(submit);

        TextView status = statusText();
        status.setId(R.id.transfer_status);
        status.setContentDescription("transfer.status");
        card.addView(status);
        content.addView(card);
        content.addView(spacer(14));

        Button cancel = secondaryButton("Back to accounts");
        cancel.setOnClickListener(view -> showDashboard());
        content.addView(cancel);

        submit.setOnClickListener(view -> {
            hideKeyboard();
            double requestedAmount;
            try {
                requestedAmount = Double.parseDouble(amount.getText().toString());
            } catch (NumberFormatException error) {
                showError(status, "Enter a valid amount.");
                return;
            }
            int fromIndex = from.getSelectedItemPosition();
            int toIndex = to.getSelectedItemPosition();
            if (fromIndex == toIndex) {
                showError(status, "Choose two different accounts.");
                return;
            }

            DemoBankModels.Account source = dashboard.accounts.get(fromIndex);
            DemoBankModels.Account destination = dashboard.accounts.get(toIndex);
            String requestedDescription = description.getText().toString();
            submit.setEnabled(false);
            submit.setText("Running evidence test…");
            status.setText("");

            executor.execute(() -> {
                try {
                    api.transfer(
                            source.id,
                            destination.id,
                            requestedAmount,
                            requestedDescription
                    );
                } catch (DemoBankModels.ApiException transferError) {
                    if (requestedAmount > 0) {
                        runOnUiThread(() -> {
                            submit.setEnabled(true);
                            submit.setText("Send Transfer");
                            showError(status, transferError.getMessage());
                        });
                        return;
                    }
                    verifyRejectedTransfer(
                            source,
                            requestedAmount,
                            transferError,
                            submit,
                            status
                    );
                    return;
                } catch (Exception transferError) {
                    runOnUiThread(() -> {
                        submit.setEnabled(true);
                        submit.setText("Attempt negative transfer");
                        showError(status, "Transfer request failed: " + transferError.getMessage());
                    });
                    return;
                }

                try {
                    DemoBankModels.Dashboard refreshed = api.loadDashboard();
                    double verifiedBalance = accountBalance(refreshed, source.id);
                    runOnUiThread(() -> {
                        dashboard = refreshed;
                        if (requestedAmount > 0) {
                            dashboardMessage = "Transfer completed successfully.";
                            showDashboard();
                            return;
                        }
                        showEvidenceReceipt(
                                requestedAmount,
                                source.balance,
                                verifiedBalance,
                                true,
                                200,
                                "Transfer accepted; balance independently re-read"
                        );
                    });
                } catch (Exception verificationError) {
                    runOnUiThread(() -> {
                        submit.setEnabled(true);
                        submit.setText("Attempt negative transfer");
                        showError(
                                status,
                                "Transfer was accepted, but balance verification failed: "
                                        + verificationError.getMessage()
                        );
                    });
                }
            });
        });

        setContentView(wrap(content));
    }

    private void showEvidenceReceipt(
            double requestedAmount,
            double beforeBalance,
            double afterBalance,
            boolean accepted,
            int statusCode,
            String detail
    ) {
        boolean reproduced = DemoBankRules.isCrisisReproduced(requestedAmount, accepted);
        boolean prevented = DemoBankRules.isCrisisPrevented(
                requestedAmount,
                statusCode,
                beforeBalance,
                afterBalance
        );
        boolean incomplete = DemoBankRules.isEvidenceIncomplete(
                requestedAmount,
                accepted,
                statusCode,
                beforeBalance,
                afterBalance
        );
        boolean balanceChanged = Math.abs(beforeBalance - afterBalance) >= 0.005;
        int stateColor = reproduced || incomplete ? DANGER : SUCCESS;
        String eyebrowText = reproduced
                ? "CRITICAL FINDING"
                : incomplete ? "EVIDENCE INCOMPLETE" : prevented ? "VERIFIED PROTECTION" : "EXPECTED BEHAVIOR";
        String titleText = reproduced
                ? "Vulnerability reproduced."
                : incomplete ? "Protection not verified." : prevented ? "Crisis prevented." : "Transfer verified.";
        String summary = reproduced
                ? balanceChanged
                ? "The backend accepted a negative transfer and changed the source balance."
                : "The backend accepted a forbidden negative transfer; the fresh balance read confirmed its final state."
                : incomplete
                ? requestedAmount <= 0
                ? "The response did not prove that the dangerous transfer left the balance unchanged."
                : "The transfer outcome did not match the expected successful behavior."
                : prevented
                ? "The backend rejected the dangerous transfer and a fresh balance read confirmed no change."
                : "The backend completed the transfer with independently verified account evidence.";
        String resultText = reproduced
                ? "FAIL · APPLICATION DEFECT"
                : incomplete
                ? "FAIL · EVIDENCE INCOMPLETE"
                : prevented ? "PASS · VERIFIED BY QUALITYMAX" : "PASS · EXPECTED BEHAVIOR";

        LinearLayout content = screen();
        brandHeader(content, "QUALITYMAX EVIDENCE RECEIPT");
        content.addView(spacer(34));

        TextView state = eyebrow(eyebrowText, stateColor);
        content.addView(state);
        TextView receiptTitle = title(titleText);
        receiptTitle.setId(R.id.receipt_title);
        receiptTitle.setContentDescription("receipt.title");
        content.addView(receiptTitle);
        content.addView(body(summary));
        content.addView(spacer(22));

        LinearLayout evidence = cardWithBorder(stateColor);
        TextView receiptStatus = evidenceRow(
                "RESULT",
                resultText,
                stateColor
        );
        receiptStatus.setId(R.id.receipt_status);
        receiptStatus.setContentDescription("receipt.status");
        evidence.addView(receiptStatus);
        evidence.addView(divider());
        evidence.addView(evidenceRow("REQUESTED TRANSFER", currency.format(requestedAmount), TEXT));
        evidence.addView(divider());
        TextView before = evidenceRow("BALANCE BEFORE", currency.format(beforeBalance), TEXT);
        before.setId(R.id.receipt_before_balance);
        before.setContentDescription("receipt.before_balance");
        evidence.addView(before);
        evidence.addView(divider());
        TextView after = evidenceRow("BALANCE AFTER", currency.format(afterBalance), stateColor);
        after.setId(R.id.receipt_after_balance);
        after.setContentDescription("receipt.after_balance");
        evidence.addView(after);
        evidence.addView(divider());
        evidence.addView(evidenceRow("HTTP OUTCOME", statusCode + " · " + safeDetail(detail), MUTED));
        content.addView(evidence);
        content.addView(spacer(16));

        TextView proof = body(reproduced
                ? balanceChanged
                ? "QualityMax captured the exact request, response and verified balance mutation needed to block this release."
                : "QualityMax captured the accepted forbidden request and independently verified the resulting balance."
                : incomplete
                ? "QualityMax will not issue a passing receipt until the response and refreshed balance agree."
                : prevented
                ? "QualityMax independently verified that the fixed release rejects this exploit without mutating the balance."
                : "QualityMax verified the response against a fresh read from the account backend.");
        proof.setBackground(rounded(SURFACE, stateColor, 1));
        proof.setPadding(dp(16), dp(14), dp(16), dp(14));
        content.addView(proof);
        content.addView(spacer(18));

        Button rerun = primaryButton(prevented ? "Run protection test again" : "Verify the fixed backend");
        rerun.setOnClickListener(view -> {
            executor.execute(() -> {
                try {
                    DemoBankModels.Dashboard loaded = api.loadDashboard();
                    runOnUiThread(() -> {
                        dashboard = loaded;
                        showTransfer();
                    });
                } catch (Exception error) {
                    runOnUiThread(this::showTransfer);
                }
            });
        });
        content.addView(rerun);
        content.addView(spacer(10));
        Button accounts = secondaryButton("Return to accounts");
        accounts.setOnClickListener(view -> {
            executor.execute(() -> {
                try {
                    DemoBankModels.Dashboard loaded = api.loadDashboard();
                    runOnUiThread(() -> {
                        dashboard = loaded;
                        showDashboard();
                    });
                } catch (Exception error) {
                    runOnUiThread(this::showDashboard);
                }
            });
        });
        content.addView(accounts);
        setContentView(wrap(content));
    }

    private void verifyRejectedTransfer(
            DemoBankModels.Account source,
            double requestedAmount,
            DemoBankModels.ApiException transferError,
            Button submit,
            TextView status
    ) {
        try {
            DemoBankModels.Dashboard refreshed = api.loadDashboard();
            double verifiedBalance = accountBalance(refreshed, source.id);
            runOnUiThread(() -> {
                dashboard = refreshed;
                showEvidenceReceipt(
                        requestedAmount,
                        source.balance,
                        verifiedBalance,
                        false,
                        transferError.statusCode,
                        transferError.getMessage()
                );
            });
        } catch (Exception verificationError) {
            runOnUiThread(() -> {
                submit.setEnabled(true);
                submit.setText("Attempt negative transfer");
                showError(
                        status,
                        "Transfer was rejected, but balance verification failed: "
                                + verificationError.getMessage()
                );
            });
        }
    }

    private static double accountBalance(DemoBankModels.Dashboard snapshot, int accountId) {
        for (DemoBankModels.Account account : snapshot.accounts) {
            if (account.id == accountId) {
                return account.balance;
            }
        }
        throw new IllegalStateException("Source account missing from refreshed balances.");
    }

    private ScrollView wrap(LinearLayout content) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        scroll.addView(content);
        scroll.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = windowInsets.getSystemWindowInsetTop();
                bottom = windowInsets.getSystemWindowInsetBottom();
            }
            view.setPadding(0, top, 0, bottom);
            return windowInsets;
        });
        return scroll;
    }

    private LinearLayout screen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22), dp(20), dp(22), dp(34));
        layout.setBackgroundColor(BACKGROUND);
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return layout;
    }

    private void brandHeader(LinearLayout parent, String context) {
        LinearLayout row = horizontal();
        row.setPadding(dp(18), dp(18), dp(18), dp(18));
        row.setBackgroundColor(NAVY);
        TextView logo = sectionTitle("DEMO BANK");
        logo.setTextColor(Color.WHITE);
        logo.setLetterSpacing(0.08f);
        row.addView(logo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView environment = caption(context);
        environment.setTextColor(Color.rgb(190, 192, 205));
        environment.setTextSize(9);
        environment.setLetterSpacing(0.05f);
        environment.setGravity(Gravity.END);
        row.addView(environment);
        parent.addView(row);
    }

    private LinearLayout horizontal() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return row;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(rounded(SURFACE, Color.TRANSPARENT, 0));
        card.setLayoutParams(matchWidth());
        return card;
    }

    private LinearLayout compactCard() {
        LinearLayout card = card();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        return card;
    }

    private LinearLayout cardWithBorder(int color) {
        LinearLayout card = card();
        card.setBackground(rounded(SURFACE, color, 1));
        return card;
    }

    private TextView title(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(TEXT);
        text.setTextSize(34);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setLetterSpacing(-0.02f);
        text.setPadding(0, dp(5), 0, dp(8));
        return text;
    }

    private TextView amountText(String value) {
        TextView text = title(value);
        text.setTextSize(38);
        text.setPadding(0, dp(8), 0, dp(8));
        return text;
    }

    private TextView sectionTitle(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(TEXT);
        text.setTextSize(17);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(MUTED);
        text.setTextSize(16);
        text.setLineSpacing(0, 1.2f);
        return text;
    }

    private TextView caption(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(MUTED);
        text.setTextSize(11);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setLetterSpacing(0.08f);
        return text;
    }

    private TextView eyebrow(String value, int color) {
        TextView text = caption(value);
        text.setTextColor(color);
        return text;
    }

    private TextView fieldLabel(String value) {
        TextView text = caption(value);
        text.setPadding(0, 0, 0, dp(7));
        return text;
    }

    private EditText input(String hint, String value, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(MUTED);
        input.setText(value);
        input.setTextColor(TEXT);
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackground(rounded(SURFACE, Color.rgb(218, 220, 226), 1));
        input.setLayoutParams(matchWidth());
        return input;
    }

    private Spinner accountSpinner(List<String> labels) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(dp(10), dp(8), dp(10), dp(8));
        spinner.setBackground(rounded(Color.WHITE, Color.TRANSPARENT, 0));
        spinner.setLayoutParams(matchWidth());
        return spinner;
    }

    private Button primaryButton(String value) {
        return button(value, VIOLET, Color.WHITE);
    }

    private Button dangerButton(String value) {
        return button(value, DANGER, Color.WHITE);
    }

    private Button secondaryButton(String value) {
        Button button = button(value, SURFACE_RAISED, TEXT);
        button.setBackground(rounded(SURFACE_RAISED, Color.rgb(218, 220, 226), 1));
        return button;
    }

    private Button button(String value, int background, int foreground) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(foreground);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(13), dp(14), dp(13));
        button.setBackground(rounded(background, Color.TRANSPARENT, 0));
        button.setLayoutParams(matchWidth());
        return button;
    }

    private TextView statusText() {
        TextView text = body("");
        text.setPadding(0, dp(12), 0, 0);
        return text;
    }

    private TextView evidenceRow(String label, String value, int valueColor) {
        TextView text = new TextView(this);
        text.setText(label + "\n" + value);
        text.setTextColor(valueColor);
        text.setTextSize(14);
        text.setLineSpacing(0, 1.15f);
        text.setPadding(0, dp(10), 0, dp(10));
        return text;
    }

    private View transactionRow(DemoBankModels.Transaction transaction) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(11), 0, dp(11));
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        TextView description = sectionTitle(transaction.description);
        description.setTextSize(14);
        details.addView(description);
        details.addView(caption(transaction.date + "  ·  " + transaction.category.toUpperCase(Locale.US)));
        row.addView(details, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        String prefix = transaction.isCredit() ? "+" : "−";
        TextView amount = sectionTitle(prefix + currency.format(Math.abs(transaction.amount)));
        amount.setTextSize(14);
        amount.setTextColor(transaction.isCredit() ? SUCCESS : TEXT);
        amount.setGravity(Gravity.END);
        row.addView(amount);
        row.setContentDescription("transactions.row." + transaction.id);
        return row;
    }

    private View divider() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(232, 233, 238));
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        return divider;
    }

    private TextView backendCaption() {
        return caption("BACKEND  " + BuildConfig.DEMO_BANK_BASE_URL);
    }

    private String modeLabel() {
        if (dashboard == null || dashboard.backendMode == null || dashboard.backendMode.isEmpty()) {
            return "LIVE BACKEND";
        }
        return dashboard.backendMode.toUpperCase(Locale.US) + " BACKEND";
    }

    private View spacer(int heightDp) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(1, dp(heightDp)));
        return spacer;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private GradientDrawable rounded(int fill, int stroke, int strokeWidthDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(16));
        if (strokeWidthDp > 0 && stroke != Color.TRANSPARENT) {
            shape.setStroke(dp(strokeWidthDp), stroke);
        }
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showError(TextView target, String message) {
        target.setText(message == null || message.isEmpty() ? "The request failed." : message);
        target.setTextColor(DANGER);
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused == null) {
            return;
        }
        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(focused.getWindowToken(), 0);
    }

    private static String safeDetail(String detail) {
        if (detail == null || detail.trim().isEmpty()) {
            return "No response detail";
        }
        String singleLine = detail.replace('\n', ' ').trim();
        return singleLine.length() > 72 ? singleLine.substring(0, 69) + "…" : singleLine;
    }
}
