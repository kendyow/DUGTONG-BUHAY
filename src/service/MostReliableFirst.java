package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import model.BloodRequest;
import model.Donor;

public class MostReliableFirst implements MatchStrategy {

    @Override
    public List<Donor> rank(List<Donor> donors, BloodRequest request) {
        List<Donor> ranked = new ArrayList<>(donors);
        ranked.sort(Comparator.comparingDouble((Donor d) -> d.getReliabilityScore())
                .reversed()
                .thenComparingDouble(d ->
                        request.getHospitalLocation().distanceKmTo(d.getLocation())));
        return ranked;
    }

    @Override
    public String getName() {
        return "Most Reliable First";
    }
}