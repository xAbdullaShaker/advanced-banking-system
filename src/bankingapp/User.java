package bankingapp;

/**
 * Login identity for either a normal customer or the admin.
 * - accountNumber: 8 digits for customers, or the literal "ADMIN" for the admin identity.
 * - pin: numeric string
 * - lockout after 3 failed attempts
 */
public class User {
    private final String accountNumberOrAdmin;  // "ADMIN" or 8-digit account number
    private final String pin;
    private int failedAttempts = 0;
    private boolean locked = false;

    public User(String accountNumberOrAdmin, String pin) {
        if (accountNumberOrAdmin == null || accountNumberOrAdmin.isBlank())
            throw new IllegalArgumentException("Username cannot be empty.");
        if (!"ADMIN".equals(accountNumberOrAdmin) && !accountNumberOrAdmin.matches("\\d{8}"))
            throw new IllegalArgumentException("User must be 8-digit account number or 'ADMIN'.");
        if (pin == null || !pin.matches("\\d{4,6}"))
            throw new IllegalArgumentException("PIN must be 4–6 digits.");
        this.accountNumberOrAdmin = accountNumberOrAdmin;
        this.pin = pin;
    }

    public String getId() { return accountNumberOrAdmin; }
    public boolean isAdmin() { return "ADMIN".equals(accountNumberOrAdmin); }

    public boolean authenticate(String enteredPin) {
        if (locked) return false;
        boolean ok = this.pin.equals(enteredPin);
        if (!ok) {
            failedAttempts++;
            if (failedAttempts >= 3) locked = true;
        } else {
            failedAttempts = 0;
        }
        return ok;
    }

    public boolean isLocked() { return locked; }
    public void unlock() { locked = false; failedAttempts = 0; }
    public int getFailedAttempts() { return failedAttempts; }
}
