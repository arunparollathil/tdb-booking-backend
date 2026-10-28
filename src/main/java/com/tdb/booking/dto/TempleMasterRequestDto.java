package com.tdb.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class TempleMasterRequestDto {

    @NotBlank(message = "Temple code is mandatory") @Size(max = 50, message = "Temple code must not exceed 50 characters") private String templeCode;

    @NotBlank(message = "Temple name is mandatory") @Size(max = 150, message = "Temple name must not exceed 150 characters") private String templeName;

    @Size(max = 255, message = "Description must not exceed 255 characters") private String description;

    @Builder.Default private Boolean isActive = true;
}
