# DUGTONG-BUHAY Testing Evidence

Group 14 | OOP 101 | Tested by: ____________________ | Date: ____________________

## Part A. Automated tests (`src/app/SystemTests.java`)

**How to run:** open `SystemTests.java` in VS Code and click **Run** above `main`.
**Expected last line:** `RESULT: 75 passed, 0 failed`

Take a screenshot of the full output and save it as `screenshots/08-automated-tests.png`.

| Group | IDs | What is checked |
|---|---|---|
| Blood types | BT-01 to BT-05 | Compatibility rules, case-insensitive parsing, invalid type rejected |
| Validation | VA-01 to VA-09 | Location bounds, phone format, empty name, `InputReader` parsing rules |
| Eligibility | EL-01 to EL-10 | Waiting periods per donation type, unhealthy donor, future date rejected, read-only history, consent off by default |
| Requests | RQ-01 to RQ-07 | Units limits, open and fulfilled status, double fulfill rejected, urgency radius, who may post requests |
| Matching | MT-01 to MT-12 | Screening filters, three ranking strategies, alerts, privacy of alert text, reliability changes, closed request rejected |
| Stock and forecast | ST-01 to ST-08, FN-01 to FN-03, FC-01 to FC-07 | Stock status, compatible units, insufficient stock, nearest hospital search, moving average and holiday factor |
| CSV | CSV-01 to CSV-03 | Comma and quote handling, round trip, empty fields |
| Persistence and registry | PS-01 to PS-11 | Save then load keeps donors, stock, reliability, consent, donations, and requests. Duplicate and unknown records rejected |

## Part B. Manual test cases (console menu)

Before testing, delete the `data/` folder so the sample data is fresh. Fill in **Actual result** and **Pass/Fail** while you run each case, and attach a screenshot for each.

| ID | Role | Steps (what you type) | Expected result | Actual result | Pass/Fail | Screenshot |
|---|---|---|---|---|---|---|
| M-01 | Any | Main menu: type `abc`, then `9` | `Please enter a whole number.` then `Please enter a number from 0 to 4.`, menu asks again | | | |
| M-02 | Public | `1`, blood type `b-`, units `4`, place `1` | Lists Lipa Medical Center (about 23 km) then Manila General Hospital (about 94 km) | | | |
| M-03 | Public | `1`, blood type `XX` | `Valid blood types: O-, O+, A-, A+, B-, B+, AB-, AB+.` and asks again | | | |
| M-04 | Staff | `2`, ID `S999` | `No hospital staff found with ID S999.` | | | |
| M-05 | Staff | `2`, `s001`, `1` | Stock board of Batangas Medical Center with statuses (A- shows OUT) | | | |
| M-06 | Staff | `2` (add stock) with `O-`, `10` | O- now 12 (OK) | | | |
| M-07 | Staff | `3` (use stock) with `A-`, `1` | `Not enough A- in stock (have 0, need 1).` | | | |
| M-08 | Staff | `4`: patient `PT-0050`, `A+`, `2` units, urgency `2`, strategy `3` | `Posted: REQ-001 ...`, ranked donors, alerts sent | | | |
| M-09 | Staff | `5`, request `1`, donor `1`, `y`, then `8` | Inbox shows the donor's contact number only after acceptance | | | |
| M-10 | Staff | `6`, request `1` | `REQ-001 is now FULFILLED.` | | | |
| M-11 | Staff | `7` | Forecast table with AT RISK or ok per blood type | | | |
| M-12 | Donor | `3`, `1`: name `Dela Cruz, Juan Jr.`, phone `0912`, then a valid phone | Phone error and asks again, then registration succeeds | | | |
| M-13 | Donor | Register with phone `09171234567` | `A donor with this mobile number is already registered.` | | | |
| M-14 | Donor | `3`, `2`, `D001`, then `1` | Profile with blood type, consent, reliability, eligibility | | | |
| M-15 | Donor | `4` record a donation: Whole Blood, `0` days ago, then `1` | Can donate today is false, next eligible date is 3 months later | | | |
| M-16 | Donor | `3` withdraw consent, then staff posts an A+ request | That donor is not in the ranked list | | | |
| M-17 | Admin | `4`, `a001`, `3` | Audit log lists logins, requests, alerts, and replies | | | |
| M-18 | Admin | `4`, `a001`, `2`, donor `D002` | `Consent withdrawn for Maria Santos.` | | | |
| M-19 | Persistence | Register a donor, exit, run again, log in as that donor | Donor is still there and the first line says `Loaded saved data` | | | |
| M-20 | Persistence | Change a blood type in `data/donors.csv` to `XX`, run | `Saved data looks corrupted: Invalid blood type: XX` and the program stops | | | |

## Part C. Screenshot checklist

- [ ] `01-main-menu.png`
- [ ] `02-public-search.png`
- [ ] `03-stock-board.png`
- [ ] `04-ranked-donors.png`
- [ ] `05-alert-and-reply.png`
- [ ] `06-forecast.png`
- [ ] `07-audit-log.png`
- [ ] `08-automated-tests.png`

## Part D. Summary

| Measure | Result |
|---|---|
| Automated checks passed | ___ / 75 |
| Manual cases passed | ___ / 20 |
| Defects found and fixed | |
| Known limitations | See README, section 12 |
