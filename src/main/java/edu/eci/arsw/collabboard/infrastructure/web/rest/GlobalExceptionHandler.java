package edu.eci.arsw.collabboard.infrastructure.web.rest;

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import edu.eci.arsw.collabboard.application.exception.BoardNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Central translation of internal exceptions into a uniform {@link ApiError}
 * HTTP contract. No stack trace or internal Java message reaches the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BoardNotFoundException.class)
    public ResponseEntity<ApiError> boardNotFound(BoardNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "BOARD_NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // All field errors, sorted: the validator reports them in no fixed order.
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                message.isEmpty() ? "Invalid request" : message, request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidDomainInput(IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_INPUT", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformedBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        // Jackson builds BoardElement through its constructor, so a broken element invariant
        // arrives wrapped here. Report it as the domain rule it is, not as unreadable JSON.
        if (ex.getCause() instanceof ValueInstantiationException wrapped
                && wrapped.getCause() instanceof IllegalArgumentException invariant) {
            return invalidDomainInput(invariant, request);
        }
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        // Spring MVC's own failures (unknown route, unsupported method or media type) already
        // carry the right 4xx status: keep it instead of degrading them to a 500.
        if (ex instanceof ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return error(status, status.name(), status.getReasonPhrase(), request.getRequestURI());
        }
        // Last resort: never leak a stack trace or internal message to the client.
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", request.getRequestURI());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, String path) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), code, message, path
        ));
    }
}