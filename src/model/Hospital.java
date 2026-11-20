package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Hospital {

    public enum StockStatus { OK, LOW, CRITICAL, OUT }

    public static final int LOW_THRESHOLD = 10;      // 10 units or fewer = LOW
    public static final int CRITICAL_THRESHOLD = 3;  // 3 units or fewer = CRITICAL

    private final String id;
    private final String name;
    private final Location location;
    private final Map<BloodType, Integer> stock = new EnumMap<>(BloodType.class);

    public Hospital(String id, String name, Location location) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Hospital ID is required.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Hospital name is required.");
        }
        if (location == null) {
            throw new IllegalArgumentException("Hospital location is required.");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.location = location;
        for (BloodType type : BloodType.values()) {
            stock.put(type, 0);      // every type starts at zero
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Location getLocation() { return location; }

    public int getUnits(BloodType type) {
        if (type == null) {
            throw new IllegalArgumentException("Blood type is required.");
        }
        return stock.get(type);
    }

    public void addUnits(BloodType type, int units) {
        if (units <= 0) {
            throw new IllegalArgumentException("Units must be greater than 0.");
        }
        stock.put(type, getUnits(type) + units);
    }

    public void useUnits(BloodType type, int units) {
        if (units <= 0) {
            throw new IllegalArgumentException("Units must be greater than 0.");
        }
        int have = getUnits(type);
        if (units > have) {
            throw new IllegalStateException("Not enough " + type
                    + " in stock (have " + have + ", need " + units + ").");
        }
        stock.put(type, have - units);
    }

    /** Traffic-light status for one blood type. */
    public StockStatus getStatus(BloodType type) {
        int units = getUnits(type);
        if (units == 0) return StockStatus.OUT;
        if (units <= CRITICAL_THRESHOLD) return StockStatus.CRITICAL;
        if (units <= LOW_THRESHOLD) return StockStatus.LOW;
        return StockStatus.OK;
    }

    /** Every blood type that is not OK (the low-stock warnings). */
    public List<BloodType> getLowStockTypes() {
        List<BloodType> low = new ArrayList<>();
        for (BloodType type : BloodType.values()) {
            if (getStatus(type) != StockStatus.OK) {
                low.add(type);
            }
        }
        return low;
    }

    public int getTotalUnits() {
        int total = 0;
        for (int units : stock.values()) {
            total += units;
        }
        return total;
    }

    /** Units of ALL types that could be given to a patient of this type. */
    public int getCompatibleUnits(BloodType recipient) {
        int total = 0;
        for (BloodType donorType : BloodType.values()) {
            if (donorType.canDonateTo(recipient)) {
                total += stock.get(donorType);
            }
        }
        return total;
    }

    public boolean canSupply(BloodType recipient, int units) {
        return getCompatibleUnits(recipient) >= units;
    }

    /** Read-only copy for displaying the board. */
    public Map<BloodType, Integer> getStockSnapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(stock));
    }

    @Override
    public String toString() {
        return name + " (" + location.getName() + ")";
    }
}