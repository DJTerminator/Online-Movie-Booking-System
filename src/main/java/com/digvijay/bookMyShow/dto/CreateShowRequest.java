package com.digvijay.bookMyShow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateShowRequest {

    @NotNull
    private Long movieId;

    @NotNull
    private Long theatreId;

    @NotNull
    private LocalDateTime showDateTime;

    @NotNull @Positive
    private Double basePrice;

    @NotBlank
    private String showType;
}
