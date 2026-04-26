package com.digvijay.bookMyShow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockSeatsRequest {

    @NotNull
    private Long showId;

    @NotEmpty
    @Size(max = 10, message = "Maximum 10 seats")
    private List<Long> seatIds;
}
