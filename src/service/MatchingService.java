package service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import model.BloodRequest;
import model.Donor;
import model.HospitalStaff;
import model.Notifiable;

public class MatchingService {
    private static final int ALERT_MULTIPLIER = 3;  // alert 3 donors per unit needed

    private MatchStrategy strategy;

    public MatchingService(MatchStrategy strategy) {
        setStrategy(strategy);
    }

    public MatchStrategy getStrategy() {
        return strategy;
    }

    /** Swap the ranking rule at any time (Strategy pattern). */
    public void setStrategy(MatchStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null.");
        }
        this.strategy = strategy;
    }

    /** Step 1: SCREEN. Keep only donors who pass every check. */
    public List<Donor> findEligibleDonors(List<Donor> allDonors,
                                          BloodRequest request, LocalDate today) {
        if (request.getStatus() != BloodRequest.Status.OPEN) {
            throw new IllegalStateException("Request " + request.getRequestId()
                    + " is not open (" + request.getStatus() + ").");
        }
        List<Donor> eligible = new ArrayList<>();
        for (Donor donor : allDonors) {
            boolean passes = donor.hasConsented()
                    && donor.canDonate(today)
                    && request.isCompatibleWith(donor)
                    && request.isWithinSearchRadius(donor.getLocation());
            if (passes) {
                eligible.add(donor);
            }
        }
        return eligible;
    }

    /** Steps 1 + 2: screen, then rank with the current strategy. */
    public List<Donor> findMatches(List<Donor> allDonors,
                                   BloodRequest request, LocalDate today) {
        return strategy.rank(findEligibleDonors(allDonors, request, today), request);
    }

    /** Step 3: ALERT the top donors. Returns who was alerted. */
    public List<Donor> sendAlerts(List<Donor> rankedDonors, BloodRequest request) {
        int limit = Math.min(rankedDonors.size(),
                             request.getUnitsNeeded() * ALERT_MULTIPLIER);
        String message = buildAlertMessage(request);
        List<Donor> alerted = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Donor donor = rankedDonors.get(i);
            deliver(donor, message);
            alerted.add(donor);
        }
        return alerted;
    }

    /** Step 4: RESPOND. Updates reliability; hospital hears about acceptances. */
    public void recordResponse(Donor donor, HospitalStaff staff,
                               BloodRequest request, boolean accepted) {
        donor.updateReliability(accepted);
        if (accepted) {
            // Contact details are shared ONLY after the donor accepts.
            deliver(staff, "Donor " + donor.getId() + " (" + donor.getName()
                    + ") accepted " + request.getRequestId()
                    + ". Contact: " + donor.getContactNumber());
        }
    }

    // Works for ANY Notifiable (polymorphism through the interface)
    private void deliver(Notifiable target, String message) {
        target.receiveAlert(message);
    }

    // The alert deliberately leaves out the patient code and any donor data.
    private String buildAlertMessage(BloodRequest request) {
        return String.format("%s: %s needs %s blood (%d unit(s)). "
                        + "Please respond within %d minutes.",
                request.getUrgency(), request.getHospitalName(),
                request.getBloodTypeNeeded(), request.getUnitsNeeded(),
                request.getUrgency().getResponseWindowMinutes());
    }
}