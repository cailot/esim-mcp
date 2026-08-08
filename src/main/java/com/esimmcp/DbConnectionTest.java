package com.esimmcp;

import com.esimmcp.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

/**
 * One-shot JDBC connectivity check against Supabase PostgreSQL.
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.esimmcp.DbConnectionTest
 * </pre>
 */
public final class DbConnectionTest {

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        String url = config.datasourceUrl();
        String user = config.datasourceUsername();
        String password = config.datasourcePassword();

        System.out.println("Testing database connection...");
        System.out.println("URL : " + maskUrl(url));
        System.out.println("User: " + user);

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("sslmode", "require");
        props.setProperty("loginTimeout", "15");
        props.setProperty("connectTimeout", "15");
        props.setProperty("socketTimeout", "30");

        try (Connection conn = DriverManager.getConnection(url, props);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "select current_database() as db, current_user as usr, version() as ver, now() as ts")) {

            if (!rs.next()) {
                System.err.println("FAIL: connected but no row returned");
                System.exit(1);
            }

            System.out.println("OK: connection successful");
            System.out.println("  database : " + rs.getString("db"));
            System.out.println("  user     : " + rs.getString("usr"));
            System.out.println("  server   : " + rs.getString("ver"));
            System.out.println("  now      : " + rs.getTimestamp("ts"));

            try (ResultSet tables = stmt.executeQuery(
                    """
                    select table_schema, table_name
                    from information_schema.tables
                    where table_schema = 'public'
                    order by table_name
                    """)) {
                System.out.println("  public tables:");
                boolean any = false;
                while (tables.next()) {
                    any = true;
                    System.out.println("    - " + tables.getString("table_schema") + "." + tables.getString("table_name"));
                }
                if (!any) {
                    System.out.println("    (none — run supabase/schema.sql if needed)");
                }
            }
        } catch (Exception e) {
            System.err.println("FAIL: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String maskUrl(String url) {
        if (url == null || url.isBlank()) {
            return "(empty)";
        }
        return url.replaceAll("password=[^&]*", "password=***");
    }

    private DbConnectionTest() {
    }
}
