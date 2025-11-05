package bankingapp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bank account model with full business rules:
 * - Private fields
 * - Deposit/withdraw with validation
 * - Daily withdrawal caps (per txn & per day)
 * - Decimal precision with BigDecimal
 * - Transaction history
 */
public class BankAccount {
    private final String accountNumber;       // exactly 8 digits
    private final String accountHolderName;   // letters + spaces, len >= 3
    private BigDecimal balance;               // scale(2)
    private BigDecimal dailyWithdrawTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private LocalDate lastWithdrawalDay = LocalDate.now();
    private final List<Transaction> history = new ArrayList<>();

    // Limits
    public static final BigDecimal MIN_DEPOSIT = new BigDecimal("10.00");
    public static final BigDecimal MAX_DEPOSIT = new BigDecimal("100000.00");
    public static final BigDecimal MAX_WITHDRAW_PER_TX = new BigDecimal("5000.00");
    public static final BigDecimal MAX_WITHDRAW_DAILY = new BigDecimal("10000.00");

    public BankAccount(String accountNumber, String accountHolderName, double initialBalance) {
        validateAccountNumber(accountNumber);
        validateHolderName(accountHolderName);
        if (initialBalance < 0) throw new IllegalArgumentException("Initial balance cannot be negative.");

        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = scale2(initialBalance);
        // initial transaction (optional)
        if (this.balance.compareTo(BigDecimal.ZERO) > 0) {
            history.add(new Transaction(LocalDateTime.now(), Transaction.Type.DEPOSIT,
                    this.balance, this.balance, "Initial balance"));
        }
    }

    private void validateAccountNumber(String acc) {
        if (acc == null || !acc.matches("\\d{8}"))
            throw new IllegalArgumentException("Account number must be exactly 8 digits.");
    }
    private void validateHolderName(String name) {
        if (name == null || name.trim().length() < 3 || !name.matches("[A-Za-z\\s]+"))
            throw new IllegalArgumentException("Name must be letters/spaces only, min length 3.");
    }
    private BigDecimal scale2(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal scale2(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }

    /** Prints account details */
    public void displayAccountInfo() {
        System.out.println("---- Account Info ----");
        System.out.println("Account Number : " + accountNumber);
        System.out.println("Account Holder : " + accountHolderName);
        System.out.println("Current Balance: " + balance);
        System.out.println("----------------------");
    }

    /** Returns current balance as primitive double (UI convenience) */
    public double checkBalance() { return balance.doubleValue(); }

    /** Deposits amount with validation and rounding. */
    public void deposit(double amount) {
        BigDecimal amt = scale2(amount);
        if (amt.compareTo(MIN_DEPOSIT) < 0) throw new IllegalArgumentException("Minimum deposit is " + MIN_DEPOSIT);
        if (amt.compareTo(MAX_DEPOSIT) > 0) throw new IllegalArgumentException("Maximum deposit is " + MAX_DEPOSIT);

        balance = scale2(balance.add(amt));
        history.add(new Transaction(LocalDateTime.now(), Transaction.Type.DEPOSIT, amt, balance, null));
        System.out.println("Deposited: " + amt + " | New Balance: " + balance);
    }

    /** Withdraws amount, enforcing per-transaction and daily limits, and sufficient funds. */
    public void withdraw(double amount) throws InsufficientFundsException {
        BigDecimal amt = scale2(amount);
        if (amt.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Withdrawal must be positive.");
        if (amt.compareTo(MAX_WITHDRAW_PER_TX) > 0) throw new IllegalArgumentException("Max per transaction: " + MAX_WITHDRAW_PER_TX);

        // reset daily counter when a new day starts
        LocalDate today = LocalDate.now();
        if (!today.equals(lastWithdrawalDay)) {
            dailyWithdrawTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            lastWithdrawalDay = today;
        }

        BigDecimal newDaily = scale2(dailyWithdrawTotal.add(amt));
        if (newDaily.compareTo(MAX_WITHDRAW_DAILY) > 0)
            throw new IllegalArgumentException("Daily limit exceeded (" + MAX_WITHDRAW_DAILY + "). Used: " + dailyWithdrawTotal);

        if (amt.compareTo(balance) > 0)
            throw new InsufficientFundsException("Insufficient funds. Balance: " + balance + ", Requested: " + amt);

        balance = scale2(balance.subtract(amt));
        dailyWithdrawTotal = newDaily;

        history.add(new Transaction(LocalDateTime.now(), Transaction.Type.WITHDRAW, amt, balance,
                "Daily used: " + dailyWithdrawTotal + "/" + MAX_WITHDRAW_DAILY));
        System.out.println("Withdrawn: " + amt + " | New Balance: " + balance);
    }

    /** Transaction history (read-only list). */
    public List<Transaction> getHistory() { return List.copyOf(history); }

    /** Admin helper to print history. */
    public void printHistory() {
        if (history.isEmpty()) {
            System.out.println("(No transactions yet)");
            return;
        }
        history.forEach(t -> System.out.println(" - " + t));
    }

    // Accessors for admin / system use
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public BigDecimal getBalanceBD() { return balance; }
}

