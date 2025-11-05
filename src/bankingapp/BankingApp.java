package bankingapp;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console UI:
 * - Login (accountNumber + PIN or ADMIN + PIN)
 * - Session timeout warning after 3 minutes of inactivity (warning only)
 * - Customer menu: info, balance, deposit, withdraw, history, exit
 * - Admin menu: view all accounts, view users, unlock user, view history/info, create account, exit
 * - Numeric input uses Scanner.nextDouble()/nextInt() with InputMismatchException handling
 */
public class BankingApp {
    private static final Scanner sc = new Scanner(System.in);

    // inactivity warning in milliseconds (3 minutes = 180_000 ms)
    private static final long INACTIVITY_WARNING_MS = 180_000;

    public static void main(String[] args) {
        System.out.println("== Advanced Java Banking System ==");

        // Seed data
        BankingSystem system = seed();

        // Login loop
        while (true) {
            long lastInteraction = System.currentTimeMillis();
            System.out.println("\nLogin as CUSTOMER (8-digit) or ADMIN:");
            String id = promptNonEmpty("Enter account number or 'ADMIN': ");
            String pin = promptNonEmpty("Enter PIN (4-6 digits): ");

            Optional<User> ou = system.findUser(id);
            if (ou.isEmpty()) {
                System.out.println("User not found.");
                continue;
            }
            User user = ou.get();
            if (user.isLocked()) {
                System.out.println("Account is locked due to failed attempts. Ask admin to unlock.");
                continue;
            }
            if (!user.authenticate(pin)) {
                System.out.println("Invalid PIN. Failed attempts: " + user.getFailedAttempts() + "/3");
                if (user.isLocked()) System.out.println("User locked. Ask admin to unlock.");
                continue;
            }

            // Logged in
            if (user.isAdmin()) {
                adminMenu(system, user, lastInteraction);
            } else {
                BankAccount acc = system.findAccount(user.getId()).orElseThrow();
                customerMenu(acc, lastInteraction);
            }
        }
    }

    // ----------------------------- //
    //           Menus               //
    // ----------------------------- //

    private static void customerMenu(BankAccount acc, long lastInteraction) {
        boolean running = true;
        while (running) {
            warnIfIdle(lastInteraction);
            printCustomerMenu();
            int choice = readMenuChoice(1, 6);
            lastInteraction = System.currentTimeMillis();

            try {
                switch (choice) {
                    case 1 -> acc.displayAccountInfo();
                    case 2 -> System.out.printf("Current Balance: %.2f%n", acc.checkBalance());
                    case 3 -> {
                        double amt = readMoney("Enter deposit amount (10 - 100000): ");
                        acc.deposit(amt);
                    }
                    case 4 -> {
                        double amt = readMoney("Enter withdrawal amount (<=5000, daily total <=10000): ");
                        acc.withdraw(amt);
                    }
                    case 5 -> {
                        System.out.println("Transaction History:");
                        acc.printHistory();
                    }
                    case 6 -> {
                        if (confirmExit()) {
                            System.out.println("Goodbye!");
                            running = false;
                        }
                    }
                }
            } catch (InsufficientFundsException e) {
                System.out.println("[Error] " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("[Validation] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[Unexpected] " + e.getMessage());
            }
        }
    }

    private static void adminMenu(BankingSystem system, User admin, long lastInteraction) {
        boolean running = true;
        while (running) {
            warnIfIdle(lastInteraction);
            printAdminMenu();
            int choice = readMenuChoice(1, 7);
            lastInteraction = System.currentTimeMillis();

            switch (choice) {
                case 1 -> { // View all accounts (summary)
                    List<BankAccount> list = system.listAllAccounts();
                    System.out.println("== Accounts ==");
                    list.forEach(a -> System.out.println(
                        a.getAccountNumber() + " | " + a.getAccountHolderName() + " | Balance: " + a.getBalanceBD()));
                }
                case 2 -> { // View all users (lock status)
                    System.out.println("== Users ==");
                    for (User u : system.listAllUsers()) {
                        System.out.println(u.getId() + (u.isAdmin() ? " (ADMIN)" : "") +
                                " | Locked: " + u.isLocked());
                    }
                }
                case 3 -> { // Unlock a user
                    String id = promptNonEmpty("Enter account number to unlock (or ADMIN): ");
                    boolean ok = system.unlockUser(id);
                    System.out.println(ok ? "Unlocked." : "User not found.");
                }
                case 4 -> { // View a specific account history
                    String accNo = promptNonEmpty("Enter 8-digit account number: ");
                    system.findAccount(accNo).ifPresentOrElse(
                        a -> { System.out.println("History for " + accNo + ":"); a.printHistory(); },
                        () -> System.out.println("Account not found.")
                    );
                }
                case 5 -> { // View a specific account info
                    String accNo = promptNonEmpty("Enter 8-digit account number: ");
                    system.findAccount(accNo).ifPresentOrElse(
                        bankingapp.BankAccount::displayAccountInfo,
                        () -> System.out.println("Account not found.")
                    );
                }
                case 6 -> { // Create new account
                    System.out.println("== Create New Customer Account ==");
                    // 1) Account number (8 digits & unique)
                    String accNo;
                    while (true) {
                        accNo = promptNonEmpty("Enter new 8-digit account number: ");
                        if (!accNo.matches("\\d{8}")) { // or use ^[1-9]\\d{7}$ to disallow leading 0
                            System.out.println("Invalid. Must be exactly 8 digits.");
                            continue;
                        }
                        if (system.findAccount(accNo).isPresent()) {
                            System.out.println("This account number already exists. Choose another.");
                            continue;
                        }
                        break;
                    }

                    // 2) Name (letters/spaces, >=3)
                    String name;
                    while (true) {
                        name = promptNonEmpty("Enter full name (letters and spaces, min 3 chars): ");
                        if (name.matches("[A-Za-z\\s]{3,}")) break;
                        System.out.println("Invalid name format.");
                    }

                    // 3) PIN (4–6 digits)
                    String pin;
                    while (true) {
                        pin = promptNonEmpty("Set 4–6 digit PIN: ");
                        if (pin.matches("\\d{4,6}")) break;
                        System.out.println("Invalid PIN format.");
                    }

                    // 4) Initial balance (>= 0)
                    double initBal;
                    while (true) {
                        initBal = readMoney("Enter initial balance (>= 0): ");
                        if (initBal >= 0) break;
                        System.out.println("Initial balance cannot be negative.");
                    }

                    // 5) Create+register
                    BankAccount newAcc = new BankAccount(accNo, name, initBal);
                    User newUser = new User(accNo, pin);
                    system.registerCustomer(newUser, newAcc);

                    System.out.println("✅ Account created successfully:");
                    newAcc.displayAccountInfo();
                }
                case 7 -> {
                    if (confirmExit()) {
                        System.out.println("Signing out of admin.");
                        running = false;
                    }
                }
            }
        }
    }

    // ----------------------------- //
    //       Seeding & Helpers       //
    // ----------------------------- //

    private static BankingSystem seed() {
        BankingSystem system = new BankingSystem();

        // Admin (change the PIN if you want)
        system.registerAdmin(new User("ADMIN", "9999"));

        // Two demo customers
        User u1 = new User("12345678", "1234");
        BankAccount a1 = new BankAccount("12345678", "Abdulla Shaker", 500.00);
        system.registerCustomer(u1, a1);

        User u2 = new User("87654321", "4321");
        BankAccount a2 = new BankAccount("87654321", "Ahmed Ali", 1500.00);
        system.registerCustomer(u2, a2);

        return system;
    }

    /** Prints idle-warning if ≥ 3 minutes since last menu draw (simulation). */
    private static void warnIfIdle(long lastInteraction) {
        long idle = System.currentTimeMillis() - lastInteraction;
        if (idle >= INACTIVITY_WARNING_MS) {
            System.out.println("[Warning] Session idle for 3+ minutes.");
        }
    }

    private static void printCustomerMenu() {
        System.out.println("""
                -----------------------------
                1: Display Account Info
                2: Check Balance
                3: Deposit Money
                4: Withdraw Money
                5: View Transaction History
                6: Exit
                -----------------------------""");
        System.out.print("Choose an option (1-6): ");
    }

    private static void printAdminMenu() {
        System.out.println("""
                -----------------------------
                ADMIN MENU
                1: View All Accounts
                2: View Users (Lock Status)
                3: Unlock User
                4: View Account History
                5: View Account Info
                6: Create New Account
                7: Exit
                -----------------------------""");
        System.out.print("Choose an option (1-7): ");
    }

    // ---------- Input utilities with validation ----------
    private static String promptNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine();
            if (s != null && !s.trim().isEmpty()) return s.trim();
            System.out.println("Input cannot be empty.");
        }
    }

    private static int readMenuChoice(int min, int max) {
        while (true) {
            System.out.print(""); // keep cursor on same line
            try {
                int n = sc.nextInt();             // will throw InputMismatchException on non-numeric
                sc.nextLine();                    // consume endline
                if (n >= min && n <= max) return n;
                System.out.print("Please enter a valid option (" + min + "-" + max + "): ");
            } catch (java.util.InputMismatchException e) {
                System.out.print("Invalid input. Enter a number (" + min + "-" + max + "): ");
                sc.nextLine(); // flush invalid token
            }
        }
    }

    private static double readMoney(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double v = sc.nextDouble();      // throws InputMismatchException if not numeric
                sc.nextLine();                   // consume endline
                if (v < 0) {
                    System.out.println("Amount cannot be negative.");
                    continue;
                }
                // rounding happens again inside BankAccount with BigDecimal; this is for UI polish only
                return Math.round(v * 100.0) / 100.0;
            } catch (java.util.InputMismatchException e) {
                System.out.println("Please enter a numeric value.");
                sc.nextLine(); // flush invalid token
            }
        }
    }

    private static boolean confirmExit() {
        System.out.print("Are you sure you want to exit? (Y/N): ");
        String ans = sc.nextLine().trim().toUpperCase();
        return ans.equals("Y") || ans.equals("YES");
    }
}

