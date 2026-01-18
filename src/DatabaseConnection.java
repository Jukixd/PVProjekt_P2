import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;
    private DatabaseConnection() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("config.properties"));

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");

            this.connection = DriverManager.getConnection(url, user, pass);

        } catch (IOException | SQLException e) {
            System.err.println("Chyba při připojování k DB: " + e.getMessage());
            throw new RuntimeException("Nelze se připojit k DB, zkontroluj config.", e);
        }
    }

    public static DatabaseConnection getInstance() {
        try {
            if (instance == null || instance.getConnection().isClosed()) {
                instance = new DatabaseConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}