package app;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import app.ConsoleMenu.Role;
import auth.AccountAccess;
import auth.AccountStore;
import service.DataStore;
import service.Registry;
import service.SampleData;

/** Exercises the shared role menu with the same credential store used by both entry points. */
public final class LoginIntegrationTests {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("dugtong-login-");
        String fixture = "sample password for tests";
        try {
            Registry registry = new Registry();
            SampleData.load(registry);
            DataStore data = new DataStore(directory.toString());
            AccountStore store = new AccountStore(directory);
            AccountAccess access = new AccountAccess(store, registry);

            check("An existing ID alone cannot open the admin menu",
                    runRole(registry, data, access, Role.ADMIN, List.of("A001"), List.of(""))
                            .contains("Sign-in failed"));
            access.enroll("A001", fixture.toCharArray());
            try {
                access.enroll("a001", "a different test password".toCharArray());
                throw new AssertionError("Setup replaced an existing password");
            } catch (IllegalArgumentException expected) {
                System.out.println("PASS  Setup rejects an existing account");
            }
            AccountAccess restarted = new AccountAccess(store, registry);
            check("Incorrect password cannot open the admin menu",
                    runRole(registry, data, restarted, Role.ADMIN, List.of("A001"), List.of("incorrect"))
                            .contains("Sign-in failed"));
            check("Correct credentials cannot open a different role",
                    runRole(registry, data, restarted, Role.STAFF, List.of("A001"), List.of(fixture))
                            .contains("Sign-in failed"));
            check("Correct saved credentials open the admin menu",
                    runRole(registry, data, restarted, Role.ADMIN, List.of("a001", "0"), List.of(fixture))
                            .contains("-- Admin menu --"));
            String donorId = registry.nextDonorId();
            String registration = runRole(registry, data, restarted, Role.DONOR,
                    List.of("1", "Example New Donor", "09998765432", "O+", "1", "0"),
                    List.of(fixture, fixture));
            check("A new donor sets a password and enters their menu", registration.contains("-- Donor menu:"));
            Registry reloaded = new Registry();
            data.load(reloaded);
            check("A registered donor can sign in after profile and credential reload",
                    runRole(reloaded, data, new AccountAccess(store, reloaded), Role.DONOR,
                            List.of("2", donorId, "0"), List.of(fixture)).contains("-- Donor menu:"));
            System.out.println("RESULT: 7 passed");
        } finally {
            try (var files = Files.newDirectoryStream(directory)) {
                for (Path file : files) {
                    Files.delete(file);
                }
            }
            Files.delete(directory);
        }
    }

    private static String runRole(Registry registry, DataStore data, AccountAccess access, Role role,
                                  List<String> answers, List<String> passwords) {
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output, true);
        Deque<String> fields = new ArrayDeque<>(answers);
        Deque<String> secrets = new ArrayDeque<>(passwords);
        InputReader input = new InputReader(writer, prompt -> fields.removeFirst(),
                prompt -> secrets.removeFirst().toCharArray());
        new ConsoleMenu(registry, data, access, input, writer).runRole(role);
        return output.toString();
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name);
        }
        System.out.println("PASS  " + name);
    }
}
