package service;

import java.util.List;

import model.BloodRequest;
import model.Donor;

public interface MatchStrategy {
    /** Returns a NEW list of donors, best candidate first. */
    List<Donor> rank(List<Donor> donors, BloodRequest request);

    String getName();
}