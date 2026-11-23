package app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import auth.AccountAccess;
import auth.AccountStore;
import exception.DugtongBuhayException;
import service.DataStore;
import service.Registry;
import service.SampleData;

public class Main {
    public static void main(String[] args) {
        boolean setup = args.length == 2 && "--setup-account".equals(args[0]);
        if (args.length != 0 && !setup) {
            System.out.println("Usage: app.Main [--setup-account ACCOUNT_ID]");
            return;
        }

        Registry registry = new Registry();
        DataStore store = new DataStore("data");
        try {
            if (store.hasSavedData()) {
                store.load(registry);
                System.out.println("Loaded saved data from the data/ folder.");
            } else {
                SampleData.load(registry);
                store.save(registry);
                System.out.println("First run: sample data loaded and saved to the data/ folder.");
            }
            AccountAccess accounts = new AccountAccess(new AccountStore(Path.of("data")), registry);
            if (setup) {
                char[] password = new InputReader().readNewPassword();
                try {
                    accounts.enroll(args[1], password);
                    System.out.println("Account password saved. You can now sign in.");
                } finally {
                    Arrays.fill(password, '\0');
                }
            } else {
                new ConsoleMenu(registry, store, accounts).run();
            }
        } catch (DugtongBuhayException | IOException | IllegalArgumentException e) {
            System.out.println("Startup problem: " + e.getMessage());
        }
    }
}
