package com.tdb.booking.controller;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.dto.DarshanBookingResponseDto;
import com.tdb.booking.dto.error.ApiErrorResponse;
import com.tdb.booking.service.DarshanBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for managing Darshan ticket bookings. Handles ticket reservation with concurrency
 * control and booking lookup.
 */
@RestController
@RequestMapping("/api/v1/darshan-bookings")
@RequiredArgsConstructor
@Tag(
        name = "Darshan Bookings",
        description = "Endpoints for booking tickets and retrieving reservation details")
public class DarshanBookingController {

    private final DarshanBookingService darshanBookingService;

    /**
     * Books one or more tickets for a specific darshan slot. Uses pessimistic write locking to
     * ensure capacity limits are preserved under concurrent requests.
     *
     * @param requestDto booking request containing slot ID, devotee details, and ticket count
     * @return confirmed {@link DarshanBookingResponseDto} with HTTP 201 Created
     */
    @PostMapping
    @Operation(
            summary = "Book one or more tickets",
            description =
                    "Reserves tickets for a devotee on a chosen darshan slot. Executes with a"
                            + " pessimistic database write lock to prevent overbooking across"
                            + " concurrent transactions.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Booking confirmed successfully",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                DarshanBookingResponseDto.class))),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Validation failure (blank name, invalid phone, non-positive"
                                        + " tickets, invalid UUID)",
                        content =
                                @Content(
                                        schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Darshan slot not found",
                        content =
                                @Content(
                                        schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "409",
                        description =
                                "Requested tickets exceed availability (INSUFFICIENT_CAPACITY) or"
                                        + " slot is inactive",
                        content =
                                @Content(
                                        schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "Unexpected internal server error",
                        content =
                                @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<DarshanBookingResponseDto> bookTickets(
            @Valid @RequestBody DarshanBookingRequestDto requestDto) {
        DarshanBookingResponseDto response = darshanBookingService.createBooking(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves existing booking confirmation details by booking UUID.
     *
     * @param bookingId unique identifier of the booking
     * @return {@link DarshanBookingResponseDto} with HTTP 200 OK
     */
    @GetMapping("/{bookingId}")
    @Operation(
            summary = "Retrieve booking details",
            description =
                    "Fetches the confirmed booking details including devotee information and ticket"
                            + " count using the booking reference UUID.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Booking details retrieved successfully",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                DarshanBookingResponseDto.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid UUID format supplied",
                        content =
                                @Content(
                                        schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Booking reference not found",
                        content =
                                @Content(
                                        schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "Unexpected internal server error",
                        content =
                                @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<DarshanBookingResponseDto> getBooking(
            @Parameter(
                            description = "UUID of the booking reference",
                            example = "44444444-4444-4444-4444-444444444444")
                    @PathVariable
                    UUID bookingId) {
        DarshanBookingResponseDto response = darshanBookingService.getBookingById(bookingId);
        return ResponseEntity.ok(response);
    }
}
