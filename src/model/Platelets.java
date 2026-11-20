package model;

import java.time.LocalDate;

public class Platelets extends Donation {
    private static final int WAIT_DAYS = 14;

    public Platelets(LocalDate donationDate) {
        super(donationDate);
    }

    @Override
    public LocalDate computeNextEligibleDate() {
        return getDonationDate().plusDays(WAIT_DAYS);
    }

    @Override
    public String getTypeName() {
        return "Platelets";
    }
}