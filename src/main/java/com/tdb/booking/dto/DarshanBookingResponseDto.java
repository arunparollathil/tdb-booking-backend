package com.tdb.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response DTO providing confirmation details of a booked darshan reservation. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Confirmation details for a darshan ticket booking")
public class DarshanBookingResponseDto {

    @Schema(
            description = "Unique booking reference UUID",
            example = "44444444-4444-4444-4444-444444444444")
    private UUID bookingId;

    @Schema(
            description = "Unique client-generated idempotency key associated with this booking",
            example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID idempotencyKey;

    @Schema(
            description = "Unique identifier of the booked darshan slot",
            example = "33333333-3333-3333-3333-333333333333")
    private UUID darshanSlotId;

    @Schema(
            description = "Unique receipt number formatted as TEMPLE_CODE/FINANCIAL_YEAR/SEQUENCE",
            example = "SBM/2026-27/000123")
    private String receiptNumber;

    @Schema(description = "Name of the devotee", example = "Arun")
    private String devoteeName;

    @Schema(description = "Phone number of the devotee", example = "9876543210")
    private String phoneNumber;

    @Schema(description = "Number of tickets confirmed", example = "3")
    private Integer ticketCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Status of the booking", example = "CONFIRMED")
    private String bookingStatus;

    @Schema(description = "Timestamp when the booking was created")
    private Instant createdAt;

    public UUID getId() {
        return bookingId;
    }

    public void setId(UUID id) {
        this.bookingId = id;
    }
}
