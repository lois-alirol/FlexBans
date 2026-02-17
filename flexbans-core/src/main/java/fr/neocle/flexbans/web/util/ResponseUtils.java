package fr.neocle.flexbans.web.util;

import com.google.gson.Gson;
import fr.neocle.flexbans.web.response.ApiResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ResponseUtils {
    private static final String CONTENT_TYPE = "application/json";
    private static final String CHARSET = "UTF-8";

    public static void sendJson(HttpServletResponse resp, ApiResponse<?> response, Gson gson) throws IOException {
        resp.setContentType(CONTENT_TYPE);
        resp.setCharacterEncoding(CHARSET);
        resp.setStatus(response.getCode());
        resp.getWriter().write(gson.toJson(response));
    }

    public static void sendImage(HttpServletResponse resp, byte[] imageData) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("image/png");
        resp.setHeader("Cache-Control", "public, max-age=86400");
        resp.getOutputStream().write(imageData);
        resp.flushBuffer();
    }
}