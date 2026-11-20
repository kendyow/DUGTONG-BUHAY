package model;

public enum BloodType {
    //        label  hasA   hasB   rhPositive
    O_NEG ("O-",  false, false, false),
    O_POS ("O+",  false, false, true),
    A_NEG ("A-",  true,  false, false),
    A_POS ("A+",  true,  false, true),
    B_NEG ("B-",  false, true,  false),
    B_POS ("B+",  false, true,  true),
    AB_NEG("AB-", true,  true,  false),
    AB_POS("AB+", true,  true,  true);

    private final String label;
    private final boolean hasA;
    private final boolean hasB;
    private final boolean rhPositive;

    BloodType(String label, boolean hasA, boolean hasB, boolean rhPositive) {
        this.label = label;
        this.hasA = hasA;
        this.hasB = hasB;
        this.rhPositive = rhPositive;
    }

    public String getLabel() {
        return label;
    }

    /** True if this donor type can give red cells to the recipient type. */
    public boolean canDonateTo(BloodType recipient) {
        boolean aOk  = !this.hasA       || recipient.hasA;
        boolean bOk  = !this.hasB       || recipient.hasB;
        boolean rhOk = !this.rhPositive || recipient.rhPositive;
        return aOk && bOk && rhOk;
    }

    /** Converts text like "o-" or "AB+" into a BloodType. */
    public static BloodType fromLabel(String text) {
        for (BloodType type : values()) {
            if (type.label.equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid blood type: " + text);
    }

    @Override
    public String toString() {
        return label;
    }
}