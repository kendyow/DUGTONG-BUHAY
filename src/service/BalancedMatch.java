package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import model.BloodRequest;
import model.Donor;

public class BalancedMatch implements MatchStrategy {
    private static final double RELIABILITY_WEIGHT = 0.6;
    private static final double DISTANCE_WEIGHT = 0.4;

    /** Score from 0.0 to 1.0. Higher is better. */
    public double score(Donor donor, BloodRequest request) {
        double radius = request.getUrgency().getSearchRadiusKm();
        double distance = request.getHospitalLocation().distanceKmTo(donor.getLocation());
        double closeness = Math.max(0.0, 1.0 - distance / radius);
        return RELIABILITY_WEIGHT * donor.getReliabilityScore()
             + DISTANCE_WEIGHT * closeness;
    }

    @Override
    public List<Donor> rank(List<Donor> donors, BloodRequest request) {
        List<Donor> ranked = new ArrayList<>(donors);
        ranked.sort(Comparator.comparingDouble((Donor d) -> score(d, request)).reversed());
        return ranked;
    }

    @Override
    public String getName() {
        return "Balanced";
    }
}