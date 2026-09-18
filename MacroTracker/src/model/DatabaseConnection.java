package model;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Embedded SQLite connection — no server to install or run. The database
 * file lives under the user's home directory so it works the same way
 * regardless of where the app is launched from, and is created (with its
 * schema) on first connection.
 */
public class DatabaseConnection {

    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".macrotracker";
    private static final String DB_PATH = DB_DIR + File.separator + "macrotracker.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    private static volatile boolean schemaInitialized = false;

    public static Connection getConnection() {
        try {
            new File(DB_DIR).mkdirs();

            Connection conn = DriverManager.getConnection(URL);

            if (!schemaInitialized) {
                initSchema(conn);
                schemaInitialized = true;
            }

            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void initSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS food_entries ("
                    + "entry_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "food_name TEXT NOT NULL, "
                    + "calories INTEGER NOT NULL, "
                    + "protein REAL NOT NULL, "
                    + "carbs REAL NOT NULL, "
                    + "fats REAL NOT NULL, "
                    + "meal_type TEXT NOT NULL, "
                    + "entry_date TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS goals ("
                    + "user_id INTEGER PRIMARY KEY, "
                    + "calories_goal INTEGER NOT NULL, "
                    + "protein_goal REAL NOT NULL, "
                    + "carbs_goal REAL NOT NULL, "
                    + "fat_goal REAL NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS calories_burned ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "calories_burned INTEGER NOT NULL, "
                    + "burned_date TEXT NOT NULL)");
        }
    }
}
