package auth;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import model.User;

/** Verifies account credentials supplied by trusted application code at startup. */
public final class AuthService {
    /** Identifies a verified account without exposing its contact details or password record. */
    public record Login(String userId, String role) {
    }

    private record Account(Login login, String passwordRecord) {
    }

    private final Map<String, Account> accounts;

    /**
     * Creates a read-only account index from existing users and their encoded password records.
     * User IDs must be unique across roles. The application must supply this map from trusted storage.
     */
    public AuthService(Map<? extends User, String> passwordRecords) {
        Map<String, Account> indexed = new HashMap<>();
        passwordRecords.forEach((user, passwordRecord) -> {
            String id = user.getId();
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Account ID must not be blank.");
            }
            Account account = new Account(new Login(id, user.getRole()),
                    Objects.requireNonNull(passwordRecord, "Password record is required."));
            if (indexed.putIfAbsent(normalizeId(id), account) != null) {
                throw new IllegalArgumentException("Duplicate account ID: " + id);
            }
        });
        accounts = Map.copyOf(indexed);
    }

    /** Returns the account identity on success or an empty result for invalid credentials. */
    public Optional<Login> login(String userId, char[] password) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        Account account = accounts.get(normalizeId(userId));
        if (account == null || !PasswordHasher.verify(password, account.passwordRecord())) {
            return Optional.empty();
        }
        return Optional.of(account.login());
    }

    static String normalizeId(String id) {
        return id.strip().toUpperCase(Locale.ROOT);
    }
}
