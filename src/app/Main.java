package app;

import model.BloodType;
import model.Donor;
import model.Location;

public class Main {
    public static void main(String[] args) {
        Location batangas = new Location("Batangas City", 13.7565, 121.0583);
        Location manila   = new Location("Manila", 14.5995, 120.9842);

        Donor juan = new Donor("D001", "Juan Dela Cruz", "09171234567",
                BloodType.O_NEG, batangas);
        juan.giveConsent();

        System.out.println(juan);
        System.out.println("Blood type: " + juan.getBloodType());
        System.out.println("Lives in: " + juan.getLocation());
        System.out.println("Consented: " + juan.hasConsented());

        // Compatibility test
        System.out.println("O- can donate to AB+? "
                + BloodType.O_NEG.canDonateTo(BloodType.AB_POS));
        System.out.println("A+ can donate to O-?  "
                + BloodType.A_POS.canDonateTo(BloodType.O_NEG));
        System.out.print("A+ can donate to: ");
        for (BloodType t : BloodType.values()) {
            if (BloodType.A_POS.canDonateTo(t)) {
                System.out.print(t + " ");
            }
        }
        System.out.println();

        // Distance test
        System.out.printf("Batangas to Manila: %.1f km%n",
                batangas.distanceKmTo(manila));

        // Validation tests: both should be caught
        try {
            juan.setContactNumber("12345");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        try {
            new Location("Nowhere", 50.0, 121.0);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }
}