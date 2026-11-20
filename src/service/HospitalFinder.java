package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import model.BloodType;
import model.Hospital;
import model.Location;

public class HospitalFinder {

    /** Hospitals that can supply the blood, nearest to 'from' first. */
    public List<Hospital> findNearestWithStock(List<Hospital> hospitals,
                                               BloodType needed, int units,
                                               Location from) {
        if (units <= 0) {
            throw new IllegalArgumentException("Units must be greater than 0.");
        }
        List<Hospital> result = new ArrayList<>();
        for (Hospital h : hospitals) {
            if (h.canSupply(needed, units)) {
                result.add(h);
            }
        }
        result.sort(Comparator.comparingDouble(
                (Hospital h) -> h.getLocation().distanceKmTo(from)));
        return result;
    }
}