package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import auth.AccountStore;
import auth.AuthService;
import auth.PasswordHasher;
import model.Admin;

/** Run with java -cp bin app.AccountStoreTests. */
public final class AccountStoreTests {
    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("dugtong-account-test-");
        Path file = directory.resolve("accounts.csv");
        char[] password = "Fixture password for account storage".toCharArray();
        try {
            Admin original = new Admin("A001", "Example Admin", "09000000001");
            List<Admin> profiles = List.of(new Admin("A001", "Updated Admin", "09000000002"));
            AccountStore store = new AccountStore(directory);
            check("Missing account file loads an empty snapshot", store.load(profiles).isEmpty());

            String record = PasswordHasher.hash(password);
            store.save(Map.of(original, record));
            String saved = Files.readString(file);
            check("File contains the hash and excludes plaintext", saved.contains(record)
                    && !saved.contains("Fixture password for account storage"));
            AuthService restarted = new AuthService(new AccountStore(directory).load(profiles));
            check("Account can sign in after loading with a fresh store and profile",
                    restarted.login("a001", password)
                            .filter(login -> login.userId().equals("A001") && login.role().equals("Admin")).isPresent());

            try {
                store.save(Map.of(original, "plaintext"));
                throw new AssertionError("Plaintext record was accepted");
            } catch (IllegalArgumentException expected) {
                check("Rejected save preserves the previous snapshot", Files.readString(file).equals(saved));
            }

            expectLoadFailure(store, List.of(), "Unknown account ID");
            Files.writeString(file, saved + "a001," + record + System.lineSeparator());
            expectLoadFailure(store, profiles, "Duplicate account ID");
            Files.writeString(file, "user_id,password_record\nA001,malformed\n");
            expectLoadFailure(store, profiles, "Malformed hash");

            store.save(Map.of());
            check("An empty snapshot replaces the previous file", Files.exists(file) && store.load(profiles).isEmpty());
            System.out.println("RESULT: 8 passed");
        } finally {
            Arrays.fill(password, '\0');
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static void expectLoadFailure(AccountStore store, List<Admin> profiles, String name) {
        try {
            store.load(profiles);
            throw new AssertionError(name + " was accepted");
        } catch (IOException expected) {
            System.out.println("PASS  " + name + " is rejected");
        }
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name);
        }
        System.out.println("PASS  " + name);
    }
}
