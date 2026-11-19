package parking.service;

import parking.dao.TicketDAO;
import parking.model.ParkingSlot;
import parking.model.Ticket;
import parking.model.Vehicle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

public class ParkingService {
    public static final String CAR = "CAR";
    public static final String BIKE = "BIKE";
    private static final double CAR_RATE_PER_HOUR = 20.0;
    private static final double BIKE_RATE_PER_HOUR = 10.0;

    private final List<ParkingSlot> slots = new ArrayList<>();
    // Queue of free slots per vehicle type: poll() on entry, offer() on exit
    private final Map<String, Queue<ParkingSlot>> freeSlots = new HashMap<>();
    // Active tickets keyed by vehicle number: fast duplicate check and lookup
    private final Map<String, Ticket> activeTickets = new HashMap<>();
    private final List<Ticket> completedTickets = new ArrayList<>();

    private final TicketDAO dao;               // may be null (in-memory only)
    private final Supplier<LocalDateTime> clock;

    public ParkingService(int carSlots, int bikeSlots, TicketDAO dao, Supplier<LocalDateTime> clock) {
        this.dao = dao;
        this.clock = clock;
        freeSlots.put(CAR, new ArrayDeque<>());
        freeSlots.put(BIKE, new ArrayDeque<>());
        int id = 1;
        for (int i = 0; i < carSlots; i++) addSlot(new ParkingSlot(id++, CAR));
        for (int i = 0; i < bikeSlots; i++) addSlot(new ParkingSlot(id++, BIKE));
        if (dao != null) {
            completedTickets.addAll(dao.loadAll());
        }
    }

    public ParkingService(int carSlots, int bikeSlots, TicketDAO dao) {
        this(carSlots, bikeSlots, dao, LocalDateTime::now);
    }

    private void addSlot(ParkingSlot slot) {
        slots.add(slot);
        freeSlots.get(slot.getSupportedType()).offer(slot);
    }

    public synchronized Ticket park(Vehicle v) {
        if (v.getNumber() == null || v.getNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a vehicle number");
        }
        String number = v.getNumber().toUpperCase();
        if (activeTickets.containsKey(number)) {
            throw new DuplicateVehicleException(number + " is already parked");
        }
        Queue<ParkingSlot> queue = freeSlots.get(v.getType());
        ParkingSlot slot = queue == null ? null : queue.poll();
        if (slot == null) {
            throw new ParkingFullException("No free slot for " + v.getType());
        }
        slot.setOccupied(true);
        Ticket ticket = new Ticket(new Vehicle(number, v.getType()), slot, clock.get());
        activeTickets.put(number, ticket);
        return ticket;
    }

    public synchronized Ticket exit(String vehicleNumber) {
        String number = vehicleNumber == null ? "" : vehicleNumber.trim().toUpperCase();
        Ticket ticket = activeTickets.remove(number);
        if (ticket == null) {
            throw new IllegalArgumentException(number + " is not parked here");
        }
        ticket.setOutTime(clock.get());
        ticket.setFee(calculateFee(ticket));
        ParkingSlot slot = ticket.getSlot();
        slot.setOccupied(false);
        freeSlots.get(slot.getSupportedType()).offer(slot);
        completedTickets.add(ticket);
        if (dao != null) {
            dao.save(ticket);
        }
        return ticket;
    }

    // Hours are rounded up, with a minimum of 1 hour
    double calculateFee(Ticket t) {
        long minutes = Duration.between(t.getInTime(), t.getOutTime()).toMinutes();
        long hours = Math.max(1, (long) Math.ceil(minutes / 60.0));
        double rate = ParkingService.CAR.equals(t.getVehicle().getType())
                ? CAR_RATE_PER_HOUR : BIKE_RATE_PER_HOUR;
        return hours * rate;
    }

    public synchronized List<ParkingSlot> getSlots() { return new ArrayList<>(slots); }
    public synchronized List<Ticket> getCompletedTickets() { return new ArrayList<>(completedTickets); }
    public synchronized List<Ticket> getActiveTickets() { return new ArrayList<>(activeTickets.values()); }

    public synchronized int freeCount() {
        return (int) slots.stream().filter(s -> !s.isOccupied()).count();
    }
}
