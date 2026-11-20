package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import exception.DuplicateRecordException;
import exception.RecordNotFoundException;
import model.Admin;
import model.BloodRequest;
import model.Donor;
import model.Hospital;
import model.HospitalStaff;

import java.util.LinkedHashSet;
import java.util.Set;

public class Registry {
    private final List<Donor> donors = new ArrayList<>();
    private final List<Hospital> hospitals = new ArrayList<>();
    private final List<HospitalStaff> staff = new ArrayList<>();
    private final List<Admin> admins = new ArrayList<>();
    private final List<BloodRequest> requests = new ArrayList<>();
    private final Map<String, List<Donor>> alerted = new HashMap<>();  // requestId -> donors waiting to reply
    private final StockForecast forecast = new StockForecast();
    private final AuditLog auditLog = new AuditLog();

    // ---------- adding ----------
    public void addDonor(Donor donor) throws DuplicateRecordException {
        for (Donor d : donors) {
            if (d.getId().equalsIgnoreCase(donor.getId())) {
                throw new DuplicateRecordException("Donor ID " + donor.getId() + " already exists.");
            }
            if (d.getContactNumber().equals(donor.getContactNumber())) {
                throw new DuplicateRecordException("A donor with this mobile number is already registered.");
            }
        }
        donors.add(donor);
    }

    public void addHospital(Hospital hospital) throws DuplicateRecordException {
        for (Hospital h : hospitals) {
            if (h.getId().equalsIgnoreCase(hospital.getId())) {
                throw new DuplicateRecordException("Hospital ID " + hospital.getId() + " already exists.");
            }
        }
        hospitals.add(hospital);
    }

    public void addStaff(HospitalStaff member) throws DuplicateRecordException {
        for (HospitalStaff s : staff) {
            if (s.getId().equalsIgnoreCase(member.getId())) {
                throw new DuplicateRecordException("Staff ID " + member.getId() + " already exists.");
            }
        }
        staff.add(member);
    }

    public void addAdmin(Admin admin) throws DuplicateRecordException {
        for (Admin a : admins) {
            if (a.getId().equalsIgnoreCase(admin.getId())) {
                throw new DuplicateRecordException("Admin ID " + admin.getId() + " already exists.");
            }
        }
        admins.add(admin);
    }

    public void addRequest(BloodRequest request) {
        requests.add(request);
    }

    // ---------- finding ----------
    public Donor findDonor(String id) throws RecordNotFoundException {
        for (Donor d : donors) {
            if (d.getId().equalsIgnoreCase(id)) {
                return d;
            }
        }
        throw new RecordNotFoundException("No donor found with ID " + id + ".");
    }

    public HospitalStaff findStaff(String id) throws RecordNotFoundException {
        for (HospitalStaff s : staff) {
            if (s.getId().equalsIgnoreCase(id)) {
                return s;
            }
        }
        throw new RecordNotFoundException("No hospital staff found with ID " + id + ".");
    }

    public Admin findAdmin(String id) throws RecordNotFoundException {
        for (Admin a : admins) {
            if (a.getId().equalsIgnoreCase(id)) {
                return a;
            }
        }
        throw new RecordNotFoundException("No admin found with ID " + id + ".");
    }

    public Hospital findHospitalByName(String name) throws RecordNotFoundException {
        for (Hospital h : hospitals) {
            if (h.getName().equalsIgnoreCase(name)) {
                return h;
            }
        }
        throw new RecordNotFoundException("No hospital named " + name + ".");
    }

    // ---------- lists (read-only views) ----------
    public List<Donor> getDonors() { return Collections.unmodifiableList(donors); }
    public List<Hospital> getHospitals() { return Collections.unmodifiableList(hospitals); }
    public List<BloodRequest> getRequests() { return Collections.unmodifiableList(requests); }

    public List<HospitalStaff> getStaff() { return Collections.unmodifiableList(staff); }
    public List<Admin> getAdmins() { return Collections.unmodifiableList(admins); }

    public Set<String> getAlertedRequestIds() {
        return new LinkedHashSet<>(alerted.keySet());
    }

    public List<BloodRequest> getOpenRequestsFor(String hospitalName) {
        List<BloodRequest> open = new ArrayList<>();
        for (BloodRequest r : requests) {
            if (r.getStatus() == BloodRequest.Status.OPEN
                    && r.getHospitalName().equalsIgnoreCase(hospitalName)) {
                open.add(r);
            }
        }
        return open;
    }

    public String nextDonorId() {
        return String.format("D%03d", donors.size() + 1);
    }

    // ---------- who has been alerted and has not replied yet ----------
    public void recordAlerted(String requestId, List<Donor> donorsAlerted) {
        alerted.put(requestId, new ArrayList<>(donorsAlerted));
    }

    public List<Donor> getAlerted(String requestId) {
        List<Donor> list = alerted.get(requestId);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    public void removeAlerted(String requestId, Donor donor) {
        List<Donor> list = alerted.get(requestId);
        if (list != null) {
            list.remove(donor);
        }
    }

    public StockForecast getForecast() { return forecast; }
    public AuditLog getAuditLog() { return auditLog; }
}