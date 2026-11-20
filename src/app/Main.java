package app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import model.BloodType;
import model.Donation;
import model.Donor;
import model.Location;
import model.Plasma;
import model.Platelets;
import model.WholeBlood;

public class Main {
    public static void main(String[] args) {
        Location batangas = new Location("Batangas City", 13.7565, 121.0583);
        Location manila   = new Location("Manila", 14.5995, 120.9842);

        Donor juan = new Donor("D001", "Juan Dela Cruz", "09171234567",
                BloodType.O_NEG, batangas);
        juan.recordDonation(new WholeBlood(LocalDate.of(2026, 10, 1)));

        Donor maria = new Donor("D002", "Maria Santos", "09181234567",
                BloodType.B_POS, manila);
        maria.recordDonation(new Platelets(LocalDate.of(2026, 11, 10)));

        Donor pedro = new Donor("D003", "Pedro Reyes", "09191234567",
                BloodType.A_POS, batangas);
        pedro.recordDonation(new Plasma(LocalDate.of(2026, 10, 1)));

        Donor ana = new Donor("D004", "Ana Lopez", "09201234567",
                BloodType.AB_POS, manila);

        List<Donor> donors = List.of(juan, maria, pedro, ana);
        LocalDate today = LocalDate.of(2026, 11, 20);

        for (Donor d : donors) {
            System.out.println(d);
            if (d.getDonationHistory().isEmpty()) {
                System.out.println("  No donations yet");
            }
            for (Donation don : d.getDonationHistory()) {
                System.out.println("  " + don);
            }
            LocalDate next = d.getNextEligibleDate(today);
            long days = ChronoUnit.DAYS.between(today, next);
            System.out.println("  Can donate on " + today + "? " + d.canDonate(today));
            System.out.println("  Next eligible: " + next + " (in " + days + " days)");
        }

        // Extra test 1: an unhealthy donor is never eligible
        juan.setHealthyToDonate(false);
        System.out.println("Juan unhealthy, can donate 2027-06-01? "
                + juan.canDonate(LocalDate.of(2027, 6, 1)));

        // Extra test 2: a future donation date must be rejected
        try {
            new WholeBlood(LocalDate.now().plusDays(5));
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }
}