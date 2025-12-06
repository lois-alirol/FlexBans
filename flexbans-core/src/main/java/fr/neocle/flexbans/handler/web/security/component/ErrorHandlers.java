package fr.neocle.flexbans.handler.web.security.component;

import fr.neocle.flexbans.handler.web.error.ForbiddenError;
import fr.neocle.flexbans.handler.web.error.NotFoundError;

public class ErrorHandlers {
    private final ForbiddenError forbiddenError;
    private final NotFoundError notFoundError;

    public ErrorHandlers(ForbiddenError forbiddenError, NotFoundError notFoundError) {
        this.forbiddenError = forbiddenError;
        this.notFoundError = notFoundError;
    }

    public ForbiddenError getForbiddenError() {
        return forbiddenError;
    }

    public NotFoundError getNotFoundError() {
        return notFoundError;
    }
}