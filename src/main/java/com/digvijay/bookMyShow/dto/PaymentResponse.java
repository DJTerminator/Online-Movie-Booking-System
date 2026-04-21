package com.digvijay.bookMyShow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {

    private String paymentId;
    private Long bookingId;

    private Double amount;
    private String paymentMethod;
    private String status; // SUCCESS, FAILED, PENDING

    private String transactionId;

    private String message; // success/failure message

    private LocalDateTime createdAt;

    public PaymentResponse(boolean b, String s, String success) {
    }
}
