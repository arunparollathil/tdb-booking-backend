package com.tdb.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    private UUID bookingId;
    private UUID darshanSlotId;
    private String devoteeName;
    private String phoneNumber;
    private Integer ticketCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String bookingStatus;

    private Instant createdAt;

    public UUID getId() {
        return bookingId;
    }

    public void setId(UUID id) {
        this.bookingId = id;
    }
}
