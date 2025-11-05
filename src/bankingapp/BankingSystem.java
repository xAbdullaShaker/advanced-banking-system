package bankingapp;

import java.util.*;

/**
 * In-memory repository and service layer:
 * - Stores users and accounts
 * - Finds accounts by number
 * - Admin utilities (list accounts, unlock users)
 */
public class BankingSystem {
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, BankAccount> accounts = new HashMap<>();

    public BankingSystem() { }

    /** Registers a customer user + its account. */
    public void registerCustomer(User user, BankAccount account) {
        if (user == null || account == null) throw new IllegalArgumentException("User/account cannot be null.");
        if (user.isAdmin()) throw new IllegalArgumentException("Admin cannot be registered as a customer here.");
        if (!user.getId().equals(account.getAccountNumber()))
            throw new IllegalArgumentException("User id must match account number.");
        if (accounts.containsKey(account.getAccountNumber()))
            throw new IllegalArgumentException("Account number already exists.");
        users.put(user.getId(), user);
        accounts.put(account.getAccountNumber(), account);
    }

    /** Registers the admin identity. */
    public void registerAdmin(User admin) {
        if (admin == null || !admin.isAdmin()) throw new IllegalArgumentException("Must provide ADMIN user.");
        users.put(admin.getId(), admin);
    }

    public Optional<User> findUser(String id) { return Optional.ofNullable(users.get(id)); }
    public Optional<BankAccount> findAccount(String accNo) { return Optional.ofNullable(accounts.get(accNo)); }

    // ---------- Admin utilities ----------
    public List<BankAccount> listAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public List<User> listAllUsers() {
        return new ArrayList<>(users.values());
    }

    public boolean unlockUser(String id) {
        User u = users.get(id);
        if (u == null) return false;
        u.unlock();
        return true;
    }
}
