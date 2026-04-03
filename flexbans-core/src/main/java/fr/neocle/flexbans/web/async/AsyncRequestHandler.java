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

    private static final FlexLogger LOGGER = FlexLogger.get(AsyncRequestHandler.class);

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
                    LOGGER.error("Async {} error: ", operationName, throwable);
                    ResponseUtils.sendJson(asyncResp, ApiResponse.error(500, "Internal server error"), gson);
                } else {
                    ResponseUtils.sendJson(asyncResp, response, gson);
                }
            } catch (IOException e) {
                LOGGER.error("Error sending async {} response: ", operationName, e);
            } finally {
                asyncContext.complete();
            }
        });
    }
}