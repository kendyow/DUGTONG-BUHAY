package service;

import java.time.LocalDate;

import exception.DuplicateRecordException;
import model.Admin;
import model.BloodType;
import model.Donor;
import model.Hospital;
import model.HospitalStaff;
import model.Location;
import model.WholeBlood;

public class SampleData {

    private SampleData() { }

    public static void load(Registry registry) throws DuplicateRecordException {
        LocalDate today = LocalDate.now();
        BloodType[] types = BloodType.values();

        // ----- Hospitals and stock -----
        Hospital bmc = new Hospital("H001", "Batangas Medical Center",
                new Location("Batangas City", 13.7700, 121.0500));
        Hospital lipa = new Hospital("H002", "Lipa Medical Center",
                PlaceCatalog.find("Lipa City"));
        Hospital manila = new Hospital("H003", "Manila General Hospital",
                PlaceCatalog.find("Manila"));

        int[] bmcStock = {2, 12, 0, 8, 1, 15, 4, 20};   // O-, O+, A-, A+, B-, B+, AB-, AB+
        for (int i = 0; i < types.length; i++) {
            if (bmcStock[i] > 0) {
                bmc.addUnits(types[i], bmcStock[i]);
            }
        }
        for (BloodType t : types) {
            lipa.addUnits(t, 10);
            manila.addUnits(t, 30);
        }
        lipa.useUnits(BloodType.O_NEG, 4);
        lipa.useUnits(BloodType.B_NEG, 5);

        registry.addHospital(bmc);
        registry.addHospital(lipa);
        registry.addHospital(manila);

        // ----- Staff and admin -----
        registry.addStaff(new HospitalStaff("S001", "Liza Mendoza", "09221234567",
                "Batangas Medical Center", bmc.getLocation()));
        registry.addStaff(new HospitalStaff("S002", "Ricardo Perez", "09271234567",
                "Lipa Medical Center", lipa.getLocation()));
        registry.addAdmin(new Admin("A001", "Carl Admin", "09231234567"));

        // ----- Donors -----
        Location batangas = PlaceCatalog.find("Batangas City");
        Donor juan  = new Donor("D001", "Juan Dela Cruz", "09171234567", BloodType.O_NEG, batangas);
        Donor maria = new Donor("D002", "Maria Santos",   "09181234567", BloodType.A_POS, PlaceCatalog.find("Lipa City"));
        Donor pedro = new Donor("D003", "Pedro Reyes",    "09191234567", BloodType.O_POS, PlaceCatalog.find("Lemery"));
        Donor ana   = new Donor("D004", "Ana Lopez",      "09201234567", BloodType.B_POS, batangas);
        Donor ben   = new Donor("D005", "Ben Cruz",       "09211234567", BloodType.A_NEG, PlaceCatalog.find("Manila"));
        Donor rico  = new Donor("D006", "Rico Diaz",      "09241234567", BloodType.A_POS, batangas);
        Donor lea   = new Donor("D007", "Lea Garcia",     "09251234567", BloodType.O_NEG, batangas);
        Donor mark  = new Donor("D008", "Mark Tan",       "09261234567", BloodType.B_NEG, PlaceCatalog.find("Tagaytay"));

        // Everyone consents except Rico
        Donor[] consenting = {juan, maria, pedro, ana, ben, lea, mark};
        for (Donor d : consenting) {
            d.giveConsent();
        }
        lea.recordDonation(new WholeBlood(today.minusDays(10)));   // not eligible yet
        for (int i = 0; i < 4; i++) maria.updateReliability(true); // 0.9
        for (int i = 0; i < 2; i++) pedro.updateReliability(true); // 0.7

        Donor[] everyone = {juan, maria, pedro, ana, ben, rico, lea, mark};
        for (Donor d : everyone) {
            registry.addDonor(d);
        }

        // ----- Weekly history for the forecast -----
        StockForecast f = registry.getForecast();
        loadWeeks(f, BloodType.O_NEG, new int[]{6, 8, 7, 9},    new int[]{4, 3, 5, 4});
        loadWeeks(f, BloodType.O_POS, new int[]{10, 9, 11, 10}, new int[]{12, 11, 13, 12});
        loadWeeks(f, BloodType.A_POS, new int[]{7, 8, 6, 7},    new int[]{6, 7, 6, 6});
        loadWeeks(f, BloodType.B_NEG, new int[]{2, 1, 2, 3},    new int[]{1, 1, 0, 1});
    }

    private static void loadWeeks(StockForecast f, BloodType type, int[] used, int[] received) {
        for (int i = 0; i < used.length; i++) {
            f.recordWeek(type, used[i], received[i]);
        }
    }
}