package com.tdb.booking.controller;

import com.tdb.booking.dto.DarshanSlotRequestDto;
import com.tdb.booking.dto.DarshanSlotResponseDto;
import com.tdb.booking.dto.error.ApiErrorResponse;
import com.tdb.booking.service.DarshanSlotService;
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
 * REST Controller for managing date-specific Darshan slots. Provides endpoints for creating slots
 * and querying slot details and real-time availability.
 */
@RestController
@RequestMapping("/api/v1/darshan-slots")
@RequiredArgsConstructor
@Tag(
        name = "Darshan Slots",
        description =
                "Endpoints for managing date-specific darshan slots and checking ticket"
                        + " availability")
public class DarshanSlotController {

    private final DarshanSlotService darshanSlotService;

    /**
     * Creates a new date-specific darshan slot based on temple and slot master templates.
     *
     * @param requestDto slot creation payload containing templeId, slotMasterId, slotDate, and
     *     capacity
     * @return created {@link DarshanSlotResponseDto} with HTTP 201 Created
     */
    @PostMapping
    @Operation(
            summary = "Create a date-specific darshan slot",
            description =
                    "Creates a new darshan slot for a specific date using an active temple and slot"
                        + " master template. Initializes booked count to 0 and calculates available"
                        + " ticket count.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Darshan slot successfully created",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                DarshanSlotResponseDto.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request fields or mismatched temple association",
                        content =
                                @Content(
                                        schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Referenced Temple or Slot Master not found",
                        content =
                                @Content(
                                        schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "409",
                        description =
                                "Slot already exists for the date, or temple/slot master is"
                                        + " inactive",
                        content =
                                @Content(
                                        schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "Unexpected internal server error",
                        content =
                                @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<DarshanSlotResponseDto> createSlot(
            @Valid @RequestBody DarshanSlotRequestDto requestDto) {
        DarshanSlotResponseDto response = darshanSlotService.createDarshanSlot(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves slot details along with real-time remaining ticket count.
     *
     * @param slotId unique identifier of the darshan slot
     * @return {@link DarshanSlotResponseDto} with HTTP 200 OK
     */
    @GetMapping("/{slotId}")
    @Operation(
            summary = "Retrieve slot details and remaining tickets",
            description =
                    "Fetches a specific darshan slot by its unique identifier, including timing"
                            + " from slot master and remaining availability.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Slot details retrieved successfully",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                DarshanSlotResponseDto.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid UUID format supplied",
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
                        responseCode = "500",
                        description = "Unexpected internal server error",
                        content =
                                @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<DarshanSlotResponseDto> getSlot(
            @Parameter(
                            description = "UUID of the darshan slot",
                            example = "33333333-3333-3333-3333-333333333333")
                    @PathVariable
                    UUID slotId) {
        DarshanSlotResponseDto response = darshanSlotService.getDarshanSlotById(slotId);
        return ResponseEntity.ok(response);
    }
}
