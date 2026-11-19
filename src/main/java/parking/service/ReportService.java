package parking.service;

import parking.model.ParkingSlot;
import parking.model.Ticket;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {
    private final ParkingService service;

    public ReportService(ParkingService service) { this.service = service; }

    public String buildReport() {
        List<ParkingSlot> slots = service.getSlots();
        List<Ticket> done = service.getCompletedTickets();

        long occupied = slots.stream().filter(ParkingSlot::isOccupied).count();
        long free = slots.size() - occupied;

        Map<String, Long> byType = done.stream()
                .collect(Collectors.groupingBy(t -> t.getVehicle().getType(), Collectors.counting()));
        double total = done.stream().mapToDouble(Ticket::getFee).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("Occupancy: ").append(occupied).append(" of ").append(slots.size()).append(" slots\n");
        sb.append("Free slots: ").append(free).append("\n");
        sb.append("Vehicles completed: ").append(done.size()).append("\n");
        byType.forEach((type, count) -> sb.append("  ").append(type).append(": ").append(count).append("\n"));
        sb.append("Total collection: Rs ").append(String.format("%.2f", total));
        return sb.toString();
    }
}
