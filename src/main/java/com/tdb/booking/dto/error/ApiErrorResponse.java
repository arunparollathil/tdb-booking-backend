package com.tdb.booking.dto.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Standardized API error response returned for all client errors and exceptions. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standardized error response payload")
public class ApiErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    @Schema(description = "UTC timestamp when the error occurred")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Specific application error code", example = "VALIDATION_ERROR")
    private String errorCode;

    @Schema(description = "Human-readable error description", example = "Request validation failed")
    private String message;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "List of field-specific validation errors, if applicable")
    private List<FieldErrorDetail> details;

    @Schema(
            description = "Target API request URI that resulted in the error",
            example = "/api/v1/darshan-bookings")
    private String path;
}
