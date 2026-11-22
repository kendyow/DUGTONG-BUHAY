# DUGTONG-BUHAY UML Class Diagrams

Three diagrams: the domain model, the services, and the exceptions.

## How to turn these into the PNG the guidelines ask for (`docs/UML-Diagram.png`)

1. Go to **https://mermaid.live** in your browser.
2. Delete the sample code on the left, then paste **one diagram at a time**. Copy only the lines that start with `classDiagram` and stop before the closing three backticks. Do not copy the lines with the backticks or the word `mermaid`.
3. Click **Actions > PNG** (or *Download PNG*) and save it as `UML-Model.png`, `UML-Services.png`, and `UML-Exceptions.png` in the `docs/` folder.
4. Optional: in VS Code, install the extension **Markdown Preview Mermaid Support**, then press `Ctrl+Shift+V` on this file to preview the diagrams.

Legend: `<|--` inheritance, `<|..` implements an interface, `o--` has a collection of, `-->` uses or has, `..>` depends on.

---

## 1. Domain model

```mermaid
classDiagram
    direction TB

    class User {
        <<abstract>>
        -String id
        -String name
        -String contactNumber
        +getRole() String
        +canPostRequests() boolean
    }
    class Donor {
        -BloodType bloodType
        -Location location
        -boolean consentToBeContacted
        -boolean healthyToDonate
        -double reliabilityScore
        +canDonate(LocalDate) boolean
        +getNextEligibleDate(LocalDate) LocalDate
        +recordDonation(Donation)
        +giveConsent()
        +withdrawConsent()
        +updateReliability(boolean)
    }
    class HospitalStaff {
        -String hospitalName
        -Location hospitalLocation
        +postRequest(String, BloodType, int, UrgencyLevel) BloodRequest
    }
    class Admin {
        +forceWithdrawConsent(Donor)
    }
    class Notifiable {
        <<interface>>
        +receiveAlert(String)
        +getInbox() List
    }

    class Donation {
        <<abstract>>
        -LocalDate donationDate
        +computeNextEligibleDate() LocalDate
        +getTypeName() String
    }
    class WholeBlood {
        +computeNextEligibleDate() LocalDate
    }
    class Platelets {
        +computeNextEligibleDate() LocalDate
    }
    class Plasma {
        +computeNextEligibleDate() LocalDate
    }

    class BloodType {
        <<enumeration>>
        O_NEG
        O_POS
        A_NEG
        A_POS
        B_NEG
        B_POS
        AB_NEG
        AB_POS
        +canDonateTo(BloodType) boolean
    }
    class UrgencyLevel {
        <<enumeration>>
        CRITICAL
        URGENT
        ROUTINE
        +getSearchRadiusKm() double
        +getResponseWindowMinutes() int
    }
    class Location {
        -String name
        -double latitude
        -double longitude
        +distanceKmTo(Location) double
    }
    class BloodRequest {
        -String requestId
        -String patientCode
        -BloodType bloodTypeNeeded
        -int unitsNeeded
        -UrgencyLevel urgency
        -Status status
        +isCompatibleWith(Donor) boolean
        +isWithinSearchRadius(Location) boolean
        +fulfill()
        +cancel()
    }
    class Hospital {
        -String id
        -String name
        -Location location
        -Map stock
        +addUnits(BloodType, int)
        +useUnits(BloodType, int)
        +getStatus(BloodType) StockStatus
        +getCompatibleUnits(BloodType) int
        +canSupply(BloodType, int) boolean
    }

    User <|-- Donor
    User <|-- HospitalStaff
    User <|-- Admin
    Notifiable <|.. Donor
    Notifiable <|.. HospitalStaff

    Donation <|-- WholeBlood
    Donation <|-- Platelets
    Donation <|-- Plasma

    Donor "1" o-- "0..*" Donation : history
    Donor --> BloodType
    Donor --> Location
    Hospital --> Location
    Hospital --> BloodType : stock per type
    BloodRequest --> BloodType
    BloodRequest --> UrgencyLevel
    BloodRequest --> Location
    HospitalStaff ..> BloodRequest : posts
    Admin ..> Donor : withdraws consent
```

---

## 2. Services (matching, stock, storage)

```mermaid
classDiagram
    direction TB

    class MatchStrategy {
        <<interface>>
        +rank(List, BloodRequest) List
        +getName() String
    }
    class NearestFirst {
        +rank(List, BloodRequest) List
    }
    class MostReliableFirst {
        +rank(List, BloodRequest) List
    }
    class BalancedMatch {
        -double RELIABILITY_WEIGHT
        -double DISTANCE_WEIGHT
        +score(Donor, BloodRequest) double
        +rank(List, BloodRequest) List
    }
    class MatchingService {
        -MatchStrategy strategy
        +setStrategy(MatchStrategy)
        +findEligibleDonors(List, BloodRequest, LocalDate) List
        +findMatches(List, BloodRequest, LocalDate) List
        +sendAlerts(List, BloodRequest) List
        +recordResponse(Donor, HospitalStaff, BloodRequest, boolean)
    }
    class HospitalFinder {
        +findNearestWithStock(List, BloodType, int, Location) List
    }
    class StockForecast {
        +recordWeek(BloodType, int, int)
        +projectedStock(Hospital, BloodType, Month) double
        +isShortageRisk(Hospital, BloodType, Month) boolean
    }
    class Registry {
        -List donors
        -List hospitals
        -List staff
        -List admins
        -List requests
        +addDonor(Donor)
        +findDonor(String) Donor
        +findStaff(String) HospitalStaff
    }
    class AuditLog {
        +record(String, String)
        +getEntries() List
    }
    class DataStore {
        +save(Registry)
        +load(Registry)
        +hasSavedData() boolean
    }
    class CsvUtil {
        +escape(String) String
        +join(Object...) String
        +parseLine(String) List
    }
    class ConsoleMenu {
        +run()
    }
    class InputReader {
        +readInt(String, int, int) int
        +readBloodType(String) BloodType
        +readPhone(String) String
    }

    MatchStrategy <|.. NearestFirst
    MatchStrategy <|.. MostReliableFirst
    MatchStrategy <|.. BalancedMatch
    MatchingService --> MatchStrategy : uses
    Registry --> StockForecast
    Registry --> AuditLog
    DataStore ..> Registry : saves and loads
    DataStore ..> CsvUtil
    ConsoleMenu --> Registry
    ConsoleMenu --> MatchingService
    ConsoleMenu --> HospitalFinder
    ConsoleMenu --> DataStore
    ConsoleMenu --> InputReader
```

---

## 3. Exceptions

```mermaid
classDiagram
    direction TB

    class Exception
    class DugtongBuhayException
    class InvalidInputException
    class DuplicateRecordException
    class RecordNotFoundException
    class NoMatchFoundException
    class DataStorageException

    Exception <|-- DugtongBuhayException
    DugtongBuhayException <|-- InvalidInputException
    DugtongBuhayException <|-- DuplicateRecordException
    DugtongBuhayException <|-- RecordNotFoundException
    DugtongBuhayException <|-- NoMatchFoundException
    DugtongBuhayException <|-- DataStorageException
```
