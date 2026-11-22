package app;

import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import exception.DuplicateRecordException;
import exception.InvalidInputException;
import exception.RecordNotFoundException;
import model.BloodRequest;
import model.BloodType;
import model.Donor;
import model.Hospital;
import model.HospitalStaff;
import model.Location;
import model.Plasma;
import model.Platelets;
import model.UrgencyLevel;
import model.WholeBlood;
import service.BalancedMatch;
import service.CsvUtil;
import service.DataStore;
import service.HospitalFinder;
import service.MatchingService;
import service.MostReliableFirst;
import service.NearestFirst;
import service.Registry;
import service.SampleData;
import service.StockForecast;

public class SystemTests {
    private static int passed = 0;
    private static int failed = 0;

    /** A piece of code that may throw any exception (lets us write short lambdas). */
    private interface Risky {
        void run() throws Exception;
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }

    /** Passes only if the action throws the expected kind of exception. */
    private static void expectError(String name, Class<? extends Exception> expected, Risky action) {
        try {
            action.run();
            check(name, false);          // nothing was thrown: that is a failure
        } catch (Exception e) {
            check(name, expected.isInstance(e));
        }
    }

    private static boolean approx(double a, double b) {
        return Math.abs(a - b) < 0.01;
    }

    public static void main(String[] args) {
        try {
            testBloodType();
            testValidation();
            testEligibility();
            testRequests();
            testMatching();
            testStockAndForecast();
            testCsv();
            testPersistenceAndRegistry();
        } catch (Exception e) {
            failed++;
            System.out.println("TEST RUN STOPPED BY AN UNEXPECTED ERROR: " + e);
        }
        System.out.println("\nRESULT: " + passed + " passed, " + failed + " failed");
    }

    // =====================================================================
    private static void testBloodType() {
        check("BT-01 O- can donate to AB+", BloodType.O_NEG.canDonateTo(BloodType.AB_POS));
        check("BT-02 A+ cannot donate to O-", !BloodType.A_POS.canDonateTo(BloodType.O_NEG));

        int recipients = 0;
        for (BloodType t : BloodType.values()) {
            if (BloodType.AB_POS.canDonateTo(t)) {
                recipients++;
            }
        }
        check("BT-03 AB+ can donate only to AB+", recipients == 1);
        check("BT-04 fromLabel ignores case and spaces", BloodType.fromLabel(" o- ") == BloodType.O_NEG);
        expectError("BT-05 invalid blood type is rejected", IllegalArgumentException.class,
                () -> BloodType.fromLabel("XX"));
    }

    // =====================================================================
    private static void testValidation() throws Exception {
        Location batangas = new Location("Batangas City", 13.7565, 121.0583);

        expectError("VA-01 location outside the Philippines", IllegalArgumentException.class,
                () -> new Location("Nowhere", 50.0, 121.0));
        expectError("VA-02 bad mobile number", IllegalArgumentException.class,
                () -> new Donor("D1", "Ann Test", "12345", BloodType.A_POS, batangas));
        expectError("VA-03 empty name", IllegalArgumentException.class,
                () -> new Donor("D1", "   ", "09171234567", BloodType.A_POS, batangas));
        expectError("VA-04 parsePhone rejects short number", InvalidInputException.class,
                () -> InputReader.parsePhone("0912"));
        expectError("VA-05 parseInt rejects text", InvalidInputException.class,
                () -> InputReader.parseInt("abc", 1, 5));
        expectError("VA-06 parseInt rejects out-of-range number", InvalidInputException.class,
                () -> InputReader.parseInt("9", 1, 5));
        expectError("VA-07 parseYesNo rejects other answers", InvalidInputException.class,
                () -> InputReader.parseYesNo("maybe"));
        expectError("VA-08 parseBloodType rejects Z+", InvalidInputException.class,
                () -> InputReader.parseBloodType("Z+"));
        check("VA-09 valid phone is accepted",
                InputReader.parsePhone("09171234567").equals("09171234567"));
    }

    // =====================================================================
    private static void testEligibility() {
        LocalDate today = LocalDate.now();
        Location batangas = new Location("Batangas City", 13.7565, 121.0583);

        check("EL-01 whole blood waits 3 months",
                new WholeBlood(today.minusDays(10)).computeNextEligibleDate()
                        .equals(today.minusDays(10).plusMonths(3)));
        check("EL-02 platelets wait 14 days",
                new Platelets(today).computeNextEligibleDate().equals(today.plusDays(14)));
        check("EL-03 plasma waits 28 days",
                new Plasma(today).computeNextEligibleDate().equals(today.plusDays(28)));

        Donor donor = new Donor("D1", "Ann Test", "09171234567", BloodType.A_POS, batangas);
        check("EL-04 new donor can donate", donor.canDonate(today));

        donor.recordDonation(new WholeBlood(today.minusDays(10)));
        check("EL-05 recent donation blocks donating today", !donor.canDonate(today));
        check("EL-06 donor can donate after the waiting period", donor.canDonate(today.plusMonths(4)));

        Donor sick = new Donor("D2", "Sick Test", "09181234567", BloodType.A_POS, batangas);
        sick.setHealthyToDonate(false);
        check("EL-07 unhealthy donor cannot donate", !sick.canDonate(today));

        expectError("EL-08 future donation date is rejected", IllegalArgumentException.class,
                () -> new WholeBlood(today.plusDays(1)));
        expectError("EL-09 donation history is read-only", UnsupportedOperationException.class,
                () -> donor.getDonationHistory().add(new Plasma(today)));
        check("EL-10 consent is off by default", !donor.hasConsented());
    }

    // =====================================================================
    private static void testRequests() {
        Location hosp = new Location("Test Hospital", 13.77, 121.05);
        HospitalStaff nurse = new HospitalStaff("S1", "Nurse Test", "09221234567",
                "Test Hospital", hosp);

        expectError("RQ-01 zero units is rejected", IllegalArgumentException.class,
                () -> nurse.postRequest("PT-1", BloodType.A_POS, 0, UrgencyLevel.URGENT));
        expectError("RQ-02 eleven units is rejected", IllegalArgumentException.class,
                () -> nurse.postRequest("PT-1", BloodType.A_POS, 11, UrgencyLevel.URGENT));

        BloodRequest request = nurse.postRequest("PT-2", BloodType.A_POS, 2, UrgencyLevel.URGENT);
        check("RQ-03 new request is OPEN", request.getStatus() == BloodRequest.Status.OPEN);

        request.fulfill();
        check("RQ-04 request becomes FULFILLED", request.getStatus() == BloodRequest.Status.FULFILLED);
        expectError("RQ-05 fulfilling twice is rejected", IllegalStateException.class,
                () -> request.fulfill());

        check("RQ-06 CRITICAL searches wider than ROUTINE",
                UrgencyLevel.CRITICAL.getSearchRadiusKm() > UrgencyLevel.ROUTINE.getSearchRadiusKm());

        Donor donor = new Donor("D1", "Ann Test", "09171234567", BloodType.A_POS, hosp);
        check("RQ-07 staff can post requests, donors cannot",
                nurse.canPostRequests() && !donor.canPostRequests());
    }

    // =====================================================================
    private static void testMatching() {
        LocalDate today = LocalDate.now();
        Location batangas = new Location("Batangas City", 13.7565, 121.0583);
        Location lipa = new Location("Lipa City", 13.9411, 121.1631);
        Location manila = new Location("Manila", 14.5995, 120.9842);
        Location hosp = new Location("Test Hospital", 13.77, 121.05);
        HospitalStaff nurse = new HospitalStaff("S1", "Nurse Test", "09221234567",
                "Test Hospital", hosp);

        Donor near = new Donor("D1", "Near Donor", "09170000001", BloodType.A_POS, batangas);
        near.giveConsent();

        Donor reliable = new Donor("D2", "Reliable Donor", "09170000002", BloodType.A_POS, lipa);
        reliable.giveConsent();
        for (int i = 0; i < 4; i++) {
            reliable.updateReliability(true);          // 0.5 -> about 0.9
        }

        Donor far = new Donor("D3", "Far Donor", "09170000003", BloodType.A_POS, manila);
        far.giveConsent();

        Donor noConsent = new Donor("D4", "No Consent", "09170000004", BloodType.A_POS, batangas);

        Donor wrongType = new Donor("D5", "Wrong Type", "09170000005", BloodType.B_POS, batangas);
        wrongType.giveConsent();

        Donor recent = new Donor("D6", "Recent Donor", "09170000006", BloodType.A_POS, batangas);
        recent.giveConsent();
        recent.recordDonation(new WholeBlood(today.minusDays(10)));

        List<Donor> all = List.of(near, reliable, far, noConsent, wrongType, recent);
        BloodRequest request = nurse.postRequest("PT-3", BloodType.A_POS, 1, UrgencyLevel.URGENT);
        MatchingService service = new MatchingService(new NearestFirst());

        List<Donor> eligible = service.findEligibleDonors(all, request, today);
        check("MT-01 screening keeps only the 2 valid donors",
                eligible.size() == 2 && eligible.contains(near) && eligible.contains(reliable));

        check("MT-02 NearestFirst puts the closest donor first",
                service.findMatches(all, request, today).get(0) == near);

        service.setStrategy(new MostReliableFirst());
        check("MT-03 MostReliableFirst puts the most reliable donor first",
                service.findMatches(all, request, today).get(0) == reliable);

        service.setStrategy(new BalancedMatch());
        check("MT-04 Balanced puts the best mixed score first",
                service.findMatches(all, request, today).get(0) == reliable);

        List<Donor> ranked = new MostReliableFirst().rank(eligible, request);
        check("MT-05 ranking returns a new list and leaves the original alone",
                eligible.get(0) == near && ranked.get(0) == reliable);

        List<Donor> alerted = service.sendAlerts(service.findMatches(all, request, today), request);
        check("MT-06 alerts go to both eligible donors",
                alerted.size() == 2 && near.getInbox().size() == 1);
        check("MT-07 alert text does not reveal the patient code",
                !near.getInbox().get(0).contains("PT-3"));

        service.recordResponse(near, nurse, request, true);
        check("MT-08 accepting raises reliability", approx(near.getReliabilityScore(), 0.6));
        check("MT-09 staff receives the donor's contact only after acceptance",
                nurse.getInbox().size() == 1 && nurse.getInbox().get(0).contains("09170000001"));

        service.recordResponse(reliable, nurse, request, false);
        check("MT-10 declining lowers reliability and sends no contact details",
                approx(reliable.getReliabilityScore(), 0.8) && nurse.getInbox().size() == 1);

        expectError("MT-11 donor without consent cannot receive alerts", IllegalStateException.class,
                () -> noConsent.receiveAlert("test"));

        request.fulfill();
        expectError("MT-12 a closed request cannot be matched", IllegalStateException.class,
                () -> service.findEligibleDonors(all, request, today));
    }

    // =====================================================================
    private static void testStockAndForecast() {
        Hospital h = new Hospital("H1", "Test Hospital", new Location("Batangas City", 13.77, 121.05));

        check("ST-01 new hospital is empty and OUT",
                h.getTotalUnits() == 0 && h.getStatus(BloodType.O_NEG) == Hospital.StockStatus.OUT);

        h.addUnits(BloodType.O_NEG, 5);
        check("ST-02 5 units is LOW", h.getStatus(BloodType.O_NEG) == Hospital.StockStatus.LOW);

        h.addUnits(BloodType.B_NEG, 2);
        check("ST-03 2 units is CRITICAL", h.getStatus(BloodType.B_NEG) == Hospital.StockStatus.CRITICAL);

        h.addUnits(BloodType.A_POS, 20);
        check("ST-04 20 units is OK", h.getStatus(BloodType.A_POS) == Hospital.StockStatus.OK);

        check("ST-05 a B- patient can use O- and B- stock (5 + 2 = 7)",
                h.getCompatibleUnits(BloodType.B_NEG) == 7);
        check("ST-06 canSupply checks compatible units",
                h.canSupply(BloodType.B_NEG, 7) && !h.canSupply(BloodType.B_NEG, 8));

        expectError("ST-07 using more than the stock is rejected", IllegalStateException.class,
                () -> h.useUnits(BloodType.A_NEG, 1));
        expectError("ST-08 adding zero units is rejected", IllegalArgumentException.class,
                () -> h.addUnits(BloodType.O_POS, 0));

        // Hospital search
        Hospital nearHospital = new Hospital("H2", "Near Hospital",
                new Location("Batangas City", 13.7565, 121.0583));
        Hospital farHospital = new Hospital("H3", "Far Hospital",
                new Location("Manila", 14.5995, 120.9842));
        Hospital emptyHospital = new Hospital("H4", "Empty Hospital",
                new Location("Lipa City", 13.9411, 121.1631));
        nearHospital.addUnits(BloodType.B_NEG, 10);
        farHospital.addUnits(BloodType.B_NEG, 30);

        List<Hospital> found = new HospitalFinder().findNearestWithStock(
                List.of(farHospital, emptyHospital, nearHospital), BloodType.B_NEG, 4,
                new Location("Batangas City", 13.7565, 121.0583));
        check("FN-01 search returns only hospitals with enough stock", found.size() == 2);
        check("FN-02 search sorts nearest first", found.get(0) == nearHospital);
        check("FN-03 hospital without stock is excluded", !found.contains(emptyHospital));

        // Forecast
        StockForecast f = new StockForecast();
        f.recordWeek(BloodType.O_NEG, 6, 4);
        f.recordWeek(BloodType.O_NEG, 8, 3);
        f.recordWeek(BloodType.O_NEG, 7, 5);
        f.recordWeek(BloodType.O_NEG, 9, 4);

        check("FC-01 usage is the 3-week moving average (8.0)",
                approx(f.forecastUsage(BloodType.O_NEG), 8.0));
        check("FC-02 December donations are 4.0 x 0.8 = 3.2",
                approx(f.forecastDonations(BloodType.O_NEG, Month.DECEMBER), 3.2));
        check("FC-03 December forecast is lower than March",
                f.forecastDonations(BloodType.O_NEG, Month.DECEMBER)
                        < f.forecastDonations(BloodType.O_NEG, Month.MARCH));
        check("FC-04 projected stock = 5 + 3.2 - 8 = 0.2",
                approx(f.projectedStock(h, BloodType.O_NEG, Month.DECEMBER), 0.2));
        check("FC-05 low projected stock is flagged AT RISK",
                f.isShortageRisk(h, BloodType.O_NEG, Month.DECEMBER));
        check("FC-06 well-stocked type with no history is not at risk",
                !f.isShortageRisk(h, BloodType.A_POS, Month.DECEMBER));
        expectError("FC-07 negative weekly units are rejected", IllegalArgumentException.class,
                () -> f.recordWeek(BloodType.O_POS, -1, 0));
    }

    // =====================================================================
    private static void testCsv() {
        String line = CsvUtil.join("D009", "Dela Cruz, Juan", "He said \"hi\"", 5);

        check("CSV-01 a name with a comma is quoted", line.contains("\"Dela Cruz, Juan\""));
        check("CSV-02 join then parse returns the original fields",
                CsvUtil.parseLine(line).equals(List.of("D009", "Dela Cruz, Juan", "He said \"hi\"", "5")));
        check("CSV-03 empty fields are kept", CsvUtil.parseLine("a,,c").size() == 3);
    }

    // =====================================================================
    private static void testPersistenceAndRegistry() throws Exception {
        String folder = "test-data";          // a separate folder, so your real data/ is untouched
        deleteFolder(folder);

        Registry r1 = new Registry();
        SampleData.load(r1);
        HospitalStaff nurse = r1.findStaff("S001");
        BloodRequest posted = nurse.postRequest("PT-9", BloodType.A_POS, 1, UrgencyLevel.URGENT);
        r1.addRequest(posted);

        DataStore store = new DataStore(folder);
        check("PS-01 no saved data before the first save", !store.hasSavedData());
        store.save(r1);
        check("PS-02 saved data exists after saving", store.hasSavedData());

        Registry r2 = new Registry();
        store.load(r2);
        check("PS-03 same number of donors after loading",
                r2.getDonors().size() == r1.getDonors().size());
        check("PS-04 hospital stock is kept (62 units)",
                r2.findHospitalByName("Batangas Medical Center").getTotalUnits() == 62);
        check("PS-05 reliability score is kept",
                approx(r2.findDonor("D002").getReliabilityScore(), 0.9));
        check("PS-06 consent is kept",
                r2.findDonor("D001").hasConsented() && !r2.findDonor("D006").hasConsented());
        check("PS-07 donation history is kept",
                r2.findDonor("D007").getDonationHistory().size() == 1);
        check("PS-08 open request keeps its ID and status",
                r2.getRequests().size() == 1
                        && r2.getRequests().get(0).getRequestId().equals(posted.getRequestId())
                        && r2.getRequests().get(0).getStatus() == BloodRequest.Status.OPEN);

        Location batangas = new Location("Batangas City", 13.7565, 121.0583);
        expectError("PS-09 duplicate donor ID is rejected", DuplicateRecordException.class,
                () -> r1.addDonor(new Donor("D001", "Copy", "09990000000", BloodType.A_POS, batangas)));
        expectError("PS-10 unknown donor ID is reported", RecordNotFoundException.class,
                () -> r1.findDonor("D999"));
        expectError("PS-11 duplicate mobile number is rejected", DuplicateRecordException.class,
                () -> r1.addDonor(new Donor("D099", "Copy Phone", "09171234567", BloodType.A_POS, batangas)));

        deleteFolder(folder);
    }

    private static void deleteFolder(String name) {
        File dir = new File(name);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        dir.delete();
    }
}