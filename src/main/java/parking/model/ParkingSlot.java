package parking.model;

public class ParkingSlot {
    private final int slotId;
    private final String supportedType;
    private boolean occupied;

    public ParkingSlot(int slotId, String supportedType) {
        this.slotId = slotId;
        this.supportedType = supportedType;
    }

    public int getSlotId() { return slotId; }
    public String getSupportedType() { return supportedType; }
    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }

    @Override
    public String toString() {
        return "Slot " + slotId + " (" + supportedType + ", " + (occupied ? "Occupied" : "Free") + ")";
    }
}
