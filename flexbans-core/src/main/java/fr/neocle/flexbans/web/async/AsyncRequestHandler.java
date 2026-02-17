package fr.neocle.flexbans.web.async;

import com.google.gson.Gson;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.security.SecurityHeadersManager;
import fr.neocle.flexbans.web.util.ResponseUtils;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AsyncRequestHandler {
    private static final long ASYNC_TIMEOUT = 30000; // 30 seconds

    private final SecurityHeadersManager securityManager;
    private final Gson gson;

    public AsyncRequestHandler(SecurityHeadersManager securityManager, Gson gson) {
        this.securityManager = securityManager;
        this.gson = gson;
    }

    public <T> void handleAsync(
            HttpServletRequest req,
            HttpServletResponse resp,
            CompletableFuture<ApiResponse<T>> futureResponse,
            String operationName
    ) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(ASYNC_TIMEOUT);

        futureResponse.whenComplete((response, throwable) -> {
            try {
                HttpServletResponse asyncResp = (HttpServletResponse) asyncContext.getResponse();
                securityManager.setHeaders(asyncResp, (HttpServletRequest) asyncContext.getRequest());

                if (throwable != null) {
                    FlexLogger.error("Async " + operationName + " error: " + throwable.getMessage());
                    ResponseUtils.sendJson(asyncResp, ApiResponse.error(500, "Internal server error"), gson);
                } else {
                    ResponseUtils.sendJson(asyncResp, response, gson);
                }
            } catch (IOException e) {
                FlexLogger.error("Error sending async " + operationName + " response: " + e.getMessage());
            } finally {
                asyncContext.complete();
            }
        });
    }
}