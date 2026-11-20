package service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import exception.DataStorageException;
import exception.DugtongBuhayException;
import model.Admin;
import model.BloodRequest;
import model.BloodType;
import model.Donation;
import model.Donor;
import model.Hospital;
import model.HospitalStaff;
import model.Location;
import model.Plasma;
import model.Platelets;
import model.UrgencyLevel;
import model.WholeBlood;

public class DataStore {
    private final Path folder;

    public DataStore(String folderName) {
        this.folder = Paths.get(folderName);
    }

    /** True if a previous run already saved data. */
    public boolean hasSavedData() {
        return Files.exists(folder.resolve("hospitals.csv"));
    }

    // =====================================================================
    //  SAVE
    // =====================================================================
    public void save(Registry registry) throws DataStorageException {
        try {
            Files.createDirectories(folder);
            saveHospitals(registry);
            saveStaff(registry);
            saveAdmins(registry);
            saveDonors(registry);
            saveDonations(registry);
            saveRequests(registry);
            saveAlerted(registry);
            saveForecast(registry);
            saveAudit(registry);
        } catch (IOException e) {
            throw new DataStorageException("Could not save data: " + e.getMessage());
        }
    }

    private void writeFile(String fileName, String header, List<String> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(header);
        lines.addAll(rows);
        Files.write(folder.resolve(fileName), lines, StandardCharsets.UTF_8);
    }

    private void saveHospitals(Registry registry) throws IOException {
        List<String> rows = new ArrayList<>();
        for (Hospital h : registry.getHospitals()) {
            List<Object> fields = new ArrayList<>();
            fields.add(h.getId());
            fields.add(h.getName());
            fields.add(h.getLocation().getName());
            fields.add(h.getLocation().getLatitude());
            fields.add(h.getLocation().getLongitude());
            for (BloodType type : BloodType.values()) {
                fields.add(h.getUnits(type));
            }
            rows.add(CsvUtil.join(fields.toArray()));
        }
        writeFile("hospitals.csv",
                "id,name,location,latitude,longitude,O-,O+,A-,A+,B-,B+,AB-,AB+", rows);
    }

    private void saveStaff(Registry registry) throws IOException {
        List<String> rows = new ArrayList<>();
        for (HospitalStaff s : registry.getStaff()) {
            rows.add(CsvUtil.join(s.getId(), s.getName(), s.getContactNumber(), s.getHospitalName()));
        }
        writeFile("staff.csv", "id,name,phone,hospital", rows);
    }

    private void saveAdmins(Registry registry) throws IOException {
        List<String> rows = new ArrayList<>();
        for (Admin a : registry.getAdmins()) {
            rows.add(CsvUtil.join(a.getId(), a.getName(), a.getContactNumber()));
        }
        writeFile("admins.csv", "id,name,phone", rows);
    }

    private void saveDonors(Registry registry) throws IOException {
        List<String> rows = new ArrayList<>();
        for (Donor d : registry.getDonors()) {
            double reliability = Math.round(d.getReliabilityScore() * 100) / 100.0;  // tidy 0.8999999 -> 0.9
            rows.add(CsvUtil.join(d.getId(), d.getName(), d.getContactNumber(),
                    d.getBloodType().getLabel(), d.getLocation().getName(),
                    d.getLocation().getLatitude(), d.getLocation().getLongitude(),
                    d.hasConsented(), d.isHealthyToDonate(), reliability));
        }
        writeFile("donors.csv",
                "id,name,phone,bloodType,location,latitude,longitude,consent,healthy,reliability", rows);
    }

    private void saveDonations(Registry registry) throws IOException {
        List<String> rows = new ArrayList<>();
        for (Donor d : registry.getDonors()) {
            for (Donation donation : d.getDonationHistory()) {
                rows.add(CsvUtil.join(d.getId(), donation.getTypeName(), donation.getDonationDate()));
            }
        }
        writeFile("donations.csv", "donorId,type,date", rows);
    }

    private void saveRequests(Registry registry) throws IOException {
        List<String> rows = new ArrayList<>();
        for (BloodRequest q : registry.getRequests()) {
            rows.add(CsvUtil.join(q.getRequestId(), q.getPatientCode(),
                    q.getBloodTypeNeeded().getLabel(), q.getUnitsNeeded(),
                    q.getUrgency().name(), q.getHospitalName(),
                    q.getRequestedByStaffId(), q.getCreatedAt(), q.getStatus().name()));
        }
        writeFile("requests.csv",
                "id,patientCode,bloodType,units,urgency,hospital,staffId,createdAt,status", rows);
    }

    private void saveAlerted(Registry registry) throws IOException {
        List<String> rows = new ArrayList<>();
        for (String requestId : registry.getAlertedRequestIds()) {
            for (Donor d : registry.getAlerted(requestId)) {
                rows.add(CsvUtil.join(requestId, d.getId()));
            }
        }
        writeFile("alerted.csv", "requestId,donorId", rows);
    }

    private void saveForecast(Registry registry) throws IOException {
        List<String> rows = new ArrayList<>();
        StockForecast forecast = registry.getForecast();
        for (BloodType type : BloodType.values()) {
            List<Integer> used = forecast.getUsageHistory(type);
            List<Integer> received = forecast.getReceivedHistory(type);
            for (int i = 0; i < used.size(); i++) {
                rows.add(CsvUtil.join(type.getLabel(), used.get(i), received.get(i)));
            }
        }
        writeFile("forecast.csv", "bloodType,used,received", rows);
    }

    private void saveAudit(Registry registry) throws IOException {
        List<String> rows = new ArrayList<>();
        for (String entry : registry.getAuditLog().getEntries()) {
            rows.add(CsvUtil.join(entry));
        }
        writeFile("audit.csv", "entry", rows);
    }

    // =====================================================================
    //  LOAD
    // =====================================================================
    public void load(Registry registry) throws DugtongBuhayException {
        try {
            loadHospitals(registry);
            loadStaff(registry);
            loadAdmins(registry);
            loadDonors(registry);
            loadDonations(registry);
            loadRequests(registry);
            loadAlerted(registry);
            loadForecast(registry);
            loadAudit(registry);
        } catch (IOException e) {
            throw new DataStorageException("Could not read saved data: " + e.getMessage());
        } catch (IllegalArgumentException | DateTimeException | IndexOutOfBoundsException e) {
            throw new DataStorageException("Saved data looks corrupted: " + e.getMessage());
        }
    }

    /** Reads a CSV file as rows of fields. Skips the header and blank lines. */
    private List<List<String>> readRows(String fileName) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        Path file = folder.resolve(fileName);
        if (!Files.exists(file)) {
            return rows;                       // a missing file just means "no records"
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {      // i = 1 skips the header line
            if (!lines.get(i).isBlank()) {
                rows.add(CsvUtil.parseLine(lines.get(i)));
            }
        }
        return rows;
    }

    private void loadHospitals(Registry registry) throws IOException, DugtongBuhayException {
        BloodType[] types = BloodType.values();
        for (List<String> row : readRows("hospitals.csv")) {
            Location location = new Location(row.get(2),
                    Double.parseDouble(row.get(3)), Double.parseDouble(row.get(4)));
            Hospital hospital = new Hospital(row.get(0), row.get(1), location);
            for (int i = 0; i < types.length; i++) {
                int units = Integer.parseInt(row.get(5 + i));
                if (units > 0) {
                    hospital.addUnits(types[i], units);
                }
            }
            registry.addHospital(hospital);
        }
    }

    private void loadStaff(Registry registry) throws IOException, DugtongBuhayException {
        for (List<String> row : readRows("staff.csv")) {
            Hospital hospital = registry.findHospitalByName(row.get(3));
            registry.addStaff(new HospitalStaff(row.get(0), row.get(1), row.get(2),
                    hospital.getName(), hospital.getLocation()));
        }
    }

    private void loadAdmins(Registry registry) throws IOException, DugtongBuhayException {
        for (List<String> row : readRows("admins.csv")) {
            registry.addAdmin(new Admin(row.get(0), row.get(1), row.get(2)));
        }
    }

    private void loadDonors(Registry registry) throws IOException, DugtongBuhayException {
        for (List<String> row : readRows("donors.csv")) {
            Location location = new Location(row.get(4),
                    Double.parseDouble(row.get(5)), Double.parseDouble(row.get(6)));
            Donor donor = new Donor(row.get(0), row.get(1), row.get(2),
                    BloodType.fromLabel(row.get(3)), location);
            if (Boolean.parseBoolean(row.get(7))) {
                donor.giveConsent();
            }
            donor.setHealthyToDonate(Boolean.parseBoolean(row.get(8)));
            donor.restoreReliability(Double.parseDouble(row.get(9)));
            registry.addDonor(donor);
        }
    }

    private void loadDonations(Registry registry) throws IOException, DugtongBuhayException {
        for (List<String> row : readRows("donations.csv")) {
            Donor donor = registry.findDonor(row.get(0));
            donor.recordDonation(createDonation(row.get(1), LocalDate.parse(row.get(2))));
        }
    }

    private Donation createDonation(String typeName, LocalDate date) {
        switch (typeName) {
            case "Whole Blood": return new WholeBlood(date);
            case "Platelets":   return new Platelets(date);
            case "Plasma":      return new Plasma(date);
            default: throw new IllegalArgumentException("Unknown donation type: " + typeName);
        }
    }

    private void loadRequests(Registry registry) throws IOException, DugtongBuhayException {
        for (List<String> row : readRows("requests.csv")) {
            Hospital hospital = registry.findHospitalByName(row.get(5));
            registry.addRequest(BloodRequest.restore(
                    row.get(0), row.get(1), BloodType.fromLabel(row.get(2)),
                    Integer.parseInt(row.get(3)), UrgencyLevel.valueOf(row.get(4)),
                    hospital.getName(), hospital.getLocation(), row.get(6),
                    LocalDateTime.parse(row.get(7)), BloodRequest.Status.valueOf(row.get(8))));
        }
    }

    private void loadAlerted(Registry registry) throws IOException, DugtongBuhayException {
        for (List<String> row : readRows("alerted.csv")) {
            String requestId = row.get(0);
            Donor donor = registry.findDonor(row.get(1));
            List<Donor> waiting = registry.getAlerted(requestId);   // a copy
            waiting.add(donor);
            registry.recordAlerted(requestId, waiting);
        }
    }

    private void loadForecast(Registry registry) throws IOException {
        for (List<String> row : readRows("forecast.csv")) {
            registry.getForecast().recordWeek(BloodType.fromLabel(row.get(0)),
                    Integer.parseInt(row.get(1)), Integer.parseInt(row.get(2)));
        }
    }

    private void loadAudit(Registry registry) throws IOException {
        for (List<String> row : readRows("audit.csv")) {
            registry.getAuditLog().restoreEntry(row.get(0));
        }
    }
}