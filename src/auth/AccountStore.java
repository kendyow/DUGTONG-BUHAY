package auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.User;
import service.CsvUtil;

/** Stores credentials separately from user profiles. Use one writer for each data directory. */
public final class AccountStore {
    private static final String HEADER = "user_id,password_record";
    private final Path directory;
    private final Path file;

    public AccountStore(Path dataDirectory) {
        directory = dataDirectory;
        file = directory.resolve("accounts.csv");
    }

    /**
     * Replaces the complete credential snapshot. Invalid input leaves the previous file intact.
     * The directory must support atomic file replacement.
     */
    public void save(Map<? extends User, String> passwordRecords) throws IOException {
        List<String> rows = new ArrayList<>();
        rows.add(HEADER);
        Set<String> seen = new HashSet<>();
        for (var entry : passwordRecords.entrySet()) {
            String id = entry.getKey().getId();
            String record = entry.getValue();
            if (!validId(id) || !PasswordHasher.isEncodedRecord(record)) {
                throw new IllegalArgumentException("Invalid account ID or password record.");
            }
            if (!seen.add(AuthService.normalizeId(id))) {
                throw new IllegalArgumentException("Duplicate account ID: " + id);
            }
            rows.add(CsvUtil.join(id, record));
        }

        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "accounts-", ".tmp");
        try {
            Files.write(temporary, rows, StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Binds stored credentials to current trusted user profiles. A missing file means no accounts. */
    public Map<User, String> load(Iterable<? extends User> users) throws IOException {
        List<String> rows;
        try {
            rows = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return Map.of();
        }
        if (rows.isEmpty() || !HEADER.equals(rows.get(0))) {
            throw new IOException("Invalid accounts.csv header.");
        }

        Map<String, User> byId = new HashMap<>();
        for (User user : users) {
            String id = user.getId();
            if (!validId(id) || byId.putIfAbsent(AuthService.normalizeId(id), user) != null) {
                throw new IOException("Invalid or duplicate user ID in account profiles.");
            }
        }

        Map<User, String> records = new HashMap<>();
        for (int index = 1; index < rows.size(); index++) {
            String row = rows.get(index);
            List<String> fields = CsvUtil.parseLine(row);
            if (fields.size() != 2 || !validId(fields.get(0))
                    || !PasswordHasher.isEncodedRecord(fields.get(1))
                    || !row.equals(CsvUtil.join(fields.get(0), fields.get(1)))) {
                throw new IOException("Invalid account record on line " + (index + 1) + ".");
            }
            User user = byId.get(AuthService.normalizeId(fields.get(0)));
            if (user == null || records.putIfAbsent(user, fields.get(1)) != null) {
                throw new IOException("Unknown or duplicate account ID on line " + (index + 1) + ".");
            }
        }
        return Map.copyOf(records);
    }

    private static boolean validId(String id) {
        return id != null && !id.isBlank() && id.indexOf('\r') < 0 && id.indexOf('\n') < 0;
    }
}
