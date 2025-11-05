package bankingapp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable transaction record.
 */
public final class Transaction {
    public enum Type { DEPOSIT, WITHDRAW }

    private final LocalDateTime timestamp;
    private final Type type;
    private final BigDecimal amount;        // always scale(2)
    private final BigDecimal balanceAfter;  // always scale(2)
    private final String note;

    public Transaction(LocalDateTime timestamp, Type type, BigDecimal amount,
                       BigDecimal balanceAfter, String note) {
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.note = note;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public Type getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getNote() { return note; }

    @Override public String toString() {
        return "[" + timestamp + "] " + type + " " + amount + " | Balance: " + balanceAfter +
               (note == null || note.isBlank() ? "" : " | " + note);
    }
}
