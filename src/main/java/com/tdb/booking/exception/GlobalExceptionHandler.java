package com.tdb.booking.exception;

import com.tdb.booking.dto.error.ApiErrorResponse;
import com.tdb.booking.dto.error.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception interceptor and advice for all REST controllers. Translates domain exceptions
 * and validation errors into uniform {@link ApiErrorResponse} payloads.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Jakarta Bean Validation errors originating from {@code @Valid} annotations.
     *
     * @param ex validation exception containing field violations
     * @param request current HTTP servlet request
     * @return 400 Bad Request with {@code VALIDATION_ERROR} and field error list
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn(
                "Validation failed for request [{}]: {}", request.getRequestURI(), ex.getMessage());

        List<FieldErrorDetail> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(
                    FieldErrorDetail.builder()
                            .field(error.getField())
                            .message(error.getDefaultMessage())
                            .build());
        }

        ApiErrorResponse errorResponse =
                ApiErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .errorCode("VALIDATION_ERROR")
                        .message("Request validation failed")
                        .details(details)
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles method parameter type conversion failures (e.g. invalid UUIDs in path variables).
     *
     * @param ex type mismatch exception
     * @param request current HTTP servlet request
     * @return 400 Bad Request with {@code INVALID_REQUEST}
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch on request [{}]: {}", request.getRequestURI(), ex.getMessage());

        String message =
                String.format(
                        "Invalid parameter '%s': value '%s' is not of expected type",
                        ex.getName(), ex.getValue());
        if (ex.getRequiredType() != null && ex.getRequiredType().equals(UUID.class)) {
            message =
                    String.format(
                            "Invalid UUID format for parameter '%s': %s",
                            ex.getName(), ex.getValue());
        }

        ApiErrorResponse errorResponse =
                ApiErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .errorCode("INVALID_REQUEST")
                        .message(message)
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles unreadable or malformed HTTP message requests (e.g. malformed JSON or invalid UUID
     * bodies).
     *
     * @param ex message unreadable exception
     * @param request current HTTP servlet request
     * @return 400 Bad Request with {@code INVALID_REQUEST}
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn(
                "Message not readable on request [{}]: {}",
                request.getRequestURI(),
                ex.getMessage());

        String message = "Malformed JSON request or invalid field format";
        if (ex.getMessage() != null && ex.getMessage().contains("UUID")) {
            message = "Invalid UUID format in request body";
        }

        ApiErrorResponse errorResponse =
                ApiErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .errorCode("INVALID_REQUEST")
                        .message(message)
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles all custom domain business exceptions derived from {@link DarshanBookingException}.
     *
     * @param ex business exception containing specific HTTP status and error code
     * @param request current HTTP servlet request
     * @return appropriate HTTP response with structured error body
     */
    @ExceptionHandler(DarshanBookingException.class)
    public ResponseEntity<ApiErrorResponse> handleDarshanBookingException(
            DarshanBookingException ex, HttpServletRequest request) {
        log.warn(
                "Business exception [{} - {}] on [{}]: {}",
                ex.getStatus(),
                ex.getErrorCode(),
                request.getRequestURI(),
                ex.getMessage());

        ApiErrorResponse errorResponse =
                ApiErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(ex.getStatus().value())
                        .errorCode(ex.getErrorCode())
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }

    /**
     * Handles illegal argument exceptions.
     *
     * @param ex illegal argument exception
     * @param request current HTTP servlet request
     * @return 400 Bad Request with {@code INVALID_REQUEST}
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument on request [{}]: {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse errorResponse =
                ApiErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .errorCode("INVALID_REQUEST")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Fallback handler for unhandled server exceptions. Prevents internal implementation details or
     * stack traces from leaking to clients.
     *
     * @param ex uncaught exception
     * @param request current HTTP servlet request
     * @return 500 Internal Server Error with {@code INTERNAL_SERVER_ERROR}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected server error on request [{}]: ", request.getRequestURI(), ex);

        ApiErrorResponse errorResponse =
                ApiErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .errorCode("INTERNAL_SERVER_ERROR")
                        .message("An unexpected error occurred")
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
