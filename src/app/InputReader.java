package app;

import java.io.Console;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Function;

import exception.InvalidInputException;
import model.BloodType;

public class InputReader {

    /** A "recipe" for turning typed text into a value (or failing). */
    private interface Parser<T> {
        T parse(String text) throws InvalidInputException;
    }

    private final Function<String, String> read;
    private final Function<String, char[]> readSecret;
    private final PrintWriter out;

    public InputReader() {
        this(new PrintWriter(System.out, true));
    }

    InputReader(PrintWriter out) {
        this.out = out;
        Console console = System.console();
        if (console != null) {
            read = prompt -> console.readLine("%s", prompt);
            readSecret = prompt -> console.readPassword("%s", prompt);
        } else {
            Scanner scanner = new Scanner(System.in);
            read = prompt -> {
                out.print(prompt);
                out.flush();
                return scanner.nextLine();
            };
            readSecret = prompt -> null;
        }
    }

    public InputReader(PrintWriter out, Function<String, String> read, Function<String, char[]> readSecret) {
        this.out = out;
        this.read = read;
        this.readSecret = readSecret;
    }

    public char[] readPassword(String prompt) throws InvalidInputException {
        char[] password = readSecret.apply(prompt);
        if (password == null) {
            throw new InvalidInputException("Password entry unavailable. Use an interactive terminal or the desktop app.");
        }
        return password;
    }

    public char[] readNewPassword() throws InvalidInputException {
        char[] password = readPassword("New password (at least 15 characters): ");
        boolean accepted = false;
        try {
            if (password.length < 15) {
                throw new InvalidInputException("Use at least 15 characters for the password.");
            }
            char[] confirmation = readPassword("Confirm password: ");
            try {
                if (!Arrays.equals(password, confirmation)) {
                    throw new InvalidInputException("The passwords did not match.");
                }
                accepted = true;
                return password;
            } finally {
                Arrays.fill(confirmation, '\0');
            }
        } finally {
            if (!accepted) {
                Arrays.fill(password, '\0');
            }
        }
    }

    // ---------- parsing rules (static, so they are easy to test) ----------
    public static String parseText(String text) throws InvalidInputException {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException("This field cannot be empty.");
        }
        return text.trim();
    }

    public static int parseInt(String text, int min, int max) throws InvalidInputException {
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Please enter a whole number.");
        }
        if (value < min || value > max) {
            throw new InvalidInputException(
                    "Please enter a number from " + min + " to " + max + ".");
        }
        return value;
    }

    public static BloodType parseBloodType(String text) throws InvalidInputException {
        try {
            return BloodType.fromLabel(text);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(
                    "Valid blood types: O-, O+, A-, A+, B-, B+, AB-, AB+.");
        }
    }

    public static String parsePhone(String text) throws InvalidInputException {
        String phone = text.trim();
        if (!phone.matches("09\\d{9}")) {
            throw new InvalidInputException(
                    "Mobile number must be 11 digits starting with 09.");
        }
        return phone;
    }

    public static boolean parseYesNo(String text) throws InvalidInputException {
        String answer = text.trim().toLowerCase();
        if (answer.equals("y") || answer.equals("yes")) {
            return true;
        }
        if (answer.equals("n") || answer.equals("no")) {
            return false;
        }
        throw new InvalidInputException("Please answer y or n.");
    }

    // ---------- reading (keeps asking until the input is valid) ----------
    private <T> T readValid(String prompt, Parser<T> parser) {
        while (true) {
            try {
                return parser.parse(read.apply(prompt));
            } catch (InvalidInputException e) {
                out.println("  ! " + e.getMessage());
            }
        }
    }

    public String readText(String prompt) {
        return readValid(prompt, t -> parseText(t));
    }

    public int readInt(String prompt, int min, int max) {
        return readValid(prompt, t -> parseInt(t, min, max));
    }

    public BloodType readBloodType(String prompt) {
        return readValid(prompt, t -> parseBloodType(t));
    }

    public String readPhone(String prompt) {
        return readValid(prompt, t -> parsePhone(t));
    }

    public boolean readYesNo(String prompt) {
        return readValid(prompt, t -> parseYesNo(t));
    }
}
