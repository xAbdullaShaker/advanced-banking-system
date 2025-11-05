package bankingapp;

/**
 * Thrown when a withdrawal would overdraw the account.
 */
public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
