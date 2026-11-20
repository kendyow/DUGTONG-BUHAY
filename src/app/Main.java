package app;

import java.time.Month;
import java.util.List;

import model.BloodType;
import model.Hospital;
import model.Location;
import service.HospitalFinder;
import service.StockForecast;

public class Main {

    private static void loadWeeks(StockForecast forecast, BloodType type,
                                  int[] used, int[] received) {
        for (int i = 0; i < used.length; i++) {
            forecast.recordWeek(type, used[i], received[i]);
        }
    }

    public static void main(String[] args) {
        BloodType[] types = BloodType.values();

        // Hospitals
        Hospital bmc = new Hospital("H001", "Batangas Medical Center",
                new Location("Batangas City", 13.7700, 121.0500));
        Hospital lipa = new Hospital("H002", "Lipa Medical Center",
                new Location("Lipa City", 13.9411, 121.1631));
        Hospital manila = new Hospital("H003", "Manila General Hospital",
                new Location("Manila", 14.5995, 120.9842));

        // Stock in enum order: O-, O+, A-, A+, B-, B+, AB-, AB+
        int[] bmcStock = {2, 12, 0, 8, 1, 15, 4, 20};
        for (int i = 0; i < types.length; i++) {
            if (bmcStock[i] > 0) bmc.addUnits(types[i], bmcStock[i]);
        }
        for (BloodType t : types) {
            lipa.addUnits(t, 10);
            manila.addUnits(t, 30);
        }
        lipa.useUnits(BloodType.O_NEG, 4);   // Lipa: O- = 6
        lipa.useUnits(BloodType.B_NEG, 5);   // Lipa: B- = 5

        // 1. STOCK BOARD
        System.out.println("STOCK BOARD - " + bmc.getName());
        for (BloodType t : types) {
            System.out.printf("%-4s %3d units  %s%n", t, bmc.getUnits(t), bmc.getStatus(t));
        }
        System.out.println("Total units: " + bmc.getTotalUnits());
        System.out.println("Needs attention: " + bmc.getLowStockTypes());

        // 2. SHORTAGE FORECAST
        StockForecast forecast = new StockForecast();
        loadWeeks(forecast, BloodType.O_NEG, new int[]{6, 8, 7, 9},     new int[]{4, 3, 5, 4});
        loadWeeks(forecast, BloodType.O_POS, new int[]{10, 9, 11, 10},  new int[]{12, 11, 13, 12});
        loadWeeks(forecast, BloodType.A_POS, new int[]{7, 8, 6, 7},     new int[]{6, 7, 6, 6});
        loadWeeks(forecast, BloodType.B_NEG, new int[]{2, 1, 2, 3},     new int[]{1, 1, 0, 1});

        Month month = Month.DECEMBER;
        System.out.println("\nSHORTAGE FORECAST for " + month + " (" + bmc.getName() + ")");
        for (BloodType t : types) {
            System.out.printf("%-4s now %3d | proj %6.1f | %s%n", t, bmc.getUnits(t),
                    forecast.projectedStock(bmc, t, month),
                    forecast.isShortageRisk(bmc, t, month) ? "AT RISK" : "ok");
        }
        System.out.println("At risk: " + forecast.getAtRiskTypes(bmc, month));
        System.out.printf("O- projected: December %.1f vs March %.1f%n",
                forecast.projectedStock(bmc, BloodType.O_NEG, Month.DECEMBER),
                forecast.projectedStock(bmc, BloodType.O_NEG, Month.MARCH));

        // 3. PUBLIC SEARCH: nearest hospitals that can supply B- (4 units)
        Location user = new Location("Batangas City", 13.7565, 121.0583);
        System.out.println("\nBMC units compatible with a B- patient: "
                + bmc.getCompatibleUnits(BloodType.B_NEG));
        System.out.println("BMC can supply 4 units of B-? " + bmc.canSupply(BloodType.B_NEG, 4));

        HospitalFinder finder = new HospitalFinder();
        List<Hospital> found = finder.findNearestWithStock(
                List.of(bmc, lipa, manila), BloodType.B_NEG, 4, user);
        System.out.println("Hospitals that can supply 4 unit(s) for B-, nearest first:");
        int rank = 1;
        for (Hospital h : found) {
            System.out.printf("%d. %s | %.1f km | compatible units: %d%n", rank++, h.getName(),
                    h.getLocation().distanceKmTo(user), h.getCompatibleUnits(BloodType.B_NEG));
        }

        // 4. STOCK CHANGES AND VALIDATION
        bmc.useUnits(BloodType.O_POS, 2);
        System.out.println("\nO+ after using 2 units: " + bmc.getUnits(BloodType.O_POS)
                + " (" + bmc.getStatus(BloodType.O_POS) + ")");
        try {
            bmc.useUnits(BloodType.A_NEG, 1);
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        try {
            bmc.addUnits(BloodType.O_NEG, 0);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }
}