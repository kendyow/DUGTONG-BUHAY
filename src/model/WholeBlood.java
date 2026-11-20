package model;

import java.time.LocalDate;

public class WholeBlood extends Donation {
    private static final int WAIT_MONTHS = 3;

    public WholeBlood(LocalDate donationDate) {
        super(donationDate);
    }

    @Override
    public LocalDate computeNextEligibleDate() {
        return getDonationDate().plusMonths(WAIT_MONTHS);
    }

    @Override
    public String getTypeName() {
        return "Whole Blood";
    }
}