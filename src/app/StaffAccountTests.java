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

public final class StaffAccountTests {
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("dugtong-staff-");
        char[] adminPassword = "sample administrator password".toCharArray();
        char[] staffPassword = "sample staff account password".toCharArray();
        try {
            Registry registry = new Registry();
            SampleData.load(registry);
            DataStore data = new DataStore(folder.toString());
            AccountAccess accounts = new AccountAccess(new AccountStore(folder), registry);
            accounts.enroll("A001", adminPassword);
            String hospital = registry.getHospitals().get(0).getName();
            int initialStaff = registry.getStaff().size();
            String denied = runAdmin(registry, data, accounts,
                    List.of("A001", "8", "S100", "Sample Staff", "09998887766", hospital, "0"),
                    List.of(adminPassword, "wrong administrator password".toCharArray()));
            if (!denied.contains("Administrator verification failed.") || registry.getStaff().size() != initialStaff) {
                throw new AssertionError("Incorrect administrator password must not create a staff profile");
            }
            System.out.println("PASS  Incorrect administrator password creates no staff account");

            String duplicate = runAdmin(registry, data, accounts,
                    List.of("A001", "8", "D001", "Sample Staff", "09998887766", hospital, "0"),
                    List.of(adminPassword));
            if (!duplicate.contains("already in use") || registry.getStaff().size() != initialStaff) {
                throw new AssertionError("An ID belonging to another role must be rejected");
            }
            System.out.println("PASS  An ID belonging to another role is rejected");

            Deque<String> fields = new ArrayDeque<>(List.of("A001", "8", "S100", "Sample Staff",
                    "09998887766", hospital, "0"));
            Deque<char[]> passwords = new ArrayDeque<>();
            passwords.add(adminPassword.clone());
            passwords.add(adminPassword.clone());
            passwords.add(staffPassword.clone());
            passwords.add(staffPassword.clone());
            StringWriter messages = new StringWriter();
            PrintWriter output = new PrintWriter(messages, true);
            InputReader input = new InputReader(output, prompt -> fields.removeFirst(),
                    prompt -> passwords.removeFirst());
            new ConsoleMenu(registry, data, accounts, input, output).runRole(ConsoleMenu.Role.ADMIN);
            if (!messages.toString().contains("Staff account created: S100.")) {
                throw new AssertionError("Staff account creation did not succeed");
            }
            Registry reloaded = new Registry();
            data.load(reloaded);
            AccountAccess restarted = new AccountAccess(new AccountStore(folder), reloaded);
            if (!hospital.equals(reloaded.findStaff("S100").getHospitalName())
                    || restarted.login("S100", staffPassword).filter(login -> "Hospital Staff".equals(login.role())).isEmpty()) {
                throw new AssertionError("Staff account did not survive reload");
            }
            System.out.println("PASS  Administrator creates a staff account and it survives reload");
        } finally {
            Arrays.fill(adminPassword, '\0');
            Arrays.fill(staffPassword, '\0');
            try (var files = Files.newDirectoryStream(folder)) {
                for (Path file : files) Files.delete(file);
            }
            Files.delete(folder);
        }
    }

    private static String runAdmin(Registry registry, DataStore data, AccountAccess accounts,
                                   List<String> answers, List<char[]> secrets) {
        Deque<String> fields = new ArrayDeque<>(answers);
        Deque<char[]> passwords = new ArrayDeque<>(secrets);
        StringWriter messages = new StringWriter();
        PrintWriter output = new PrintWriter(messages, true);
        InputReader input = new InputReader(output, prompt -> fields.removeFirst(),
                prompt -> passwords.removeFirst().clone());
        new ConsoleMenu(registry, data, accounts, input, output).runRole(ConsoleMenu.Role.ADMIN);
        return messages.toString();
    }
}
