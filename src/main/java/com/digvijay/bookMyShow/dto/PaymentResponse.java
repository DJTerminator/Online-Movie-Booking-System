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
    private String status;
    private String transactionId;
    private String message;
    private LocalDateTime createdAt;
}
