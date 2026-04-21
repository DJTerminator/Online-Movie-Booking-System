package com.digvijay.bookMyShow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Payment method is required")
    private String paymentMethod;
    // Example: UPI, CARD, NETBANKING, WALLET

    @NotNull(message = "Amount is required")
    private Double amount;

    // Optional fields (can be extended later)
    private String cardNumber;
    private String upiId;
}
