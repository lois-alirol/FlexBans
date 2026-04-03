package fr.neocle.flexbans.util.loader;

import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.DriverShim;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;

public class LibsLoader {

    private static final String SQLITE_JDBC_URL = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar";
    private static final String MYSQL_JDBC_URL = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.2.0/mysql-connector-j-9.2.0.jar";

    private static final File LIBS_FOLDER = new File("plugins/flexbans/libs");
    private static final File SQLITE_JAR = new File(LIBS_FOLDER, "sqlite.jar");
    private static final File MYSQL_JAR = new File(LIBS_FOLDER, "mysql.jar");

    private static final FlexLogger LOGGER = FlexLogger.get(LibsLoader.class);

    private URLClassLoader sqliteClassLoader;
    private URLClassLoader mysqlClassLoader;

    public LibsLoader() {}

    public void loadDriver(String databaseType) {
        switch (databaseType.toLowerCase()) {
            case "sqlite":
                ensureSQLiteAvailable();
                break;
            case "mysql":
                ensureMySQLAvailable();
                break;
            default:
                LOGGER.info("Database type " + databaseType + " does not require a driver to be loaded dynamically.");
        }
    }

    private void ensureSQLiteAvailable() {
        try {
            if (!LIBS_FOLDER.exists()) LIBS_FOLDER.mkdirs();
            if (!SQLITE_JAR.exists()) {
                LOGGER.info("Downloading SQLite JDBC...");
                downloadFile(SQLITE_JDBC_URL, SQLITE_JAR);
                LOGGER.info("SQLite JDBC downloaded successfully!");
            }

            if (sqliteClassLoader == null) {
                sqliteClassLoader = new URLClassLoader(new URL[]{SQLITE_JAR.toURI().toURL()}, getClass().getClassLoader());
                Driver driver = (Driver) Class.forName("org.sqlite.JDBC", true, sqliteClassLoader)
                        .getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(driver));
            }
        } catch (Exception e) {
            LOGGER.error("Something went wrong when loading SQLITE: ", e);
        }
    }

    private void ensureMySQLAvailable() {
        try {
            if (!LIBS_FOLDER.exists()) LIBS_FOLDER.mkdirs();
            if (!MYSQL_JAR.exists()) {
                LOGGER.info("Downloading MySQL JDBC...");
                downloadFile(MYSQL_JDBC_URL, MYSQL_JAR);
                LOGGER.info("MySQL JDBC downloaded successfully!");
            }

            if (mysqlClassLoader == null) {
                mysqlClassLoader = new URLClassLoader(new URL[]{MYSQL_JAR.toURI().toURL()}, getClass().getClassLoader());
                Driver driver = (Driver) Class.forName("com.mysql.cj.jdbc.Driver", true, mysqlClassLoader)
                        .getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(driver));
            }
        } catch (Exception e) {
            LOGGER.error("Something went wrong when loading MYSQL: ", e);
        }
    }

    private void downloadFile(String url, File file) throws IOException {
        try (InputStream in = new URL(url).openStream();
             FileOutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}