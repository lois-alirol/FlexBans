package fr.neocle.flexbans.util;

import fr.neocle.flexbans.logger.FlexLogger;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.logging.Logger;

public class LibsLoader {

    private static final String SQLITE_JDBC_URL = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar";
    private static final String MYSQL_JDBC_URL = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.2.0/mysql-connector-j-9.2.0.jar";
    private static final File LIBS_FOLDER = new File("plugins/FlexBans/libs");
    private static final File SQLITE_JAR = new File(LIBS_FOLDER, "sqlite.jar");
    private static final File MYSQL_JAR = new File(LIBS_FOLDER, "mysql.jar");

    public LibsLoader() {}

    public void ensureSQLiteAvailable() {
        try {
            if (!LIBS_FOLDER.exists()) {
                LIBS_FOLDER.mkdirs();
            }

            if (!SQLITE_JAR.exists()) {
                FlexLogger.info("Downloading SQLite JDBC...");
                downloadFile(SQLITE_JDBC_URL, SQLITE_JAR);
                FlexLogger.info("SQLite JDBC downloaded successfully!");
            }

            URL jarUrl = SQLITE_JAR.toURI().toURL();
            URLClassLoader jarClassLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());

            Driver driver = (Driver) Class.forName("org.sqlite.JDBC", true, jarClassLoader).getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ensureMySQLAvailable() {
        try {
            if (!LIBS_FOLDER.exists()) {
                LIBS_FOLDER.mkdirs();
            }

            if (!MYSQL_JAR.exists()) {
                FlexLogger.info("Downloading MySQL JDBC...");
                downloadFile(MYSQL_JDBC_URL, MYSQL_JAR);
                FlexLogger.info("MySQL JDBC downloaded successfully!");
            }

            URL jarUrl = MYSQL_JAR.toURI().toURL();
            URLClassLoader jarClassLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());

            Driver driver = (Driver) Class.forName("com.mysql.cj.jdbc.Driver", true, jarClassLoader).getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (Exception e) {
            e.printStackTrace();
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

    private void loadJar(File jarFile) throws Exception {
        if (!jarFile.exists()) {
            throw new IOException("Jar file does not exist: " + jarFile.getAbsolutePath());
        }

        URL jarUrl = jarFile.toURI().toURL();
        URLClassLoader jarClassLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());

        Thread.currentThread().setContextClassLoader(jarClassLoader);
    }
}
