package parking.model;

import java.time.LocalDateTime;

public class Ticket {
    private final Vehicle vehicle;
    private final ParkingSlot slot;
    private final LocalDateTime inTime;
    private LocalDateTime outTime; // null until the vehicle exits
    private double fee;

    public Ticket(Vehicle vehicle, ParkingSlot slot, LocalDateTime inTime) {
        this.vehicle = vehicle;
        this.slot = slot;
        this.inTime = inTime;
    }

    public Vehicle getVehicle() { return vehicle; }
    public ParkingSlot getSlot() { return slot; }
    public LocalDateTime getInTime() { return inTime; }
    public LocalDateTime getOutTime() { return outTime; }
    public void setOutTime(LocalDateTime outTime) { this.outTime = outTime; }
    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }

    @Override
    public String toString() {
        return vehicle + " in slot " + slot.getSlotId() + " since " + inTime;
    }
}
