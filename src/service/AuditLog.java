package service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditLog {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<String> entries = new ArrayList<>();

    public void record(String actor, String action) {
        entries.add(LocalDateTime.now().format(FORMAT) + " | " + actor + " | " + action);
    }

    public List<String> getEntries() {
        return Collections.unmodifiableList(entries);
    }

        /** Used only when loading saved data: re-adds an already formatted entry. */
    public void restoreEntry(String line) {
        entries.add(line);
    }
}