package app;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import exception.DugtongBuhayException;
import exception.NoMatchFoundException;
import exception.RecordNotFoundException;
import model.Admin;
import model.BloodRequest;
import model.BloodType;
import model.Donation;
import model.Donor;
import model.Hospital;
import model.HospitalStaff;
import model.Location;
import model.Plasma;
import model.Platelets;
import model.UrgencyLevel;
import model.WholeBlood;
import service.BalancedMatch;
import service.HospitalFinder;
import service.MatchingService;
import service.MostReliableFirst;
import service.NearestFirst;
import service.PlaceCatalog;
import service.Registry;

public class ConsoleMenu {
    private final Registry registry;
    private final InputReader in = new InputReader();
    private final HospitalFinder finder = new HospitalFinder();
    private final MatchingService matching = new MatchingService(new BalancedMatch());

    public ConsoleMenu(Registry registry) {
        this.registry = registry;
    }

    // =====================================================================
    //  MAIN MENU
    // =====================================================================
    public void run() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== DUGTONG-BUHAY ===");
            System.out.println("1. Find blood nearby (public)");
            System.out.println("2. Hospital staff");
            System.out.println("3. Donor");
            System.out.println("4. Admin");
            System.out.println("0. Exit");
            int choice = in.readInt("Choose: ", 0, 4);
            try {
                switch (choice) {
                    case 1: publicSearch(); break;
                    case 2: staffMenu(); break;
                    case 3: donorMenu(); break;
                    case 4: adminMenu(); break;
                    default: running = false;
                }
            } catch (DugtongBuhayException e) {
                System.out.println("  ! " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
        System.out.println("Salamat! Goodbye.");
    }

    // =====================================================================
    //  PUBLIC: find hospitals that have the blood (no donor data shown)
    // =====================================================================
    private void publicSearch() throws NoMatchFoundException {
        System.out.println("\n-- Find blood nearby --");
        BloodType type = in.readBloodType("Blood type needed (e.g. O-, A+): ");
        int units = in.readInt("Units needed (1-10): ", 1, 10);
        Location here = chooseLocation("Where are you?");

        List<Hospital> found = finder.findNearestWithStock(
                registry.getHospitals(), type, units, here);
        registry.getAuditLog().record("Public", "Searched " + type + " near " + here.getName());

        if (found.isEmpty()) {
            throw new NoMatchFoundException(
                    "No hospital in the system can supply " + units + " unit(s) for " + type + " right now.");
        }
        System.out.println("Hospitals that can supply " + units + " unit(s) for " + type + ":");
        int rank = 1;
        for (Hospital h : found) {
            System.out.printf("%d. %s | %.1f km | compatible units: %d%n", rank++,
                    h.getName(), h.getLocation().distanceKmTo(here), h.getCompatibleUnits(type));
        }
    }

    private Location chooseLocation(String prompt) {
        List<Location> places = PlaceCatalog.getAll();
        System.out.println(prompt);
        for (int i = 0; i < places.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + places.get(i).getName());
        }
        int pick = in.readInt("Choose a place: ", 1, places.size());
        return places.get(pick - 1);
    }

    // =====================================================================
    //  HOSPITAL STAFF
    // =====================================================================
    private void staffMenu() throws DugtongBuhayException {
        String id = in.readText("Staff ID (try S001): ");
        HospitalStaff staff = registry.findStaff(id);
        Hospital hospital = registry.findHospitalByName(staff.getHospitalName());
        registry.getAuditLog().record(staff.getId(), "Logged in");
        System.out.println("Welcome, " + staff.getName() + " of " + hospital.getName());

        boolean back = false;
        while (!back) {
            System.out.println("\n-- Staff menu --");
            System.out.println("1. Stock board");
            System.out.println("2. Add stock (units received)");
            System.out.println("3. Use stock (units given to a patient)");
            System.out.println("4. Post a blood request and find donors");
            System.out.println("5. Record a donor's reply");
            System.out.println("6. Mark a request fulfilled");
            System.out.println("7. Shortage forecast");
            System.out.println("8. My inbox");
            System.out.println("0. Back");
            int choice = in.readInt("Choose: ", 0, 8);
            try {
                switch (choice) {
                    case 1: showStockBoard(hospital); break;
                    case 2: changeStock(hospital, staff, true); break;
                    case 3: changeStock(hospital, staff, false); break;
                    case 4: postRequestAndMatch(staff); break;
                    case 5: recordReply(staff, hospital); break;
                    case 6: fulfillRequest(staff, hospital); break;
                    case 7: showForecast(hospital); break;
                    case 8: showInbox(staff); break;
                    default: back = true;
                }
            } catch (DugtongBuhayException | IllegalArgumentException | IllegalStateException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    private void showStockBoard(Hospital hospital) {
        System.out.println("\nSTOCK BOARD - " + hospital.getName());
        for (BloodType t : BloodType.values()) {
            System.out.printf("%-4s %3d units  %s%n", t, hospital.getUnits(t), hospital.getStatus(t));
        }
        System.out.println("Total units: " + hospital.getTotalUnits());
        System.out.println("Needs attention: " + hospital.getLowStockTypes());
    }

    private void changeStock(Hospital hospital, HospitalStaff staff, boolean adding) {
        BloodType type = in.readBloodType("Blood type: ");
        int units = in.readInt("Units (1-100): ", 1, 100);
        if (adding) {
            hospital.addUnits(type, units);
            registry.getAuditLog().record(staff.getId(), "Added " + units + " x " + type);
        } else {
            hospital.useUnits(type, units);      // may throw IllegalStateException
            registry.getAuditLog().record(staff.getId(), "Used " + units + " x " + type);
        }
        System.out.println("  " + type + " now: " + hospital.getUnits(type)
                + " (" + hospital.getStatus(type) + ")");
    }

    private void postRequestAndMatch(HospitalStaff staff) throws NoMatchFoundException {
        System.out.println("\n-- New blood request (use a patient CODE, never a real name) --");
        String code = in.readText("Patient code (e.g. PT-0042): ");
        BloodType type = in.readBloodType("Blood type needed: ");
        int units = in.readInt("Units needed (1-10): ", 1, 10);
        UrgencyLevel urgency = chooseUrgency();

        BloodRequest request = staff.postRequest(code, type, units, urgency);
        registry.addRequest(request);
        registry.getAuditLog().record(staff.getId(), "Posted " + request.getRequestId()
                + " (" + type + ", " + urgency + ")");
        System.out.println("Posted: " + request);

        chooseStrategy();
        List<Donor> ranked = matching.findMatches(registry.getDonors(), request, LocalDate.now());
        if (ranked.isEmpty()) {
            throw new NoMatchFoundException("No eligible, consenting donors within "
                    + urgency.getSearchRadiusKm() + " km. Request stays open.");
        }

        System.out.println("Ranked donors (" + matching.getStrategy().getName() + "):");
        int rank = 1;
        for (Donor d : ranked) {
            System.out.printf("%d. %s %s | %.1f km | reliability %.2f%n", rank++,
                    d.getId(), d.getName(),
                    staff.getHospitalLocation().distanceKmTo(d.getLocation()),
                    d.getReliabilityScore());
        }

        List<Donor> alerted = matching.sendAlerts(ranked, request);
        registry.recordAlerted(request.getRequestId(), alerted);
        for (Donor d : alerted) {
            registry.getAuditLog().record(staff.getId(),
                    "Alerted donor " + d.getId() + " for " + request.getRequestId());
        }
        System.out.println("Alerts sent to " + alerted.size() + " donor(s).");
    }

    private UrgencyLevel chooseUrgency() {
        UrgencyLevel[] levels = UrgencyLevel.values();
        System.out.println("Urgency levels:");
        for (int i = 0; i < levels.length; i++) {
            System.out.printf("  %d. %s - radius %.0f km, respond within %d min%n", i + 1,
                    levels[i], levels[i].getSearchRadiusKm(), levels[i].getResponseWindowMinutes());
        }
        int pick = in.readInt("Choose urgency: ", 1, levels.length);
        return levels[pick - 1];
    }

    private void chooseStrategy() {
        System.out.println("Ranking strategy:");
        System.out.println("  1. Nearest first");
        System.out.println("  2. Most reliable first");
        System.out.println("  3. Balanced (recommended)");
        int pick = in.readInt("Choose: ", 1, 3);
        switch (pick) {
            case 1: matching.setStrategy(new NearestFirst()); break;
            case 2: matching.setStrategy(new MostReliableFirst()); break;
            default: matching.setStrategy(new BalancedMatch());
        }
    }

    private BloodRequest pickOpenRequest(Hospital hospital) throws RecordNotFoundException {
        List<BloodRequest> open = registry.getOpenRequestsFor(hospital.getName());
        if (open.isEmpty()) {
            throw new RecordNotFoundException("No open requests for " + hospital.getName() + ".");
        }
        System.out.println("Open requests:");
        for (int i = 0; i < open.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + open.get(i));
        }
        int pick = in.readInt("Choose a request: ", 1, open.size());
        return open.get(pick - 1);
    }

    /** Simulates a donor answering an alert (in real life the donor would reply by phone/app). */
    private void recordReply(HospitalStaff staff, Hospital hospital) throws DugtongBuhayException {
        BloodRequest request = pickOpenRequest(hospital);
        List<Donor> waiting = registry.getAlerted(request.getRequestId());
        if (waiting.isEmpty()) {
            throw new RecordNotFoundException(
                    "No donors are waiting to reply to " + request.getRequestId() + ".");
        }
        System.out.println("Donors who were alerted:");
        for (int i = 0; i < waiting.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + waiting.get(i).getId()
                    + " " + waiting.get(i).getName());
        }
        int pick = in.readInt("Which donor replied? ", 1, waiting.size());
        Donor donor = waiting.get(pick - 1);
        boolean accepted = in.readYesNo("Did the donor accept? (y/n): ");

        matching.recordResponse(donor, staff, request, accepted);
        registry.removeAlerted(request.getRequestId(), donor);
        registry.getAuditLog().record(staff.getId(), "Donor " + donor.getId()
                + (accepted ? " accepted " : " declined ") + request.getRequestId());
        System.out.println(accepted
                ? "Accepted. The donor's contact details were sent to your inbox."
                : "Declined. The donor's reliability score was lowered.");
    }

    private void fulfillRequest(HospitalStaff staff, Hospital hospital) throws RecordNotFoundException {
        BloodRequest request = pickOpenRequest(hospital);
        request.fulfill();
        registry.getAuditLog().record(staff.getId(), "Fulfilled " + request.getRequestId());
        System.out.println(request.getRequestId() + " is now " + request.getStatus() + ".");
    }

    private void showForecast(Hospital hospital) {
        Month month = LocalDate.now().getMonth();
        System.out.println("\nSHORTAGE FORECAST for " + month + " (" + hospital.getName() + ")");
        for (BloodType t : BloodType.values()) {
            System.out.printf("%-4s now %3d | projected %6.1f | %s%n", t, hospital.getUnits(t),
                    registry.getForecast().projectedStock(hospital, t, month),
                    registry.getForecast().isShortageRisk(hospital, t, month) ? "AT RISK" : "ok");
        }
    }

    private void showInbox(HospitalStaff staff) {
        List<String> inbox = staff.getInbox();
        if (inbox.isEmpty()) {
            System.out.println("No messages.");
            return;
        }
        for (String message : inbox) {
            System.out.println("- " + message);
        }
    }

    // =====================================================================
    //  DONOR
    // =====================================================================
    private void donorMenu() throws DugtongBuhayException {
        System.out.println("1. Register as a new donor");
        System.out.println("2. I am already registered");
        int choice = in.readInt("Choose: ", 1, 2);
        Donor donor = (choice == 1) ? registerDonor()
                                    : registry.findDonor(in.readText("Donor ID (try D001): "));

        boolean back = false;
        while (!back) {
            System.out.println("\n-- Donor menu: " + donor.getName() + " --");
            System.out.println("1. My profile and eligibility");
            System.out.println("2. Give consent to receive alerts");
            System.out.println("3. Withdraw consent");
            System.out.println("4. Record a donation");
            System.out.println("5. My alerts");
            System.out.println("6. Update my health status");
            System.out.println("0. Back");
            int pick = in.readInt("Choose: ", 0, 6);
            try {
                switch (pick) {
                    case 1: showProfile(donor); break;
                    case 2:
                        donor.giveConsent();
                        registry.getAuditLog().record(donor.getId(), "Gave consent");
                        System.out.println("Thank you! You may now receive emergency alerts.");
                        break;
                    case 3:
                        donor.withdrawConsent();
                        registry.getAuditLog().record(donor.getId(), "Withdrew consent");
                        System.out.println("Consent withdrawn. You will not receive alerts.");
                        break;
                    case 4: recordDonationFor(donor); break;
                    case 5: showAlerts(donor); break;
                    case 6:
                        donor.setHealthyToDonate(in.readYesNo("Are you well and fit to donate? (y/n): "));
                        registry.getAuditLog().record(donor.getId(), "Updated health status");
                        System.out.println("Status updated.");
                        break;
                    default: back = true;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    private Donor registerDonor() throws DugtongBuhayException {
        System.out.println("\n-- Donor registration --");
        String name = in.readText("Full name: ");
        String phone = in.readPhone("Mobile number (09XXXXXXXXX): ");
        BloodType type = in.readBloodType("Your blood type: ");
        Location place = chooseLocation("Where do you live?");

        Donor donor = new Donor(registry.nextDonorId(), name, phone, type, place);
        registry.addDonor(donor);   // may throw DuplicateRecordException
        registry.getAuditLog().record(donor.getId(), "Registered");
        System.out.println("Registered! Your Donor ID is " + donor.getId() + ".");
        System.out.println("You will NOT receive alerts until you give consent (option 2).");
        return donor;
    }

    private void showProfile(Donor donor) {
        LocalDate today = LocalDate.now();
        System.out.println(donor);
        System.out.println("  Blood type   : " + donor.getBloodType());
        System.out.println("  Location     : " + donor.getLocation().getName());
        System.out.println("  Consent      : " + (donor.hasConsented() ? "YES" : "NO"));
        System.out.println("  Fit to donate: " + (donor.isHealthyToDonate() ? "YES" : "NO"));
        System.out.printf("  Reliability  : %.2f%n", donor.getReliabilityScore());
        System.out.println("  Can donate today? " + donor.canDonate(today));
        System.out.println("  Next eligible: " + donor.getNextEligibleDate(today));
        if (donor.getDonationHistory().isEmpty()) {
            System.out.println("  No donations recorded yet.");
        }
        for (Donation d : donor.getDonationHistory()) {
            System.out.println("  - " + d);
        }
    }

    private void recordDonationFor(Donor donor) {
        System.out.println("1. Whole Blood   2. Platelets   3. Plasma");
        int type = in.readInt("Donation type: ", 1, 3);
        int daysAgo = in.readInt("How many days ago? (0-365): ", 0, 365);
        LocalDate date = LocalDate.now().minusDays(daysAgo);

        Donation donation;
        switch (type) {
            case 1: donation = new WholeBlood(date); break;
            case 2: donation = new Platelets(date); break;
            default: donation = new Plasma(date);
        }
        donor.recordDonation(donation);
        registry.getAuditLog().record(donor.getId(), "Recorded " + donation.getTypeName());
        System.out.println("Saved: " + donation);
    }

    private void showAlerts(Donor donor) {
        List<String> inbox = donor.getInbox();
        if (inbox.isEmpty()) {
            System.out.println("No alerts.");
            return;
        }
        for (String message : inbox) {
            System.out.println("- " + message);
        }
    }

    // =====================================================================
    //  ADMIN
    // =====================================================================
    private void adminMenu() throws DugtongBuhayException {
        String id = in.readText("Admin ID (try A001): ");
        Admin admin = registry.findAdmin(id);
        System.out.println("Welcome, " + admin.getName());

        boolean back = false;
        while (!back) {
            System.out.println("\n-- Admin menu --");
            System.out.println("1. List donors");
            System.out.println("2. Withdraw a donor's consent (privacy request)");
            System.out.println("3. View audit log");
            System.out.println("4. System summary");
            System.out.println("0. Back");
            int choice = in.readInt("Choose: ", 0, 4);
            try {
                switch (choice) {
                    case 1: listDonors(); break;
                    case 2: withdrawConsentFor(admin); break;
                    case 3: showAuditLog(); break;
                    case 4: showSummary(); break;
                    default: back = true;
                }
            } catch (DugtongBuhayException | IllegalArgumentException | IllegalStateException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    private void listDonors() {
        LocalDate today = LocalDate.now();
        for (Donor d : registry.getDonors()) {
            System.out.printf("%s | %-16s | %-3s | consent=%-5b | eligible=%b%n",
                    d.getId(), d.getName(), d.getBloodType(), d.hasConsented(), d.canDonate(today));
        }
    }

    private void withdrawConsentFor(Admin admin) throws RecordNotFoundException {
        Donor donor = registry.findDonor(in.readText("Donor ID: "));
        admin.forceWithdrawConsent(donor);
        registry.getAuditLog().record(admin.getId(), "Withdrew consent of " + donor.getId());
        System.out.println("Consent withdrawn for " + donor.getName() + ".");
    }

    private void showAuditLog() {
        List<String> entries = registry.getAuditLog().getEntries();
        if (entries.isEmpty()) {
            System.out.println("Audit log is empty.");
            return;
        }
        for (String entry : entries) {
            System.out.println(entry);
        }
    }

    private void showSummary() {
        int consented = 0;
        for (Donor d : registry.getDonors()) {
            if (d.hasConsented()) {
                consented++;
            }
        }
        int open = 0;
        for (BloodRequest r : registry.getRequests()) {
            if (r.getStatus() == BloodRequest.Status.OPEN) {
                open++;
            }
        }
        System.out.println("Donors: " + registry.getDonors().size() + " (" + consented + " consenting)");
        System.out.println("Hospitals: " + registry.getHospitals().size());
        System.out.println("Requests: " + registry.getRequests().size() + " (" + open + " open)");
    }
}