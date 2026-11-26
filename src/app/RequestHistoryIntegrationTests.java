package app;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import auth.AccountAccess;
import auth.AccountStore;
import model.BloodRequest;
import model.BloodType;
import model.HospitalStaff;
import model.UrgencyLevel;
import service.DataStore;
import service.Registry;
import service.SampleData;

public final class RequestHistoryIntegrationTests {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("dugtong-history-");
        char[] password = "Example staff test password".toCharArray();
        try {
            Registry registry = new Registry();
            SampleData.load(registry);
            HospitalStaff first = registry.findStaff("S001");
            HospitalStaff second = registry.findStaff("S002");
            BloodRequest open = first.postRequest("TEST-1", BloodType.A_POS, 1, UrgencyLevel.URGENT);
            BloodRequest closed = first.postRequest("TEST-2", BloodType.O_NEG, 1, UrgencyLevel.ROUTINE);
            closed.cancel();
            BloodRequest other = second.postRequest("TEST-3", BloodType.A_POS, 1, UrgencyLevel.URGENT);
            registry.addRequest(open);
            registry.addRequest(closed);
            registry.addRequest(other);

            DataStore store = new DataStore(directory.toString());
            AccountAccess accounts = new AccountAccess(new AccountStore(directory), registry);
            accounts.enroll("S001", password);
            AtomicReference<List<BloodRequest>> displayed = new AtomicReference<>();
            Deque<String> answers = new ArrayDeque<>(List.of("S001", "9", "0"));
            PrintWriter output = new PrintWriter(new StringWriter(), true);
            InputReader input = new InputReader(output, prompt -> answers.removeFirst(),
                    prompt -> password.clone());
            input.setRequestHistory(displayed::set);
            new ConsoleMenu(registry, store, accounts, input, output).runRole(ConsoleMenu.Role.STAFF);

            List<BloodRequest> history = displayed.get();
            if (history == null || !history.contains(open) || !history.contains(closed)
                    || history.contains(other)) {
                throw new AssertionError("History must include this hospital's requests across statuses only");
            }
            System.out.println("PASS  Staff history includes both open and cancelled requests for their hospital");
            System.out.println("PASS  Staff history excludes another hospital's request");
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
}
