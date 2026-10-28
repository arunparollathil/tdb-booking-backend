package com.tdb.booking.controller;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.dto.DarshanBookingResponseDto;
import com.tdb.booking.service.DarshanBookingService;
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

@RestController
@RequestMapping("/api/v1/darshan-bookings")
@RequiredArgsConstructor
public class DarshanBookingController {

    private final DarshanBookingService darshanBookingService;

    @PostMapping
    public ResponseEntity<DarshanBookingResponseDto> bookTickets(
            @Valid @RequestBody DarshanBookingRequestDto requestDto) {
        DarshanBookingResponseDto response = darshanBookingService.createBooking(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<DarshanBookingResponseDto> getBooking(@PathVariable UUID bookingId) {
        DarshanBookingResponseDto response = darshanBookingService.getBookingById(bookingId);
        return ResponseEntity.ok(response);
    }
}
