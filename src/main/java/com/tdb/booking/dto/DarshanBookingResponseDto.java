package com.tdb.booking.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DarshanBookingResponseDto {

    private UUID id;
    private UUID darshanSlotId;
    private String devoteeName;
    private String phoneNumber;
    private Integer ticketCount;
    private Instant createdAt;
}
