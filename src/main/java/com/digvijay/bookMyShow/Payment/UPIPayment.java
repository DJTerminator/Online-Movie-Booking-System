package com.digvijay.bookMyShow.Payment;

import com.digvijay.bookMyShow.dto.PaymentRequest;
import com.digvijay.bookMyShow.dto.PaymentResponse;
import com.digvijay.bookMyShow.enums.PaymentMethod;

import java.time.LocalDateTime;

public class UPIPayment implements PaymentStrategy {

    @Override
    public PaymentMethod getType() {
        return PaymentMethod.UPI;
    }

    @Override
    public PaymentResponse process(double amount, PaymentRequest request) {

        return PaymentResponse.builder()
                .paymentId("TXN_UPI_" + System.currentTimeMillis())
                .bookingId(request.getBookingId())
                .amount(amount)
                .paymentMethod("UPI")
                .status("SUCCESS")
                .transactionId("TXN_UPI_" + System.currentTimeMillis())
                .message("UPI Payment Successful")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public void refund(String transactionId, double amount) {
        // implement later
    }
}
