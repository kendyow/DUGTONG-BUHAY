package model;

import java.time.LocalDate;

public abstract class Donation {
    private final LocalDate donationDate;

    protected Donation(LocalDate donationDate) {
        if (donationDate == null) {
            throw new IllegalArgumentException("Donation date is required.");
        }
        if (donationDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Donation date cannot be in the future.");
        }
        this.donationDate = donationDate;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    /** Each donation type decides its own waiting period (polymorphism). */
    public abstract LocalDate computeNextEligibleDate();

    /** Display name of the donation type. */
    public abstract String getTypeName();

    @Override
    public String toString() {
        return getTypeName() + " on " + donationDate
                + " (next eligible: " + computeNextEligibleDate() + ")";
    }
}