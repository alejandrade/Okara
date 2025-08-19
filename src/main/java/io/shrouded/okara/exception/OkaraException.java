package io.shrouded.okara.exception;

import org.springframework.http.HttpStatus;

public class OkaraException extends RuntimeException {
    private final HttpStatus status;
    private final String type;

    public OkaraException(HttpStatus status, String type, String message) {
        super(message);
        this.status = status;
        this.type = type;
    }

    public OkaraException(HttpStatus status, String type, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.type = type;
    }

    // Convenience factory methods
    public static OkaraException badRequest(String message) {
        return new OkaraException(HttpStatus.BAD_REQUEST, "validation-error", message);
    }

    public static OkaraException unauthorized(String message) {
        return new OkaraException(HttpStatus.UNAUTHORIZED, "authentication-required", message);
    }

    public static OkaraException forbidden(String message) {
        return new OkaraException(HttpStatus.FORBIDDEN, "access-denied", message);
    }

    public static OkaraException notFound(String resource) {
        return new OkaraException(HttpStatus.NOT_FOUND, "resource-not-found",
                                  String.format("The requested %s was not found", resource));
    }

    public static OkaraException internalError(String message) {
        return new OkaraException(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }
}