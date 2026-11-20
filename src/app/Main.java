package app;

import java.util.List;

import model.Admin;
import model.BloodRequest;
import model.BloodType;
import model.Donor;
import model.HospitalStaff;
import model.Location;
import model.UrgencyLevel;
import model.User;

public class Main {
    public static void main(String[] args) {
        Location bmc = new Location("Batangas Medical Center", 13.7700, 121.0500);
        Location batangas = new Location("Batangas City", 13.7565, 121.0583);

        HospitalStaff nurse = new HospitalStaff("S001", "Liza Mendoza",
                "09221234567", "Batangas Medical Center", bmc);
        Admin admin = new Admin("A001", "Carl Admin", "09231234567");
        Donor juan = new Donor("D001", "Juan Dela Cruz", "09171234567",
                BloodType.O_NEG, batangas);
        juan.giveConsent();

        // Polymorphism: one list, three kinds of users
        List<User> users = List.of(nurse, admin, juan);
        for (User u : users) {
            System.out.println(u + " | can post requests? " + u.canPostRequests());
        }

        // Posting requests
        BloodRequest r1 = nurse.postRequest("PT-0042", BloodType.O_NEG, 2, UrgencyLevel.CRITICAL);
        BloodRequest r2 = nurse.postRequest("PT-0043", BloodType.B_POS, 1, UrgencyLevel.ROUTINE);
        System.out.println(r1);
        System.out.println(r2);

        // Urgency settings
        for (UrgencyLevel level : UrgencyLevel.values()) {
            System.out.println(level + ": radius " + level.getSearchRadiusKm()
                    + " km, response window " + level.getResponseWindowMinutes() + " min");
        }

        // Compatibility and radius
        System.out.println("Juan (O-) compatible with " + r2.getRequestId()
                + " (B+)? " + r2.isCompatibleWith(juan));
        System.out.println("Juan within radius of " + r2.getRequestId() + "? "
                + r2.isWithinSearchRadius(juan.getLocation()));

        // Controlled status changes
        r1.fulfill();
        System.out.println(r1.getRequestId() + " status: " + r1.getStatus());
        try {
            r1.fulfill();
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // Validation
        try {
            nurse.postRequest("PT-0044", BloodType.A_POS, 0, UrgencyLevel.URGENT);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // Admin privacy action
        System.out.println("Juan consent before: " + juan.hasConsented());
        admin.forceWithdrawConsent(juan);
        System.out.println("Juan consent after:  " + juan.hasConsented());
    }
}