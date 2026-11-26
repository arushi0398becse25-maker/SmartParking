# Smart Parking Slot Allocator (JAVA-P027)

Open in Apache NetBeans: File > Open Project > select this folder (it is a Maven project).
Run: F6 (main class parking.Main). The first run needs internet so Maven can download the SQLite driver.

Packages: parking.model, parking.service, parking.dao, parking (ParkingFrame, Main)
Rates: CAR Rs 20/hour, BIKE Rs 10/hour, hours rounded up, minimum 1 hour.
Data is saved to parking.db when a vehicle exits.

## User interface (Swing)
Main window (ParkingFrame): entry/exit form, live slot map (green = free, red = occupied; click an occupied
slot to fill its number for exit), tabs for Parked vehicles (duration and fee so far), History and Report,
summary cards, live clock and an exit receipt. Press Enter in the number box to park.
