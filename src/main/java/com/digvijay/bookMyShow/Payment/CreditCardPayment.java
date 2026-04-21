package com.digvijay.bookMyShow.Payment;

import com.digvijay.bookMyShow.dto.PaymentRequest;
import com.digvijay.bookMyShow.dto.PaymentResponse;
import com.digvijay.bookMyShow.enums.PaymentMethod;

import java.time.LocalDateTime;

public class CreditCardPayment implements PaymentStrategy {

    @Override
    public PaymentMethod getType() {
        return PaymentMethod.CARD;
    }

    @Override
    public PaymentResponse process(double amount, PaymentRequest request) {
        // simulate
        return PaymentResponse.builder()
                .paymentId("TXN_123")
                .bookingId(request.getBookingId())
                .amount(amount)
                .paymentMethod("CARD")
                .status("SUCCESS")
                .transactionId("TXN_123")
                .message("Success")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public void refund(String transactionId, double amount) {}
}
