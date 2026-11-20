package app;

import java.util.Scanner;

import exception.InvalidInputException;
import model.BloodType;

public class InputReader {

    /** A "recipe" for turning typed text into a value (or failing). */
    private interface Parser<T> {
        T parse(String text) throws InvalidInputException;
    }

    private final Scanner scanner = new Scanner(System.in);

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
    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private <T> T readValid(String prompt, Parser<T> parser) {
        while (true) {
            try {
                return parser.parse(readLine(prompt));
            } catch (InvalidInputException e) {
                System.out.println("  ! " + e.getMessage());
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