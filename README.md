# Smart Parking Slot Allocator (JAVA-P027)

Open in Apache NetBeans: File > Open Project > select this folder (it is a Maven project).
Run: F6 (main class parking.Main). The first run needs internet so Maven can download the SQLite driver.

Packages: parking.model, parking.service, parking.dao, parking (ParkingFrame, Main)
Rates: CAR Rs 20/hour, BIKE Rs 10/hour, hours rounded up, minimum 1 hour.
Data is saved to parking.db when a vehicle exits.
