package service;

import java.time.Month;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import model.BloodType;
import model.Hospital;

public class StockForecast {
    private static final int WINDOW_WEEKS = 3;
    private static final double HOLIDAY_FACTOR = 0.8;  // simulation assumption

    private final Map<BloodType, List<Integer>> weeklyUsage = new EnumMap<>(BloodType.class);
    private final Map<BloodType, List<Integer>> weeklyDonations = new EnumMap<>(BloodType.class);

    public StockForecast() {
        for (BloodType type : BloodType.values()) {
            weeklyUsage.put(type, new ArrayList<>());
            weeklyDonations.put(type, new ArrayList<>());
        }
    }

    /** Record one week of history: units used and units received. */
    public void recordWeek(BloodType type, int unitsUsed, int unitsReceived) {
        if (unitsUsed < 0 || unitsReceived < 0) {
            throw new IllegalArgumentException("Units cannot be negative.");
        }
        weeklyUsage.get(type).add(unitsUsed);
        weeklyDonations.get(type).add(unitsReceived);
    }

    /** Average of the most recent weeks (0 if there is no history). */
    private double movingAverage(List<Integer> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        int count = Math.min(WINDOW_WEEKS, values.size());
        double sum = 0;
        for (int i = values.size() - count; i < values.size(); i++) {
            sum += values.get(i);
        }
        return sum / count;
    }

    /** Fewer donors come forward in holiday months (Dec, Jan, Apr). */
    public double seasonFactor(Month month) {
        switch (month) {
            case DECEMBER:
            case JANUARY:
            case APRIL:
                return HOLIDAY_FACTOR;
            default:
                return 1.0;
        }
    }

    public double forecastUsage(BloodType type) {
        return movingAverage(weeklyUsage.get(type));
    }

    public double forecastDonations(BloodType type, Month month) {
        return movingAverage(weeklyDonations.get(type)) * seasonFactor(month);
    }

    public double projectedStock(Hospital hospital, BloodType type, Month month) {
        return hospital.getUnits(type)
                + forecastDonations(type, month)
                - forecastUsage(type);
    }

    public boolean isShortageRisk(Hospital hospital, BloodType type, Month month) {
        return projectedStock(hospital, type, month) < Hospital.LOW_THRESHOLD;
    }

    public List<BloodType> getAtRiskTypes(Hospital hospital, Month month) {
        List<BloodType> atRisk = new ArrayList<>();
        for (BloodType type : BloodType.values()) {
            if (isShortageRisk(hospital, type, month)) {
                atRisk.add(type);
            }
        }
        return atRisk;
    }
}