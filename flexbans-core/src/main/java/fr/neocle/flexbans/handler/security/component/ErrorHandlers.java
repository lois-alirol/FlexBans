package fr.neocle.flexbans.handler.security.component;

import fr.neocle.flexbans.handler.error.ForbiddenError;
import fr.neocle.flexbans.handler.error.NotFoundError;

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