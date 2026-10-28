package com.tdb.booking.controller;

import com.tdb.booking.dto.DarshanSlotRequestDto;
import com.tdb.booking.dto.DarshanSlotResponseDto;
import com.tdb.booking.service.DarshanSlotService;
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
@RequestMapping("/api/v1/darshan-slots")
@RequiredArgsConstructor
public class DarshanSlotController {

    private final DarshanSlotService darshanSlotService;

    @PostMapping
    public ResponseEntity<DarshanSlotResponseDto> createSlot(
            @Valid @RequestBody DarshanSlotRequestDto requestDto) {
        DarshanSlotResponseDto response = darshanSlotService.createDarshanSlot(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slotId}")
    public ResponseEntity<DarshanSlotResponseDto> getSlot(@PathVariable UUID slotId) {
        DarshanSlotResponseDto response = darshanSlotService.getDarshanSlotById(slotId);
        return ResponseEntity.ok(response);
    }
}
