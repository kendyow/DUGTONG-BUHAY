package app;

import exception.DuplicateRecordException;
import service.Registry;
import service.SampleData;

public class Main {
    public static void main(String[] args) {
        Registry registry = new Registry();
        try {
            SampleData.load(registry);
        } catch (DuplicateRecordException e) {
            System.out.println("Could not load sample data: " + e.getMessage());
        }
        new ConsoleMenu(registry).run();
    }
}