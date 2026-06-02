import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// ============================================================
//   BHARAT BANK ATM - IMPROVED VERSION
//   Features: Bug Fix, Security, Transaction History,
//             Spending Analysis, Gemini AI, Fraud Alert
// ============================================================

class ATM {

    // ── Security ──────────────────────────────────────────────
    private final int CORRECT_PIN = 1234;
    private int failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;

    // ── Session Data ──────────────────────────────────────────
    private double totalWithdrawn = 0;
    private double totalDeposited = 0;
    private List<String> transactionHistory = new ArrayList<>();

    // ── Gemini API ────────────────────────────────────────────
    private static final String GEMINI_API_KEY = "YOUR_API_KEY_HERE";

    // ── DB Config ─────────────────────────────────────────────
    private static final String DB_URL = "jdbc:mysql://localhost:3306/atm";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "1234";

    Scanner sc = new Scanner(System.in);

    // ── UI Helpers ────────────────────────────────────────────
    private void printLine() {
        System.out.println("==================================");
    }

    private void printHeader(String title) {
        printLine();
        System.out.printf("|%s|\n", centerText(title, 34));
        printLine();
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, width - text.length() - padding));
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL Driver not found!", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // ─────────────────────────────────────────────────────────
    //  1. PIN CHECK WITH LOCKOUT
    // ─────────────────────────────────────────────────────────
    public void checkpin() {
        printHeader("  BHARAT BANK  ");
        System.out.println("|    CARD HAS BEEN INSERTED      |");
        System.out.println("|         ENTER YOUR PIN         |");
        printLine();

        while (failedAttempts < MAX_ATTEMPTS) {
            System.out.print("PIN: ");
            int inputPin = sc.nextInt();

            if (inputPin == CORRECT_PIN) {
                System.out.println("\n✅ PIN CORRECT! Welcome!\n");
                mainmenu();
                return;
            } else {
                failedAttempts++;
                int remaining = MAX_ATTEMPTS - failedAttempts;
                if (remaining > 0) {
                    System.out.println("❌ INCORRECT PIN! Attempts left: " + remaining);
                }
            }
        }

        // Locked out
        printHeader("  CARD BLOCKED  ");
        System.out.println("| Too many wrong attempts!       |");
        System.out.println("| Please contact your bank.      |");
        printLine();
        System.exit(0);
    }

    // ─────────────────────────────────────────────────────────
    //  2. MAIN MENU (BUG FIXED - 7 = Exit now works)
    // ─────────────────────────────────────────────────────────
    public void mainmenu() {
        System.out.println();
        printHeader("    MAIN MENU   ");
        System.out.println("|  1. Check Balance              |");
        System.out.println("|  2. Withdraw Money             |");
        System.out.println("|  3. Deposit Money              |");
        System.out.println("|  4. AI Assistant               |");
        System.out.println("|  5. Spending Analysis          |");
        System.out.println("|  6. Transaction Prediction     |");
        System.out.println("|  7. Transaction History        |");
        System.out.println("|  8. Gemini AI Chat             |");
        System.out.println("|  9. Exit                       |");
        printLine();
        System.out.print("Choose option: ");

        int choice = sc.nextInt();
        System.out.println();

        switch (choice) {
            case 1 -> { acbalance(); pause(); mainmenu(); }
            case 2 -> { moneywithdraw(); pause(); mainmenu(); }
            case 3 -> { moneydeposit(); pause(); mainmenu(); }
            case 4 -> { aiAssistant(); mainmenu(); }
            case 5 -> { spendingAnalysis(); mainmenu(); }
            case 6 -> { transactionPrediction(); mainmenu(); }
            case 7 -> { showTransactionHistory(); mainmenu(); }   // NEW
            case 8 -> { geminiChat(); mainmenu(); }
            case 9 -> {                                            // BUG FIXED
                printHeader("   THANK YOU!   ");
                System.out.println("|  Please take your card.        |");
                System.out.println("|  Have a great day!             |");
                printLine();
                System.exit(0);
            }
            default -> {
                System.out.println("❌ Invalid option! Please choose 1-9.");
                mainmenu();
            }
        }
    }

    private void pause() {
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ─────────────────────────────────────────────────────────
    //  3. CHECK BALANCE
    // ─────────────────────────────────────────────────────────
    public void acbalance() {
        printHeader(" ACCOUNT BALANCE ");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM account")) {

            while (rs.next()) {
                System.out.println("  Name    : " + rs.getString("Name"));
                System.out.printf("  Balance : INR %.2f%n", rs.getDouble("Account_Balance"));
            }
        } catch (Exception e) {
            System.out.println("❌ Error fetching balance: " + e.getMessage());
        }
        printLine();
    }

    // ─────────────────────────────────────────────────────────
    //  4. WITHDRAW (with Fraud Alert)
    // ─────────────────────────────────────────────────────────
    public void moneywithdraw() {
        printHeader("  WITHDRAW MONEY ");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM account")) {

            while (rs.next()) {
                double balance = rs.getDouble("Account_Balance");
                System.out.printf("  Current Balance: INR %.2f%n", balance);
                System.out.print("  Enter amount   : INR ");
                double amount = sc.nextDouble();

                // Fraud Alert
                if (amount > 25000) {
                    System.out.println("\n  ⚠️  AI FRAUD ALERT!");
                    System.out.printf("  Large transaction: INR %.2f%n", amount);
                    System.out.print("  Are you sure? (yes/no): ");
                    sc.nextLine();
                    String confirm = sc.nextLine().trim().toLowerCase();
                    if (!confirm.equals("yes")) {
                        System.out.println("  ❌ Transaction Cancelled.");
                        return;
                    }
                }

                if (amount <= 0) {
                    System.out.println("  ❌ Invalid amount!");
                    return;
                }

                if (amount > balance) {
                    System.out.println("  ❌ Insufficient Balance!");
                    return;
                }

                totalWithdrawn += amount;
                updateBalance(balance - amount);
                String record = String.format("WITHDRAW  | INR -%.2f | Bal: %.2f", amount, balance - amount);
                transactionHistory.add(record);

                System.out.printf("%n  ✅ Withdrawn : INR %.2f%n", amount);
                System.out.printf("  Remaining  : INR %.2f%n", balance - amount);
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
        printLine();
    }

    // ─────────────────────────────────────────────────────────
    //  5. DEPOSIT
    // ─────────────────────────────────────────────────────────
    public void moneydeposit() {
        printHeader("  DEPOSIT MONEY  ");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM account")) {

            while (rs.next()) {
                double balance = rs.getDouble("Account_Balance");
                System.out.printf("  Current Balance: INR %.2f%n", balance);
                System.out.print("  Enter amount   : INR ");
                double amount = sc.nextDouble();

                if (amount <= 0) {
                    System.out.println("  ❌ Invalid amount!");
                    return;
                }

                totalDeposited += amount;
                updateBalance(balance + amount);
                String record = String.format("DEPOSIT   | INR +%.2f | Bal: %.2f", amount, balance + amount);
                transactionHistory.add(record);

                System.out.printf("%n  ✅ Deposited : INR %.2f%n", amount);
                System.out.printf("  New Balance : INR %.2f%n", balance + amount);
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
        printLine();
    }

    // ─────────────────────────────────────────────────────────
    //  6. UPDATE BALANCE IN DB
    // ─────────────────────────────────────────────────────────
    private void updateBalance(double newBalance) {
        String sql = "UPDATE account SET Account_Balance = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("❌ DB Update Error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────
    //  7. AI ASSISTANT (IMPROVED)
    // ─────────────────────────────────────────────────────────
    public void aiAssistant() {
        printHeader("  AI ASSISTANT  ");
        System.out.println("  Type your query (e.g. balance,");
        System.out.println("  withdraw, deposit, history):");
        System.out.print("  > ");
        sc.nextLine();
        String query = sc.nextLine().toLowerCase();

        if (query.contains("balance")) {
            acbalance();
        } else if (query.contains("withdraw")) {
            System.out.println("  👉 Use Option 2 to withdraw money.");
        } else if (query.contains("deposit")) {
            System.out.println("  👉 Use Option 3 to deposit money.");
        } else if (query.contains("history")) {
            showTransactionHistory();
        } else if (query.contains("analysis") || query.contains("spending")) {
            spendingAnalysis();
        } else {
            System.out.println("  🤖 Sorry, I didn't understand.");
            System.out.println("  Try: balance, withdraw, deposit, history");
        }
        printLine();
    }

    // ─────────────────────────────────────────────────────────
    //  8. SPENDING ANALYSIS (IMPROVED)
    // ─────────────────────────────────────────────────────────
    public void spendingAnalysis() {
        printHeader(" SPENDING ANALYSIS ");
        System.out.printf("  Total Withdrawn : INR %.2f%n", totalWithdrawn);
        System.out.printf("  Total Deposited : INR %.2f%n", totalDeposited);
        double net = totalWithdrawn - totalDeposited;
        System.out.printf("  Net Spending    : INR %.2f%n", net);
        System.out.println();

        // AI Suggestion
        if (net > 20000) {
            System.out.println("  🔴 AI: Spending is HIGH this session.");
            System.out.println("  💡 Tip: Consider reducing withdrawals.");
        } else if (net > 10000) {
            System.out.println("  🟡 AI: Spending is MODERATE.");
            System.out.println("  💡 Tip: Keep an eye on expenses.");
        } else if (net <= 0) {
            System.out.println("  🟢 AI: You deposited more than you spent!");
            System.out.println("  💡 Great saving habit!");
        } else {
            System.out.println("  🟢 AI: Spending is UNDER CONTROL.");
            System.out.println("  💡 Keep it up!");
        }
        printLine();
    }

    // ─────────────────────────────────────────────────────────
    //  9. TRANSACTION PREDICTION (IMPROVED)
    // ─────────────────────────────────────────────────────────
    public void transactionPrediction() {
        printHeader("  TX PREDICTION  ");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM account")) {

            while (rs.next()) {
                double balance = rs.getDouble("Account_Balance");
                System.out.printf("  Current Balance     : INR %.2f%n", balance);
                System.out.printf("  Session Withdrawals : INR %.2f%n", totalWithdrawn);
                double predicted = balance - totalWithdrawn;
                System.out.printf("  Predicted Balance   : INR %.2f%n", predicted);
                System.out.println();

                if (predicted < 1000) {
                    System.out.println("  ⚠️  AI: Balance may run LOW soon!");
                    System.out.println("  💡 Consider depositing funds.");
                } else {
                    System.out.println("  ✅ AI: Balance looks healthy.");
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
        printLine();
    }

    // ─────────────────────────────────────────────────────────
    //  10. TRANSACTION HISTORY (NEW FEATURE)
    // ─────────────────────────────────────────────────────────
    public void showTransactionHistory() {
        printHeader(" TRANSACTION HISTORY ");
        if (transactionHistory.isEmpty()) {
            System.out.println("  No transactions this session.");
        } else {
            for (int i = 0; i < transactionHistory.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, transactionHistory.get(i));
            }
        }
        printLine();
    }

    // ─────────────────────────────────────────────────────────
    //  11. GEMINI AI CHAT
    // ─────────────────────────────────────────────────────────
    public void geminiChat() {
        printHeader("  GEMINI AI CHAT  ");
        System.out.print("  Ask anything: ");
        sc.nextLine();
        String prompt = sc.nextLine();

        System.out.println("  🤖 Thinking...\n");

        String json = """
        {
          "contents": [{
            "parts": [{
              "text": "%s"
            }]
          }]
        }
        """.formatted(prompt.replace("\"", "\\\""));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                                + GEMINI_API_KEY))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Extract text from JSON response
            String body = response.body();
            int start = body.indexOf("\"text\": \"") + 9;
            int end = body.indexOf("\"", start);
            if (start > 9 && end > start) {
                String reply = body.substring(start, end)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"");
                System.out.println("  Gemini: " + reply);
            } else {
                System.out.println("  Raw Response: " + body);
            }

        } catch (Exception e) {
            System.out.println("  ❌ Gemini Error: " + e.getMessage());
        }
        printLine();
    }

} // ── ATM CLASS END ─────────────────────────────────────────


class Main {
    public static void main(String[] args) {
        ATM atm = new ATM();
        atm.checkpin();
    }
}
