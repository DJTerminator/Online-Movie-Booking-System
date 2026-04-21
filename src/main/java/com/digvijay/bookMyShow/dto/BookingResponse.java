package com.digvijay.bookMyShow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private String status;
    private LocalDateTime bookingDateTime;
    private String paymentId;
    private String paymentStatus;
    private String message;

    public Double getFinalAmount() {
        if (totalAmount == null) {
            return 0.0;
        }
        if (discountApplied == null) {
            return totalAmount;
        }
        return totalAmount - discountApplied;
    }

    public static BookingResponse failure(String message) {
        BookingResponse response = new BookingResponse();
        response.message = message;
        response.status = "FAILED";
        return response;
    }

}
