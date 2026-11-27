package app;

import java.util.Arrays;

import auth.PasswordHasher;

/** Run with java -cp bin app.PasswordHasherTests. */
public final class PasswordHasherTests {
    private static int passed;

    private PasswordHasherTests() {
    }

    public static void main(String[] args) {
        char[] password = "Sampaguita ñ 🩸 ".toCharArray();
        char[] wrong = "Sampaguita ñ 🩸".toCharArray();
        try {
            String first = PasswordHasher.hash(password);
            String second = PasswordHasher.hash(password);
            check("Correct password verifies", PasswordHasher.verify(password, first));
            check("Changed whitespace fails verification", !PasswordHasher.verify(wrong, first));
            check("Identical passwords get different salts",
                    !first.substring(0, first.lastIndexOf('$'))
                            .equals(second.substring(0, second.lastIndexOf('$'))));
            check("Second salted record verifies", PasswordHasher.verify(password, second));
            char last = first.charAt(first.length() - 1);
            String tampered = first.substring(0, first.length() - 1) + (last == '0' ? '1' : '0');
            check("Modified hash is rejected", !PasswordHasher.verify(password, tampered));
            check("Unsupported iteration count is rejected",
                    !PasswordHasher.verify(password, first.replace("$600000$", "$2147483647$")));
            check("Truncated record is rejected",
                    !PasswordHasher.verify(password, first.substring(0, first.length() - 1)));
            check("Invalid hex is rejected", !PasswordHasher.verify(password, first.substring(0, first.length() - 1) + "z"));
            check("Null record is rejected", !PasswordHasher.verify(password, null));
            check("Null password is rejected", !PasswordHasher.verify(null, first));
            check("Empty password fails verification", !PasswordHasher.verify(new char[0], first));
            try {
                PasswordHasher.hash(new char[0]);
                throw new AssertionError("Empty password was hashed");
            } catch (IllegalArgumentException expected) {
                check("Empty password cannot be hashed", true);
            }
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(wrong, '\0');
        }
        System.out.println("RESULT: " + passed + " passed");
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name);
        }
        passed++;
        System.out.println("PASS  " + name);
    }
}
