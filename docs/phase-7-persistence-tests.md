# Phase 7 persistence test results

All five checks passed using Java 21. Each restart launched a fresh JVM. The application ran in an isolated working directory, and the evidence images show excerpts from the captured console and CSV output.

| # | Test | Steps | Expected | Actual | Result |
|---|---|---|---|---|---|
| 1 | First run | Launch with no test data | First-run message and nine CSV files | All nine files were created. Sample donor values matched, and Lea Garcia's donation was dated ten days before the run. | [Pass](../screenshots/phase7-test-1.png) |
| 2 | Donor persistence | Register D009, give consent, restart, and open the profile | Quoted comma in CSV and full name after restart | The CSV quoted Dela Cruz, Juan Jr. The restarted profile showed the full name and consent as YES. | [Pass](../screenshots/phase7-test-2.png) |
| 3 | Request and alert persistence | Post REQ-001, restart, accept Maria's reply, and post again | Open request and pending alerts restored. New request uses REQ-002. | REQ-001 reopened with D002 and D009 waiting. Maria's acceptance appeared in the inbox. The next request was REQ-002. | [Pass](../screenshots/phase7-test-3.png) |
| 4 | Corruption detection | Try blood type XX and latitude abc, then restore the CSV | Startup reports corruption and leaves the CSV intact. Restored data loads. | Both variants stopped before the menu. The donor CSV hash stayed the same during each failed launch. Startup succeeded after restoration. | [Pass](../screenshots/phase7-test-4.png) |
| 5 | CSV round trip | Run CsvTest with a comma and embedded quotes | CSV quotes both fields and parsing recovers four values | Both output lines matched the expected round trip. | [Pass](../screenshots/phase7-test-5.png) |
