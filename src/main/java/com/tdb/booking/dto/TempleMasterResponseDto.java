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
public class TempleMasterResponseDto {

    private UUID id;
    private String templeCode;
    private String templeName;
    private String description;
    private Boolean isActive;
    private Instant createdAt;
    private Instant modifiedAt;
}
