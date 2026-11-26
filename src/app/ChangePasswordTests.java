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
import service.DataStore;
import service.Registry;
import service.SampleData;

public final class ChangePasswordTests {
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("dugtong-password-");
        char[] oldPassword = "old example donor password".toCharArray();
        char[] newPassword = "new example donor password".toCharArray();
        char[] otherPassword = "example admin password".toCharArray();
        try {
            Registry registry = new Registry();
            SampleData.load(registry);
            AccountStore store = new AccountStore(folder);
            AccountAccess accounts = new AccountAccess(store, registry);
            accounts.enroll("D001", oldPassword);
            accounts.enroll("A001", otherPassword);
            check("Incorrect current password is rejected",
                    !accounts.changePassword("D001", otherPassword, newPassword));
            check("Old password still works after rejection", accounts.login("D001", oldPassword).isPresent());
            try {
                accounts.changePassword("D001", oldPassword, "short".toCharArray());
                throw new AssertionError("Short password accepted");
            } catch (IllegalArgumentException expected) {
                check("Short replacement leaves the old password working", accounts.login("D001", oldPassword).isPresent());
            }

            Deque<String> answers = new ArrayDeque<>(List.of("2", "D001", "8", "0"));
            Deque<char[]> passwords = new ArrayDeque<>();
            passwords.add(oldPassword.clone());
            passwords.add(oldPassword.clone());
            passwords.add(newPassword.clone());
            passwords.add(newPassword.clone());
            StringWriter result = new StringWriter();
            PrintWriter output = new PrintWriter(result, true);
            InputReader input = new InputReader(output, prompt -> answers.removeFirst(), prompt -> passwords.removeFirst());
            new ConsoleMenu(registry, new DataStore(folder.toString()), accounts, input, output)
                    .runRole(ConsoleMenu.Role.DONOR);
            check("Donor menu reports success", result.toString().contains("Password changed."));

            AccountAccess reloaded = new AccountAccess(new AccountStore(folder), registry);
            check("New password works after reload", reloaded.login("D001", newPassword).isPresent());
            check("Old password fails after reload", reloaded.login("D001", oldPassword).isEmpty());
            check("Another account remains accessible", reloaded.login("A001", otherPassword).isPresent());
            System.out.println("RESULT: 7 passed");
        } finally {
            Arrays.fill(oldPassword, '\0');
            Arrays.fill(newPassword, '\0');
            Arrays.fill(otherPassword, '\0');
            try (var files = Files.newDirectoryStream(folder)) {
                for (Path file : files) {
                    Files.delete(file);
                }
            }
            Files.delete(folder);
        }
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name);
        }
        System.out.println("PASS  " + name);
    }
}
