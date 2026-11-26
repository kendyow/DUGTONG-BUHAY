package app;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import auth.AccountAccess;
import auth.AccountStore;
import model.Donor;
import service.DataStore;
import service.Registry;
import service.SampleData;

public final class AccountDeactivationTests {
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("dugtong-deactivation-");
        char[] password = "example donor password".toCharArray();
        char[] wrong = "incorrect password".toCharArray();
        char[] adminPassword = "example administrator password".toCharArray();
        try {
            Registry registry = new Registry();
            SampleData.load(registry);
            DataStore data = new DataStore(folder.toString());
            data.save(registry);
            AccountStore store = new AccountStore(folder);
            AccountAccess accounts = new AccountAccess(store, registry);
            accounts.enroll("D001", password);
            accounts.enroll("A001", adminPassword);
            Donor donor = registry.findDonor("D001");
            donor.giveConsent();
            check(!accounts.deactivate("D001", wrong), "Incorrect password is rejected");
            check(accounts.login("D001", password).isPresent(), "Account stays accessible");
            check(accounts.deactivate("D001", password), "Account is deactivated");
            check(accounts.login("D001", password).isEmpty(), "Deactivated account cannot sign in");
            check(!donor.hasConsented(), "Deactivated donor is excluded from alerts");
            Registry reloaded = new Registry();
            data.load(reloaded);
            AccountAccess restarted = new AccountAccess(store, reloaded);
            check(restarted.login("D001", password).isEmpty(), "Deactivation survives reload");
            check(!reloaded.findDonor("D001").hasConsented(), "Reloaded donor is excluded from alerts");
            try {
                restarted.enroll("D001", password);
                throw new AssertionError("Deactivated account was enrolled again");
            } catch (IllegalArgumentException expected) {
                check(true, "Setup cannot bypass deactivation");
            }
            check(!restarted.setAccountActive("D001", password, "D001", true),
                    "A donor cannot restore an account");
            check(!restarted.setAccountActive("A001", wrong, "D001", true),
                    "Incorrect administrator password is rejected");
            check(restarted.setAccountActive("A001", adminPassword, "D001", true),
                    "Administrator can restore the donor account");
            AccountAccess restored = new AccountAccess(store, reloaded);
            check(restored.login("D001", password).isPresent(), "Restored account works after reload");
            runAdminMenu(reloaded, data, restored,
                    List.of("A001", "7", "D001", "n", "n", "0"), List.of(adminPassword));
            check(restored.login("D001", password).isPresent(), "Cancelled administrator action leaves account active");
            runAdminMenu(reloaded, data, restored,
                    List.of("A001", "7", "D001", "n", "y", "0"), List.of(adminPassword, adminPassword));
            check(restored.login("D001", password).isEmpty(), "Administrator menu deactivates selected account");
            runAdminMenu(reloaded, data, restored,
                    List.of("A001", "7", "D001", "y", "y", "0"), List.of(adminPassword, adminPassword));
            check(restored.login("D001", password).isPresent(), "Administrator menu restores selected account");
            try {
                restored.deactivate("A001", adminPassword);
                throw new AssertionError("Last administrator was deactivated");
            } catch (IllegalStateException expected) {
                check(true, "Last active administrator cannot self-deactivate");
            }
            System.out.println("RESULT: 16 passed");
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(wrong, '\0');
            Arrays.fill(adminPassword, '\0');
            try (var files = Files.newDirectoryStream(folder)) {
                for (Path file : files) Files.delete(file);
            }
            Files.delete(folder);
        }
    }

    private static void runAdminMenu(Registry registry, DataStore data, AccountAccess accounts,
                                     List<String> answers, List<char[]> secrets) {
        Deque<String> fields = new ArrayDeque<>(answers);
        Deque<char[]> passwords = new ArrayDeque<>(secrets);
        PrintWriter output = new PrintWriter(new StringWriter(), true);
        InputReader input = new InputReader(output, prompt -> fields.removeFirst(),
                prompt -> passwords.removeFirst().clone());
        new ConsoleMenu(registry, data, accounts, input, output).runRole(ConsoleMenu.Role.ADMIN);
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        System.out.println("PASS  " + name);
    }
}
