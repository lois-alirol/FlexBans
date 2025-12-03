package fr.neocle.flexbans.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class IpUtils {
    public static String getPublicIP() {
        try {
            URL url = new URL("https://api.ipify.org");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            Scanner scanner = new Scanner(conn.getInputStream());
            String ip = scanner.hasNext() ? scanner.nextLine() : null;
            scanner.close();
            return ip;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
