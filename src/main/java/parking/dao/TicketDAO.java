package parking.dao;

import parking.model.ParkingSlot;
import parking.model.Ticket;
import parking.model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {

    public TicketDAO() {
        try (Connection c = DBConnection.get(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS tickets ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "vehicle_number TEXT, vehicle_type TEXT, slot_id INTEGER,"
                    + "in_time TEXT, out_time TEXT, fee REAL)");
        } catch (SQLException e) {
            throw new RuntimeException("Could not set up database: " + e.getMessage(), e);
        }
    }

    public void save(Ticket t) {
        String sql = "INSERT INTO tickets(vehicle_number, vehicle_type, slot_id, in_time, out_time, fee)"
                + " VALUES (?,?,?,?,?,?)";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getVehicle().getNumber());
            ps.setString(2, t.getVehicle().getType());
            ps.setInt(3, t.getSlot().getSlotId());
            ps.setString(4, t.getInTime().toString());
            ps.setString(5, t.getOutTime().toString());
            ps.setDouble(6, t.getFee());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Could not save ticket: " + e.getMessage());
        }
    }

    public List<Ticket> loadAll() {
        List<Ticket> list = new ArrayList<>();
        try (Connection c = DBConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM tickets")) {
            while (rs.next()) {
                Vehicle v = new Vehicle(rs.getString("vehicle_number"), rs.getString("vehicle_type"));
                ParkingSlot s = new ParkingSlot(rs.getInt("slot_id"), rs.getString("vehicle_type"));
                Ticket t = new Ticket(v, s, LocalDateTime.parse(rs.getString("in_time")));
                t.setOutTime(LocalDateTime.parse(rs.getString("out_time")));
                t.setFee(rs.getDouble("fee"));
                list.add(t);
            }
        } catch (SQLException e) {
            System.err.println("Could not load tickets: " + e.getMessage());
        }
        return list;
    }
}
