package nova;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlTableCreate {

  public static void main(String[] args) {
    // SQLite connection string
    String url = NovaConstant.dbUrl;

    // SQL statements
    String createTableSQL =
        "CREATE TABLE crypto (\n"
            + " id integer PRIMARY KEY,\n"
            + " product_id text NOT NULL,\n"
            + " price REAL NOT NULL,\n"
            + " time text NOT NULL,\n"
            + " size real NOT NULL,\n"
            + " source text NOT NULL\n"
            + ");";

    String insertSQL =
        "INSERT INTO crypto(product_id, price, time, size, source) VALUES('TEST-ID', 0.0,"
            + " 'TEST-TIME', 1.0, 'TEST SOURCE');";
    String selectSQL = "SELECT * FROM crypto;";

    // Establish connection to SQLite database
    try (Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement()) {

      // Create table
      stmt.execute(createTableSQL);

      // Insert data
      stmt.execute(insertSQL);

      // Query data
      ResultSet rs = stmt.executeQuery(selectSQL);

      // Process the result set
      while (rs.next()) {
        System.out.println("ProductID: " + rs.getInt("product_id"));
        System.out.println("Price: " + rs.getString("price"));
        System.out.println("Time: " + rs.getString("time"));
        System.out.println("size: " + rs.getString("size"));
        System.out.println("source: " + rs.getString("source"));
        System.out.println("-------------------");
      }

    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
}
