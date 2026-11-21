package app;

import exception.DugtongBuhayException;
import service.DataStore;
import service.Registry;
import service.SampleData;

public class Main {
    public static void main(String[] args) {
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
        } catch (DugtongBuhayException e) {
            System.out.println("Startup problem: " + e.getMessage());
            System.out.println("Fix the file in the data/ folder, or delete the folder to start fresh.");
            return;
        }
        new ConsoleMenu(registry, store).run();
    }
}
