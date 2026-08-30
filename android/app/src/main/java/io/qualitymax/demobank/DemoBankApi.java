package io.qualitymax.demobank;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class DemoBankApi {
    private final String baseUrl;

    DemoBankApi(String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    DemoBankModels.SessionUser login(String username, String password)
            throws IOException, JSONException, DemoBankModels.ApiException {
        JSONObject body = new JSONObject()
                .put("username", username)
                .put("password", password);
        JSONObject response = request("POST", "/api/login", body);
        return new DemoBankModels.SessionUser(response.getJSONObject("user").optString("name", username));
    }

    DemoBankModels.Dashboard loadDashboard()
            throws IOException, JSONException, DemoBankModels.ApiException {
        JSONObject response = request("GET", "/api/accounts", null);
        JSONArray rawAccounts = response.getJSONArray("accounts");
        List<DemoBankModels.Account> accounts = new ArrayList<>();
        for (int index = 0; index < rawAccounts.length(); index++) {
            JSONObject raw = rawAccounts.getJSONObject(index);
            accounts.add(new DemoBankModels.Account(
                    raw.getInt("id"),
                    raw.optString("name", "Account"),
                    raw.optString("number", ""),
                    raw.getDouble("balance")
            ));
        }

        String mode = "connected";
        try {
            mode = request("GET", "/api/health", null).optString("mode", "connected");
        } catch (DemoBankModels.ApiException ignored) {
            // Accounts loaded successfully; health metadata is optional.
        }
        List<DemoBankModels.Transaction> transactions = accounts.isEmpty()
                ? new ArrayList<>()
                : loadTransactions(accounts.get(0).id, "");
        return new DemoBankModels.Dashboard(accounts, transactions, mode);
    }

    List<DemoBankModels.Transaction> loadTransactions(int accountId, String search)
            throws IOException, JSONException, DemoBankModels.ApiException {
        String path = "/api/transactions?account_id=" + accountId;
        if (search != null && !search.trim().isEmpty()) {
            path += "&search=" + java.net.URLEncoder.encode(search.trim(), StandardCharsets.UTF_8.name());
        }
        JSONArray rawTransactions = request("GET", path, null).getJSONArray("transactions");
        List<DemoBankModels.Transaction> transactions = new ArrayList<>();
        for (int index = 0; index < rawTransactions.length(); index++) {
            JSONObject raw = rawTransactions.getJSONObject(index);
            transactions.add(new DemoBankModels.Transaction(
                    raw.optInt("id", -1),
                    raw.optString("type", "debit"),
                    raw.optDouble("amount", 0),
                    raw.optString("description", "Transaction"),
                    raw.optString("date", ""),
                    raw.optString("category", "other")
            ));
        }
        return transactions;
    }

    DemoBankModels.TransferResult transfer(int fromId, int toId, double amount, String description)
            throws IOException, JSONException, DemoBankModels.ApiException {
        JSONObject body = new JSONObject()
                .put("from_account_id", fromId)
                .put("to_account_id", toId)
                .put("amount", amount)
                .put("description", description);
        JSONObject response = request("POST", "/api/transfer", body);
        JSONObject transaction = response.optJSONObject("transaction");
        return new DemoBankModels.TransferResult(
                response.getDouble("new_balance"),
                transaction == null ? -1 : transaction.optInt("id", -1)
        );
    }

    private JSONObject request(String method, String path, JSONObject body)
            throws IOException, JSONException, DemoBankModels.ApiException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("Accept", "application/json");

        if (body != null) {
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
        }

        int statusCode = connection.getResponseCode();
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String responseText = readFully(stream);
        connection.disconnect();

        JSONObject response = responseText.isEmpty() ? new JSONObject() : new JSONObject(responseText);
        if (statusCode >= 400) {
            throw new DemoBankModels.ApiException(
                    response.optString("error", "Request failed with status " + statusCode),
                    statusCode
            );
        }
        return response;
    }

    private static String readFully(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    private static String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
