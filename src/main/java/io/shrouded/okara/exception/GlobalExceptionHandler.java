package io.shrouded.okara.exception;

import io.shrouded.okara.dto.common.ProblemDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OkaraException.class)
    public ResponseEntity<ProblemDetail> handleOkaraException(OkaraException ex, ServerWebExchange exchange) {
        log.warn("OkaraException: {} - {}", ex.getStatus(), ex.getMessage());

        ProblemDetail problem = new ProblemDetail(
                "https://okara.io/problems/" + ex.getType(),
                getTitle(ex.getStatus()),
                ex.getStatus().value(),
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ProblemDetail problem = new ProblemDetail(
                "https://okara.io/problems/internal-error",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                exchange.getRequest().getPath().value()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private String getTitle(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Bad Request";
            case UNAUTHORIZED -> "Unauthorized";
            case FORBIDDEN -> "Forbidden";
            case NOT_FOUND -> "Not Found";
            case INTERNAL_SERVER_ERROR -> "Internal Server Error";
            default -> status.getReasonPhrase();
        };
    }
}