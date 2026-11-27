package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import auth.AccountAccess;
import exception.DataStorageException;
import exception.DugtongBuhayException;
import exception.InvalidInputException;
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
import service.DataStore;
import service.HospitalFinder;
import service.MatchingService;
import service.MostReliableFirst;
import service.NearestFirst;
import service.PlaceCatalog;
import service.Registry;

public class ConsoleMenu {
    private final Registry registry;
    private final DataStore store;
    private final AccountAccess accounts;
    private final InputReader in;
    private final PrintWriter out;
    private final HospitalFinder finder = new HospitalFinder();
    private final MatchingService matching = new MatchingService(new BalancedMatch());

    public ConsoleMenu(Registry registry, DataStore store, AccountAccess accounts) {
        this.registry = registry;
        this.store = store;
        this.accounts = accounts;
        this.out = new PrintWriter(System.out, true);
        this.in = new InputReader(out);
    }

    public ConsoleMenu(Registry registry, DataStore store, AccountAccess accounts, InputReader in, PrintWriter out) {
        this.registry = registry;
        this.store = store;
        this.accounts = accounts;
        this.in = in;
        this.out = out;
    }

    public enum Role {
        STAFF("Hospital Staff"), DONOR("Donor"), ADMIN("Admin");

        private final String accountRole;

        Role(String accountRole) {
            this.accountRole = accountRole;
        }
    }

    private String signIn(Role role) throws InvalidInputException {
        String id = in.readText("Account ID: ");
        char[] password = in.readPassword("Password: ");
        try {
            return accounts.login(id, password)
                    .filter(login -> role.accountRole.equals(login.role()))
                    .orElseThrow(() -> new InvalidInputException("Sign-in failed. Check your ID, password, and role."))
                    .userId();
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public void runRole(Role role) {
        try {
            switch (role) {
                case STAFF -> staffMenu();
                case DONOR -> donorMenu();
                case ADMIN -> adminMenu();
            }
        } catch (DugtongBuhayException | IllegalArgumentException | IllegalStateException e) {
            out.println("  ! " + e.getMessage());
        } finally {
            autosave();
        }
    }

    private void autosave() {
        try {
            store.save(registry);
        } catch (DataStorageException e) {
            out.println("  ! Could not save changes: " + e.getMessage());
        }
    }

    // =====================================================================
    //  MAIN MENU
    // =====================================================================
    public void run() {
        boolean running = true;
        while (running) {
            out.println("\n=== DUGTONG-BUHAY ===");
            out.println("1. Find blood nearby (public)");
            out.println("2. Hospital staff");
            out.println("3. Donor");
            out.println("4. Admin");
            out.println("0. Exit");
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
                out.println("  ! " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.println("  ! " + e.getMessage());
            } finally {
                autosave();
            }
        }
        out.println("Salamat! Goodbye.");
    }

    // =====================================================================
    //  PUBLIC: find hospitals that have the blood (no donor data shown)
    // =====================================================================
    private void publicSearch() throws NoMatchFoundException {
        out.println("\n-- Find blood nearby --");
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
        out.println("Hospitals that can supply " + units + " unit(s) for " + type + ":");
        int rank = 1;
        for (Hospital h : found) {
            out.printf("%d. %s | %.1f km | compatible units: %d%n", rank++,
                    h.getName(), h.getLocation().distanceKmTo(here), h.getCompatibleUnits(type));
        }
    }

    private Location chooseLocation(String prompt) {
        List<Location> places = PlaceCatalog.getAll();
        out.println(prompt);
        for (int i = 0; i < places.size(); i++) {
            out.println("  " + (i + 1) + ". " + places.get(i).getName());
        }
        int pick = in.readInt("Choose a place: ", 1, places.size());
        return places.get(pick - 1);
    }

    // =====================================================================
    //  HOSPITAL STAFF
    // =====================================================================
    private void staffMenu() throws DugtongBuhayException {
        String id = signIn(Role.STAFF);
        HospitalStaff staff = registry.findStaff(id);
        Hospital hospital = registry.findHospitalByName(staff.getHospitalName());
        registry.getAuditLog().record(staff.getId(), "Logged in");
        out.println("Welcome, " + staff.getName() + " of " + hospital.getName());

        boolean back = false;
        while (!back) {
            out.println("\n-- Staff menu --");
            out.println("1. Stock board");
            out.println("2. Add stock (units received)");
            out.println("3. Use stock (units given to a patient)");
            out.println("4. Post a blood request and find donors");
            out.println("5. Record a donor's reply");
            out.println("6. Mark a request fulfilled");
            out.println("7. Shortage forecast");
            out.println("8. My inbox");
            out.println("9. Request history");
            out.println("10. Change my password");
            out.println("11. Deactivate my account");
            out.println("0. Back");
            int choice = in.readInt("Choose: ", 0, 11);
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
                    case 9:
                        List<BloodRequest> history = registry.getRequests().stream()
                                .filter(request -> hospital.getName().equalsIgnoreCase(request.getHospitalName()))
                                .toList();
                        in.showRequestHistory(history);
                        break;
                    case 10: changePassword(staff.getId()); break;
                    case 11: back = deactivateAccount(staff.getId()); break;
                    default: back = true;
                }
            } catch (DugtongBuhayException | IllegalArgumentException | IllegalStateException e) {
                out.println("  ! " + e.getMessage());
            } finally {
                autosave();
            }
        }
    }

    private void showStockBoard(Hospital hospital) {
        out.println("\nSTOCK BOARD - " + hospital.getName());
        for (BloodType t : BloodType.values()) {
            out.printf("%-4s %3d units  %s%n", t, hospital.getUnits(t), hospital.getStatus(t));
        }
        out.println("Total units: " + hospital.getTotalUnits());
        out.println("Needs attention: " + hospital.getLowStockTypes());
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
        out.println("  " + type + " now: " + hospital.getUnits(type)
                + " (" + hospital.getStatus(type) + ")");
    }

    private void postRequestAndMatch(HospitalStaff staff) throws DugtongBuhayException {
        out.println("\n-- New blood request (use a patient CODE, never a real name) --");
        String code;
        BloodType type;
        int units;
        UrgencyLevel urgency;
        if (in.hasRequestForm()) {
            InputReader.RequestDetails details = in.showRequestForm(staff);
            if (details == null) {
                out.println("Request cancelled.");
                return;
            }
            code = details.patientCode();
            type = details.bloodType();
            units = details.units();
            urgency = details.urgency();
        } else {
            code = in.readText("Patient code (e.g. PT-0042): ");
            type = in.readBloodType("Blood type needed: ");
            units = in.readInt("Units needed (1-10): ", 1, 10);
            urgency = chooseUrgency();
        }

        BloodRequest request = staff.postRequest(code, type, units, urgency);
        registry.addRequest(request);
        registry.getAuditLog().record(staff.getId(), "Posted " + request.getRequestId()
                + " (" + type + ", " + urgency + ")");
        store.save(registry);
        out.println("Posted: " + request);

        chooseStrategy();
        List<Donor> ranked = matching.findMatches(registry.getDonors(), request, LocalDate.now());
        if (ranked.isEmpty()) {
            throw new NoMatchFoundException("No eligible, consenting donors within "
                    + urgency.getSearchRadiusKm() + " km. Request stays open.");
        }

        out.println("Ranked donors (" + matching.getStrategy().getName() + "):");
        int rank = 1;
        for (Donor d : ranked) {
            out.printf("%d. %s %s | %.1f km | reliability %.2f%n", rank++,
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
        out.println("Alerts sent to " + alerted.size() + " donor(s).");
    }

    private UrgencyLevel chooseUrgency() {
        UrgencyLevel[] levels = UrgencyLevel.values();
        out.println("Urgency levels:");
        for (int i = 0; i < levels.length; i++) {
            out.printf("  %d. %s - radius %.0f km, respond within %d min%n", i + 1,
                    levels[i], levels[i].getSearchRadiusKm(), levels[i].getResponseWindowMinutes());
        }
        int pick = in.readInt("Choose urgency: ", 1, levels.length);
        return levels[pick - 1];
    }

    private void chooseStrategy() {
        out.println("Ranking strategy:");
        out.println("  1. Nearest first");
        out.println("  2. Most reliable first");
        out.println("  3. Balanced (recommended)");
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
        out.println("Open requests:");
        for (int i = 0; i < open.size(); i++) {
            out.println("  " + (i + 1) + ". " + open.get(i));
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
        out.println("Donors who were alerted:");
        for (int i = 0; i < waiting.size(); i++) {
            out.println("  " + (i + 1) + ". " + waiting.get(i).getId()
                    + " " + waiting.get(i).getName());
        }
        int pick = in.readInt("Which donor replied? ", 1, waiting.size());
        Donor donor = waiting.get(pick - 1);
        boolean accepted = in.readYesNo("Did the donor accept? (y/n): ");

        matching.recordResponse(donor, staff, request, accepted);
        registry.removeAlerted(request.getRequestId(), donor);
        registry.getAuditLog().record(staff.getId(), "Donor " + donor.getId()
                + (accepted ? " accepted " : " declined ") + request.getRequestId());
        out.println(accepted
                ? "Accepted. The donor's contact details were sent to your inbox."
                : "Declined. The donor's reliability score was lowered.");
    }

    private void fulfillRequest(HospitalStaff staff, Hospital hospital) throws RecordNotFoundException {
        BloodRequest request = pickOpenRequest(hospital);
        request.fulfill();
        registry.getAuditLog().record(staff.getId(), "Fulfilled " + request.getRequestId());
        out.println(request.getRequestId() + " is now " + request.getStatus() + ".");
    }

    private void showForecast(Hospital hospital) {
        Month month = LocalDate.now().getMonth();
        out.println("\nSHORTAGE FORECAST for " + month + " (" + hospital.getName() + ")");
        for (BloodType t : BloodType.values()) {
            out.printf("%-4s now %3d | projected %6.1f | %s%n", t, hospital.getUnits(t),
                    registry.getForecast().projectedStock(hospital, t, month),
                    registry.getForecast().isShortageRisk(hospital, t, month) ? "AT RISK" : "ok");
        }
    }

    private void showInbox(HospitalStaff staff) {
        List<String> inbox = staff.getInbox();
        if (inbox.isEmpty()) {
            out.println("No messages.");
            return;
        }
        for (String message : inbox) {
            out.println("- " + message);
        }
    }

    // =====================================================================
    //  DONOR
    // =====================================================================
    private void donorMenu() throws DugtongBuhayException {
        out.println("1. Register as a new donor");
        out.println("2. I am already registered");
        out.println("0. Back");
        int choice = in.readInt("Choose: ", 0, 2);
        if (choice == 0) {
            return;
        }
        Donor donor = choice == 1 ? registerDonor() : registry.findDonor(signIn(Role.DONOR));

        boolean back = false;
        while (!back) {
            out.println("\n-- Donor menu: " + donor.getName() + " --");
            out.println("1. My profile and eligibility");
            out.println("2. Give consent to receive alerts");
            out.println("3. Withdraw consent");
            out.println("4. Record a donation");
            out.println("5. My alerts");
            out.println("6. Update my health status");
            out.println("7. Edit my name and mobile number");
            out.println("8. Change my password");
            out.println("9. Deactivate my account");
            out.println("0. Back");
            int pick = in.readInt("Choose: ", 0, 9);
            try {
                switch (pick) {
                    case 1: showProfile(donor); break;
                    case 2:
                        donor.giveConsent();
                        registry.getAuditLog().record(donor.getId(), "Gave consent");
                        out.println("Thank you! You may now receive emergency alerts.");
                        break;
                    case 3:
                        donor.withdrawConsent();
                        registry.getAuditLog().record(donor.getId(), "Withdrew consent");
                        out.println("Consent withdrawn. You will not receive alerts.");
                        break;
                    case 4: recordDonationFor(donor); break;
                    case 5: showAlerts(donor); break;
                    case 6:
                        donor.setHealthyToDonate(in.readYesNo("Are you well and fit to donate? (y/n): "));
                        registry.getAuditLog().record(donor.getId(), "Updated health status");
                        out.println("Status updated.");
                        break;
                    case 7:
                        editDonorProfile(donor);
                        break;
                    case 8:
                        changePassword(donor.getId());
                        break;
                    case 9:
                        back = deactivateAccount(donor.getId());
                        break;
                    default: back = true;
                }
            } catch (DugtongBuhayException | IllegalArgumentException | IllegalStateException e) {
                out.println("  ! " + e.getMessage());
            } finally {
                autosave();
            }
        }
    }

    private void editDonorProfile(Donor donor) throws DugtongBuhayException {
        String name = in.readText("Full name: ");
        String phone = in.readPhone("Mobile number (09XXXXXXXXX): ");
        String previousName = donor.getName();
        String previousPhone = donor.getContactNumber();
        registry.updateDonorProfile(donor, name, phone);
        try {
            store.save(registry);
        } catch (DataStorageException e) {
            registry.updateDonorProfile(donor, previousName, previousPhone);
            throw e;
        }
        registry.getAuditLog().record(donor.getId(), "Updated profile");
        out.println("Profile updated.");
    }

    private Donor registerDonor() throws DugtongBuhayException {
        out.println("\n-- Donor registration --");
        String name = in.readText("Full name: ");
        String phone = in.readPhone("Mobile number (09XXXXXXXXX): ");
        BloodType type = in.readBloodType("Your blood type: ");
        Location place = chooseLocation("Where do you live?");

        Donor donor = new Donor(registry.nextDonorId(), name, phone, type, place);
        char[] password = in.readNewPassword();
        try {
            registry.addDonor(donor);
            // Persist the profile first so a saved credential always has a profile to load.
            store.save(registry);
            try {
                accounts.enroll(donor.getId(), password);
            } catch (IOException e) {
                throw new DataStorageException("Profile " + donor.getId()
                        + " was saved, but its password could not be saved. Ask the local operator to set it up.");
            }
        } finally {
            Arrays.fill(password, '\0');
        }
        registry.getAuditLog().record(donor.getId(), "Registered");
        out.println("Registered! Your Donor ID is " + donor.getId() + ".");
        out.println("You will NOT receive alerts until you give consent (option 2).");
        return donor;
    }

    private void showProfile(Donor donor) {
        LocalDate today = LocalDate.now();
        out.println(donor);
        out.println("  Blood type   : " + donor.getBloodType());
        out.println("  Location     : " + donor.getLocation().getName());
        out.println("  Consent      : " + (donor.hasConsented() ? "YES" : "NO"));
        out.println("  Fit to donate: " + (donor.isHealthyToDonate() ? "YES" : "NO"));
        out.printf("  Reliability  : %.2f%n", donor.getReliabilityScore());
        out.println("  Can donate today? " + donor.canDonate(today));
        out.println("  Next eligible: " + donor.getNextEligibleDate(today));
        if (donor.getDonationHistory().isEmpty()) {
            out.println("  No donations recorded yet.");
        }
        for (Donation d : donor.getDonationHistory()) {
            out.println("  - " + d);
        }
    }

    private void recordDonationFor(Donor donor) {
        out.println("1. Whole Blood   2. Platelets   3. Plasma");
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
        out.println("Saved: " + donation);
    }

    private void showAlerts(Donor donor) {
        List<String> inbox = donor.getInbox();
        if (inbox.isEmpty()) {
            out.println("No alerts.");
            return;
        }
        for (String message : inbox) {
            out.println("- " + message);
        }
    }

    // =====================================================================
    //  ADMIN
    // =====================================================================
    private void adminMenu() throws DugtongBuhayException {
        String id = signIn(Role.ADMIN);
        Admin admin = registry.findAdmin(id);
        out.println("Welcome, " + admin.getName());

        boolean back = false;
        while (!back) {
            out.println("\n-- Admin menu --");
            out.println("1. List donors");
            out.println("2. Withdraw a donor's consent (privacy request)");
            out.println("3. View audit log");
            out.println("4. System summary");
            out.println("5. Change my password");
            out.println("6. Deactivate my account");
            out.println("7. Deactivate or restore an account");
            out.println("8. Create a hospital staff account");
            out.println("0. Back");
            int choice = in.readInt("Choose: ", 0, 8);
            try {
                switch (choice) {
                    case 1: listDonors(); break;
                    case 2: withdrawConsentFor(admin); break;
                    case 3: showAuditLog(); break;
                    case 4: showSummary(); break;
                    case 5: changePassword(admin.getId()); break;
                    case 6: back = deactivateAccount(admin.getId()); break;
                    case 7: manageAccountStatus(admin); break;
                    case 8: createStaffAccount(admin); break;
                    default: back = true;
                }
            } catch (DugtongBuhayException | IllegalArgumentException | IllegalStateException e) {
                out.println("  ! " + e.getMessage());
            } finally {
                autosave();
            }
        }
    }

    private void createStaffAccount(Admin admin) throws DugtongBuhayException {
        String id = in.readText("New staff account ID: ");
        String name = in.readText("Staff full name: ");
        String phone = in.readPhone("Mobile number (09XXXXXXXXX): ");
        String hospitalName = in.readText("Existing hospital name: ");
        Hospital hospital = registry.findHospitalByName(hospitalName);
        if (registry.getDonors().stream().anyMatch(user -> user.getId().equalsIgnoreCase(id))
                || registry.getStaff().stream().anyMatch(user -> user.getId().equalsIgnoreCase(id))
                || registry.getAdmins().stream().anyMatch(user -> user.getId().equalsIgnoreCase(id))) {
            throw new IllegalArgumentException("That account ID is already in use.");
        }
        char[] adminPassword = in.readPassword("Your administrator password: ");
        try {
            if (accounts.login(admin.getId(), adminPassword).filter(login -> "Admin".equals(login.role())).isEmpty()) {
                out.println("Administrator verification failed.");
                return;
            }
        } finally {
            Arrays.fill(adminPassword, '\0');
        }
        char[] staffPassword = in.readNewPassword();
        try {
            HospitalStaff staff = new HospitalStaff(id, name, phone, hospital.getName(), hospital.getLocation());
            registry.addStaff(staff);
            store.save(registry);
            try {
                accounts.enroll(staff.getId(), staffPassword);
            } catch (IOException e) {
                throw new DataStorageException("Staff profile " + staff.getId()
                        + " was saved, but its password could not be saved. Use account setup to complete it.");
            }
            registry.getAuditLog().record(admin.getId(), "Created staff account " + staff.getId());
            out.println("Staff account created: " + staff.getId() + ".");
        } finally {
            Arrays.fill(staffPassword, '\0');
        }
    }

    private void manageAccountStatus(Admin admin) throws DugtongBuhayException {
        String target = in.readText("Account ID: ");
        boolean active = in.readYesNo("Restore this account? Answer no to deactivate it (y/n): ");
        if (!in.readYesNo("Confirm account status change? (y/n): ")) {
            return;
        }
        char[] password = in.readPassword("Your administrator password: ");
        try {
            if (!accounts.setAccountActive(admin.getId(), password, target, active)) {
                out.println("Administrator verification failed.");
                return;
            }
            registry.getAuditLog().record(admin.getId(), (active ? "Restored " : "Deactivated ") + target);
            out.println(active ? "Account restored." : "Account deactivated.");
        } catch (IOException e) {
            throw new DataStorageException("Could not change account status: " + e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private boolean deactivateAccount(String userId) throws DugtongBuhayException {
        if (!in.readYesNo("Deactivate your account and sign out? (y/n): ")) {
            return false;
        }
        char[] password = in.readPassword("Current password: ");
        try {
            if (!accounts.deactivate(userId, password)) {
                out.println("Current password is incorrect.");
                return false;
            }
            registry.getAuditLog().record(userId, "Deactivated account");
            out.println("Account deactivated. You are signed out.");
            return true;
        } catch (IOException e) {
            throw new DataStorageException("Could not deactivate account: " + e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void changePassword(String userId) throws DugtongBuhayException {
        char[] current = in.readPassword("Current password: ");
        try {
            char[] replacement = in.readNewPassword();
            try {
                if (!accounts.changePassword(userId, current, replacement)) {
                    out.println("Current password is incorrect.");
                    return;
                }
                registry.getAuditLog().record(userId, "Changed password");
                out.println("Password changed.");
            } catch (IOException e) {
                throw new DataStorageException("Could not save the new password: " + e.getMessage());
            } finally {
                Arrays.fill(replacement, '\0');
            }
        } finally {
            Arrays.fill(current, '\0');
        }
    }

    private void listDonors() {
        LocalDate today = LocalDate.now();
        for (Donor d : registry.getDonors()) {
            out.printf("%s | %-16s | %-3s | consent=%-5b | eligible=%b%n",
                    d.getId(), d.getName(), d.getBloodType(), d.hasConsented(), d.canDonate(today));
        }
    }

    private void withdrawConsentFor(Admin admin) throws RecordNotFoundException {
        Donor donor = registry.findDonor(in.readText("Donor ID: "));
        admin.forceWithdrawConsent(donor);
        registry.getAuditLog().record(admin.getId(), "Withdrew consent of " + donor.getId());
        out.println("Consent withdrawn for " + donor.getName() + ".");
    }

    private void showAuditLog() {
        List<String> entries = registry.getAuditLog().getEntries();
        if (entries.isEmpty()) {
            out.println("Audit log is empty.");
            return;
        }
        for (String entry : entries) {
            out.println(entry);
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
        out.println("Donors: " + registry.getDonors().size() + " (" + consented + " consenting)");
        out.println("Hospitals: " + registry.getHospitals().size());
        out.println("Requests: " + registry.getRequests().size() + " (" + open + " open)");
    }
}
