package parking.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // SQLite keeps everything in one file next to the project: no server needed
    private static final String URL = "jdbc:sqlite:parking.db";

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
