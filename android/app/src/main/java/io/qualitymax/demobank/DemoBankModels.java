package io.qualitymax.demobank;

import java.util.List;

final class DemoBankModels {
    private DemoBankModels() {}

    static final class SessionUser {
        final String name;

        SessionUser(String name) {
            this.name = name;
        }
    }

    static final class Account {
        final int id;
        final String name;
        final String number;
        final double balance;

        Account(int id, String name, String number, double balance) {
            this.id = id;
            this.name = name;
            this.number = number;
            this.balance = balance;
        }

        String displayName() {
            return name + "  " + number;
        }
    }

    static final class Dashboard {
        final List<Account> accounts;
        final List<Transaction> transactions;
        final String backendMode;

        Dashboard(List<Account> accounts, List<Transaction> transactions, String backendMode) {
            this.accounts = accounts;
            this.transactions = transactions;
            this.backendMode = backendMode;
        }

        double totalBalance() {
            double total = 0;
            for (Account account : accounts) {
                total += account.balance;
            }
            return total;
        }
    }

    static final class Transaction {
        final int id;
        final String type;
        final double amount;
        final String description;
        final String date;
        final String category;

        Transaction(int id, String type, double amount, String description, String date, String category) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.date = date;
            this.category = category;
        }

        boolean isCredit() {
            return "credit".equals(type);
        }
    }

    static final class TransferResult {
        final double newBalance;
        final int transactionId;

        TransferResult(double newBalance, int transactionId) {
            this.newBalance = newBalance;
            this.transactionId = transactionId;
        }
    }

    static final class ApiException extends Exception {
        final int statusCode;

        ApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}
