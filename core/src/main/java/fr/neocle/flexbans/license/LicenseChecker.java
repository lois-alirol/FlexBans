package fr.neocle.flexbans.license;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class LicenseChecker {

    private static final String API_URL = "https://license-checker.license-verif.workers.dev/";

    public static boolean isLicenseValid(String key, String ip) {
        try {
            if (ip == null) {
                return false;
            }

            String fullUrl = API_URL + "?key=" + key + "&ip=" + ip;
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            InputStream inputStream = (responseCode >= 400)
                    ? conn.getErrorStream()
                    : conn.getInputStream();

            if (inputStream == null) {
                System.err.println("No response stream from license server.");
                return false;
            }

            Scanner scanner = new Scanner(inputStream);
            StringBuilder jsonResponse = new StringBuilder();
            while (scanner.hasNext()) {
                jsonResponse.append(scanner.nextLine());
            }
            scanner.close();

            return jsonResponse.toString().contains("\"valid\":true");

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getDiscordId(String licenseKey) {
        try {
            String fullUrl = API_URL + "get-discord-id?key=" + licenseKey;
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            InputStream inputStream = (responseCode >= 400)
                    ? conn.getErrorStream()
                    : conn.getInputStream();

            if (inputStream == null) {
                System.err.println("No response stream from license server.");
                return null;
            }

            Scanner scanner = new Scanner(inputStream);
            StringBuilder jsonResponse = new StringBuilder();
            while (scanner.hasNext()) {
                jsonResponse.append(scanner.nextLine());
            }
            scanner.close();

            String response = jsonResponse.toString();
            int index = response.indexOf("\"discord_id\"");
            if (index == -1) {
                System.err.println("discord_id not found in response: " + response);
                return null;
            }

            int start = response.indexOf(":", index) + 2;
            int end = response.indexOf("\"", start);
            if (start == -1 || end == -1) {
                System.err.println("Malformed discord_id in response: " + response);
                return null;
            }
            return response.substring(start, end);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}