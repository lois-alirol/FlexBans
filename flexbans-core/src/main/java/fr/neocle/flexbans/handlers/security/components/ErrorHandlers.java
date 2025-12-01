package fr.neocle.flexbans.handlers.security.components;

import fr.neocle.flexbans.handlers.errors.ForbiddenError;
import fr.neocle.flexbans.handlers.errors.NotFoundError;

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