package com.digvijay.bookMyShow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowDTO {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private Long theatreId;
    private String theatreName;
    private String theatreCity;
    private String theatreAddress;
    private LocalDateTime showDateTime;
    private Double basePrice;
    private String showType;
    private Integer availableSeats;
}
