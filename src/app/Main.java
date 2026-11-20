package app;

import java.time.LocalDate;
import java.util.List;

import model.BloodRequest;
import model.BloodType;
import model.Donor;
import model.HospitalStaff;
import model.Location;
import model.UrgencyLevel;
import model.WholeBlood;
import service.BalancedMatch;
import service.MatchStrategy;
import service.MatchingService;
import service.MostReliableFirst;
import service.NearestFirst;

public class Main {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();

        Location bmc = new Location("Batangas Medical Center", 13.7700, 121.0500);
        Location batangasCity = new Location("Batangas City", 13.7565, 121.0583);
        HospitalStaff nurse = new HospitalStaff("S001", "Liza Mendoza",
                "09221234567", "Batangas Medical Center", bmc);

        Donor juan  = new Donor("D001", "Juan Dela Cruz", "09171234567",
                BloodType.O_NEG, batangasCity);
        Donor maria = new Donor("D002", "Maria Santos", "09181234567",
                BloodType.A_POS, new Location("Lipa City", 13.9411, 121.1631));
        Donor pedro = new Donor("D003", "Pedro Reyes", "09191234567",
                BloodType.O_POS, new Location("Lemery", 13.8847, 120.9100));
        Donor ana   = new Donor("D004", "Ana Lopez", "09201234567",
                BloodType.B_POS, batangasCity);
        Donor ben   = new Donor("D005", "Ben Cruz", "09211234567",
                BloodType.A_NEG, new Location("Manila", 14.5995, 120.9842));
        Donor rico  = new Donor("D006", "Rico Diaz", "09241234567",
                BloodType.A_POS, batangasCity);
        Donor lea   = new Donor("D007", "Lea Garcia", "09251234567",
                BloodType.O_NEG, batangasCity);

        // Everyone consents except Rico
        for (Donor d : List.of(juan, maria, pedro, ana, ben, lea)) {
            d.giveConsent();
        }
        // Lea donated whole blood 10 days ago, so she is not eligible yet
        lea.recordDonation(new WholeBlood(today.minusDays(10)));
        // Give Maria and Pedro a response history
        for (int i = 0; i < 4; i++) maria.updateReliability(true);   // 0.9
        for (int i = 0; i < 2; i++) pedro.updateReliability(true);   // 0.7

        List<Donor> all = List.of(juan, maria, pedro, ana, ben, rico, lea);
        BloodRequest request = nurse.postRequest("PT-0050", BloodType.A_POS,
                2, UrgencyLevel.URGENT);

        // 1. SCREENING REPORT
        System.out.println("Screening report for " + request.getRequestId()
                + " (needs A+, URGENT, radius "
                + request.getUrgency().getSearchRadiusKm() + " km)");
        for (Donor d : all) {
            System.out.printf("%-15s consent=%-5b canDonate=%-5b compatible=%-5b inRadius=%b%n",
                    d.getName(), d.hasConsented(), d.canDonate(today),
                    request.isCompatibleWith(d),
                    request.isWithinSearchRadius(d.getLocation()));
        }

        MatchingService service = new MatchingService(new NearestFirst());
        List<Donor> eligible = service.findEligibleDonors(all, request, today);
        System.out.println("Eligible donors: " + eligible.size());

        // 2. RANKING with each strategy (polymorphism through the interface)
        BalancedMatch balanced = new BalancedMatch();
        MatchStrategy[] strategies = { new NearestFirst(), new MostReliableFirst(), balanced };
        for (MatchStrategy s : strategies) {
            service.setStrategy(s);
            System.out.println("\n--- " + s.getName() + " ---");
            int rank = 1;
            for (Donor d : service.findMatches(all, request, today)) {
                double km = bmc.distanceKmTo(d.getLocation());
                System.out.printf("%d. %s | %.1f km | reliability %.2f%n",
                        rank++, d.getName(), km, d.getReliabilityScore());
            }
        }

        System.out.println("\nBalanced scores:");
        for (Donor d : balanced.rank(eligible, request)) {
            System.out.printf("%s -> %.2f%n", d.getName(), balanced.score(d, request));
        }

        // 3. ALERTS (using the Balanced strategy)
        service.setStrategy(balanced);
        List<Donor> ranked = service.findMatches(all, request, today);
        List<Donor> alerted = service.sendAlerts(ranked, request);
        System.out.println("\nAlerts sent to " + alerted.size() + " donor(s)");
        System.out.println("Maria's inbox: " + maria.getInbox().get(0));

        // 4. RESPONSES
        service.recordResponse(maria, nurse, request, true);
        service.recordResponse(pedro, nurse, request, false);
        System.out.println("Staff inbox: " + nurse.getInbox().get(0));
        System.out.printf("Maria reliability: %.2f%n", maria.getReliabilityScore());
        System.out.printf("Pedro reliability: %.2f%n", pedro.getReliabilityScore());

        // 5. PRIVACY AND STATE CHECKS
        try {
            rico.receiveAlert("test");
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        request.fulfill();
        try {
            service.findMatches(all, request, today);
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }
}