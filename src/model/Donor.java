package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Donor extends User implements Notifiable {
    private final BloodType bloodType;
    private final Location location;
    private boolean consentToBeContacted;
    private boolean healthyToDonate;
    private double reliabilityScore;      // 0.0 to 1.0
    private final List<Donation> donationHistory = new ArrayList<>();
    private final List<String> inbox = new ArrayList<>();

    public Donor(String id, String name, String contactNumber,
                 BloodType bloodType, Location location) {
        super(id, name, contactNumber);
        if (bloodType == null) {
            throw new IllegalArgumentException("Blood type is required.");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location is required.");
        }
        this.bloodType = bloodType;
        this.location = location;
        this.consentToBeContacted = false;  // privacy: opt-in only
        this.healthyToDonate = true;
        this.reliabilityScore = 0.5;        // neutral starting score
    }

    @Override
    public String getRole() {
        return "Donor";
    }
    @Override
    public boolean canPostRequests() {
        return false;   // donors respond to requests; they don't post them
    }

    public BloodType getBloodType() { return bloodType; }
    public Location getLocation() { return location; }
    public boolean hasConsented() { return consentToBeContacted; }
    public boolean isHealthyToDonate() { return healthyToDonate; }
    public double getReliabilityScore() { return reliabilityScore; }

    public void giveConsent() { consentToBeContacted = true; }
    public void withdrawConsent() { consentToBeContacted = false; }

    public void setHealthyToDonate(boolean healthy) {
        this.healthyToDonate = healthy;
    }

    /** Read-only view: outside code cannot modify the list. */
    public List<Donation> getDonationHistory() {
        return Collections.unmodifiableList(donationHistory);
    }

    /** Accepts ANY Donation subtype (polymorphism). */
    public void recordDonation(Donation donation) {
        if (donation == null) {
            throw new IllegalArgumentException("Donation cannot be null.");
        }
        donationHistory.add(donation);
    }

    /**
     * The ONLY way to check eligibility. The donor must be healthy AND every
     * past donation's waiting period must be over by the given date.
     */
    public boolean canDonate(LocalDate date) {
        if (!healthyToDonate) {
            return false;
        }
        for (Donation d : donationHistory) {
            if (date.isBefore(d.computeNextEligibleDate())) {
                return false;
            }
        }
        return true;
    }

    /** Earliest date on or after 'from' when this donor may donate again. */
    public LocalDate getNextEligibleDate(LocalDate from) {
        LocalDate next = from;
        for (Donation d : donationHistory) {
            LocalDate candidate = d.computeNextEligibleDate();
            if (candidate.isAfter(next)) {
                next = candidate;
            }
        }
        return next;
    }

    /** Responding to alerts raises the score; ignoring them lowers it. */
    public void updateReliability(boolean responded) {
        double change = responded ? 0.1 : -0.1;
        reliabilityScore = Math.max(0.0, Math.min(1.0, reliabilityScore + change));
    }

        /** Second line of defense for privacy: no consent, no alerts. */
    @Override
    public void receiveAlert(String message) {
        if (!consentToBeContacted) {
            throw new IllegalStateException("Donor has not consented to alerts.");
        }
        inbox.add(message);
    }

    @Override
    public List<String> getInbox() {
        return Collections.unmodifiableList(inbox);
    }
}