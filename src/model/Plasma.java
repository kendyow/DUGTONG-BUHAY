package model;

import java.time.LocalDate;

public class Plasma extends Donation {
    private static final int WAIT_DAYS = 28;

    public Plasma(LocalDate donationDate) {
        super(donationDate);
    }

    @Override
    public LocalDate computeNextEligibleDate() {
        return getDonationDate().plusDays(WAIT_DAYS);
    }

    @Override
    public String getTypeName() {
        return "Plasma";
    }
}