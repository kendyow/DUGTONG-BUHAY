package app;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import app.ConsoleMenu.Role;
import app.InputReader.RequestDetails;
import auth.AccountAccess;
import auth.AccountStore;
import model.BloodRequest;
import model.BloodType;
import model.UrgencyLevel;
import service.DataStore;
import service.Registry;
import service.SampleData;

/**
 * Tests request creation and cancellation through the shared staff workflow.
 */
public final class EmergencyRequestWorkflowTests {
    private static int passed;

    private EmergencyRequestWorkflowTests() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("dugtong-request-tests-");
        char[] password = "Example staff test password".toCharArray();

        try {
            Registry registry = new Registry();
            SampleData.load(registry);

            DataStore dataStore = new DataStore(directory.toString());
            AccountStore accountStore = new AccountStore(directory);
            AccountAccess accounts = new AccountAccess(accountStore, registry);
            accounts.enroll("S001", password);

            int originalCount = registry.getRequests().size();

            runStaffWorkflow(
                    registry,
                    dataStore,
                    accounts,
                    password,
                    null,
                    List.of("S001", "4", "0")
            );

            check(
                    "Cancelling the form creates no request",
                    registry.getRequests().size() == originalCount
            );

            Registry afterCancel = new Registry();
            dataStore.load(afterCancel);

            check(
                    "Cancelling leaves no new request after reload",
                    afterCancel.getRequests().size() == originalCount
            );

            RequestDetails confirmed = new RequestDetails(
                    "TEST-REQUEST-001",
                    BloodType.A_POS,
                    2,
                    UrgencyLevel.URGENT
            );

            runStaffWorkflow(
                    registry,
                    dataStore,
                    accounts,
                    password,
                    confirmed,
                    List.of("S001", "4", "3", "0")
            );

            check(
                    "Confirming creates one request",
                    registry.getRequests().size() == originalCount + 1
            );

            Registry reloaded = new Registry();
            dataStore.load(reloaded);

            BloodRequest saved = reloaded.getRequests().stream()
                    .filter(request -> "TEST-REQUEST-001".equals(request.getPatientCode()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Saved request was not found"));

            check(
                    "Request details survive reload",
                    saved.getBloodTypeNeeded() == BloodType.A_POS
                            && saved.getUnitsNeeded() == 2
                            && saved.getUrgency() == UrgencyLevel.URGENT
            );

            check(
                    "Saved request belongs to the signed-in staff hospital",
                    saved.getHospitalName().equals(
                            registry.findStaff("S001").getHospitalName()
                    )
            );

            check(
                    "Saved request is open",
                    saved.getStatus() == BloodRequest.Status.OPEN
            );

            System.out.println("RESULT: " + passed + " passed");
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

    private static void runStaffWorkflow(
            Registry registry,
            DataStore dataStore,
            AccountAccess accounts,
            char[] password,
            RequestDetails formResult,
            List<String> answers
    ) {
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output, true);
        Deque<String> fields = new ArrayDeque<>(answers);

        InputReader input = new InputReader(
                writer,
                prompt -> fields.removeFirst(),
                prompt -> password.clone()
        );

        input.setRequestForm(staff -> formResult);

        new ConsoleMenu(registry, dataStore, accounts, input, writer)
                .runRole(Role.STAFF);
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name);
        }

        passed++;
        System.out.println("PASS  " + name);
    }
}