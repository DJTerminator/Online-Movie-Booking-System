package com.digvijay.bookMyShow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long bookingId;
    private String bookingReference;
    private Long showId;
    private String movieTitle;
    private String theatreName;
    private LocalDateTime showDateTime;
    private List<String> seatNumbers;
    private Double totalAmount;
    private Double discountApplied;
    private Double finalAmount;
    private String status;
    private LocalDateTime bookingDateTime;
    private LocalDateTime lockExpiry;
    private String paymentId;
    private String message;

}
