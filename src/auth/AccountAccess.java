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
        Map<User, String> records = store.load(profiles());
        for (var entry : records.entrySet()) {
            if (entry.getValue().startsWith(AccountStore.DEACTIVATED)) {
                entry.getKey().deactivateAccount();
            }
        }
        auth = new AuthService(records);
    }

    public Optional<AuthService.Login> login(String id, char[] password) {
        return auth.login(id, password);
    }

    /** Deactivates the signed-in account after its password is verified. */
    public boolean deactivate(String id, char[] password) throws IOException {
        List<User> profiles = profiles();
        Map<User, String> records = new HashMap<>(store.load(profiles));
        Optional<AuthService.Login> verified = new AuthService(records).login(id, password);
        if (verified.isEmpty()) {
            return false;
        }
        User user = profiles.stream().filter(profile -> profile.getId().equals(verified.get().userId()))
                .findFirst().orElseThrow();
        if ("Admin".equals(user.getRole()) && records.entrySet().stream()
                .noneMatch(entry -> entry.getKey() != user && "Admin".equals(entry.getKey().getRole())
                        && !entry.getValue().startsWith(AccountStore.DEACTIVATED))) {
            throw new IllegalStateException("At least one administrator account must remain active.");
        }
        records.put(user, AccountStore.DEACTIVATED + records.get(user));
        AuthService updated = new AuthService(records);
        store.save(records);
        user.deactivateAccount();
        auth = updated;
        return true;
    }

    /** Changes another account's status after verifying an administrator's password. */
    public boolean setAccountActive(String adminId, char[] password, String targetId, boolean active)
            throws IOException {
        List<User> profiles = profiles();
        Map<User, String> records = new HashMap<>(store.load(profiles));
        Optional<AuthService.Login> verified = new AuthService(records).login(adminId, password);
        if (verified.isEmpty() || !"Admin".equals(verified.get().role())) {
            return false;
        }
        User target = profiles.stream()
                .filter(user -> AuthService.normalizeId(user.getId()).equals(AuthService.normalizeId(targetId)))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No profile with that account ID."));
        if (target.getId().equalsIgnoreCase(verified.get().userId())) {
            throw new IllegalArgumentException("Use the self-deactivation option for your own account.");
        }
        String record = records.get(target);
        if (record == null) {
            throw new IllegalArgumentException("That account has no password set.");
        }
        boolean alreadyActive = !record.startsWith(AccountStore.DEACTIVATED);
        if (alreadyActive == active) {
            throw new IllegalArgumentException("That account already has the requested status.");
        }
        records.put(target, active ? record.substring(AccountStore.DEACTIVATED.length())
                : AccountStore.DEACTIVATED + record);
        AuthService updated = new AuthService(records);
        store.save(records);
        if (active) {
            target.activateAccount();
        } else {
            target.deactivateAccount();
        }
        auth = updated;
        return true;
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
