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

/** Tests profile edits through the authenticated donor menu and CSV reload. */
public final class DonorProfileTests {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("dugtong-profile-");
        char[] password = "Example donor test password".toCharArray();
        try {
            Registry registry = new Registry();
            SampleData.load(registry);
            DataStore store = new DataStore(directory.toString());
            AccountAccess accounts = new AccountAccess(new AccountStore(directory), registry);
            accounts.enroll("D001", password);
            Donor donor = registry.findDonor("D001");
            String otherPhone = registry.findDonor("D002").getContactNumber();
            String oldName = donor.getName();
            String oldPhone = donor.getContactNumber();

            runDonorMenu(registry, store, accounts, password,
                    List.of("2", "D001", "7", "Rejected Name", otherPhone, "0"));
            check("Duplicate phone leaves both fields unchanged",
                    oldName.equals(donor.getName()) && oldPhone.equals(donor.getContactNumber()));

            runDonorMenu(registry, store, accounts, password,
                    List.of("2", "D001", "7", "Updated Donor", "09987654321", "0"));
            Registry reloaded = new Registry();
            store.load(reloaded);
            Donor saved = reloaded.findDonor("D001");
            check("Updated name and mobile survive reload", "Updated Donor".equals(saved.getName())
                    && "09987654321".equals(saved.getContactNumber()));
            check("Other donor profile stays unchanged", otherPhone.equals(reloaded.findDonor("D002").getContactNumber()));
            System.out.println("RESULT: 3 passed");
        } finally {
            Arrays.fill(password, '\0');
            try (var files = Files.newDirectoryStream(directory)) {
                for (Path file : files) {
                    Files.delete(file);
                }
            }
            Files.delete(directory);
        }
    }

    private static void runDonorMenu(Registry registry, DataStore store, AccountAccess accounts,
                                     char[] password, List<String> answers) {
        Deque<String> input = new ArrayDeque<>(answers);
        PrintWriter output = new PrintWriter(new StringWriter(), true);
        InputReader reader = new InputReader(output, prompt -> input.removeFirst(), prompt -> password.clone());
        new ConsoleMenu(registry, store, accounts, reader, output).runRole(ConsoleMenu.Role.DONOR);
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name);
        }
        System.out.println("PASS  " + name);
    }
}
