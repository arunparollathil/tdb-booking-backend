package com.tdb.booking.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Encapsulates validation error details for an individual field. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Details of a field-level validation constraint violation")
public class FieldErrorDetail {

    @Schema(description = "Name of the invalid request field", example = "ticketCount")
    private String field;

    @Schema(
            description = "Validation failure reason",
            example = "Ticket count must be greater than zero")
    private String message;
}
