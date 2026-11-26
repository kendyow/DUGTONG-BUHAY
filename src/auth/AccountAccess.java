package auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import model.User;
import service.Registry;

/** Connects stored credentials to the profiles available in the current application run. */
public final class AccountAccess {
    private final AccountStore store;
    private final Registry registry;
    private AuthService auth;

    public AccountAccess(AccountStore store, Registry registry) throws IOException {
        this.store = store;
        this.registry = registry;
        auth = new AuthService(store.load(profiles()));
    }

    public Optional<AuthService.Login> login(String id, char[] password) {
        return auth.login(id, password);
    }

    /** Verifies the current password and atomically stores a replacement. */
    public boolean changePassword(String id, char[] currentPassword, char[] newPassword) throws IOException {
        List<User> profiles = profiles();
        Map<User, String> records = new HashMap<>(store.load(profiles));
        AuthService current = new AuthService(records);
        Optional<AuthService.Login> login = current.login(id, currentPassword);
        if (login.isEmpty()) {
            return false;
        }
        if (newPassword == null || newPassword.length < 15) {
            throw new IllegalArgumentException("Use at least 15 characters for the password.");
        }
        String accountId = login.get().userId();
        User user = profiles.stream().filter(profile -> profile.getId().equals(accountId))
                .findFirst().orElseThrow();
        records.put(user, PasswordHasher.hash(newPassword));
        AuthService updated = new AuthService(records);
        store.save(records);
        auth = updated;
        return true;
    }

    /** Sets a first password for an existing profile. Existing credentials cannot be replaced here. */
    public void enroll(String id, char[] password) throws IOException {
        if (password == null || password.length < 15) {
            throw new IllegalArgumentException("Use at least 15 characters for the password.");
        }
        List<User> profiles = profiles();
        Map<User, String> records = new HashMap<>(store.load(profiles));
        String normalizedId = AuthService.normalizeId(id);
        User user = profiles.stream().filter(profile -> AuthService.normalizeId(profile.getId()).equals(normalizedId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No profile with that account ID."));
        if (records.containsKey(user)) {
            throw new IllegalArgumentException("That account already has a password.");
        }
        records.put(user, PasswordHasher.hash(password));
        AuthService updated = new AuthService(records);
        store.save(records);
        auth = updated;
    }

    private List<User> profiles() {
        List<User> users = new ArrayList<>(registry.getDonors());
        users.addAll(registry.getStaff());
        users.addAll(registry.getAdmins());
        return users;
    }
}
