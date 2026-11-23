package app;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import auth.AuthService;
import auth.PasswordHasher;
import model.Admin;
import model.HospitalStaff;
import model.Location;
import model.User;

/** Run with java -cp bin app.AuthServiceTests. */
public final class AuthServiceTests {
    private AuthServiceTests() {
    }

    public static void main(String[] args) {
        char[] password = "Example admin password".toCharArray();
        char[] staffPassword = "Example staff password".toCharArray();
        try {
            Admin admin = new Admin("A001", "Example Admin", "09000000001");
            HospitalStaff staff = new HospitalStaff("S001", "Example Staff", "09000000002",
                    "Example Hospital", new Location("Batangas City", 13.76, 121.06));
            String adminRecord = PasswordHasher.hash(password);
            Map<User, String> records = new HashMap<>();
            records.put(admin, adminRecord);
            records.put(staff, PasswordHasher.hash(staffPassword));
            AuthService auth = new AuthService(records);
            records.clear();

            check("Login identifies the admin and accepts case-insensitive IDs",
                    auth.login(" a001 ", password).equals(Optional.of(new AuthService.Login("A001", "Admin"))));
            check("Login identifies hospital staff",
                    auth.login("S001", staffPassword).equals(Optional.of(new AuthService.Login("S001", "Hospital Staff"))));
            check("A different account's password is rejected", auth.login("S001", password).isEmpty());
            check("An unknown ID is rejected", auth.login("A999", password).isEmpty());
            check("A blank ID is rejected", auth.login(" ", password).isEmpty());
            check("A missing password is rejected", auth.login("A001", null).isEmpty());
            check("A damaged password record is rejected",
                    new AuthService(Map.of(admin, "invalid")).login("A001", password).isEmpty());

            HospitalStaff duplicate = new HospitalStaff("a001", "Duplicate Staff", "09000000003",
                    "Example Hospital", staff.getHospitalLocation());
            try {
                new AuthService(Map.of(admin, adminRecord, duplicate, adminRecord));
                throw new AssertionError("Duplicate account ID was accepted");
            } catch (IllegalArgumentException expected) {
                System.out.println("PASS  Duplicate account ID across roles is rejected");
            }
            System.out.println("RESULT: 8 passed");
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(staffPassword, '\0');
        }
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name);
        }
        System.out.println("PASS  " + name);
    }
}
