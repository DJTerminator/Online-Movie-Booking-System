package com.digvijay.bookMyShow.Payment;

import com.digvijay.bookMyShow.dto.PaymentRequest;
import com.digvijay.bookMyShow.dto.PaymentResponse;
import com.digvijay.bookMyShow.enums.PaymentMethod;

import java.time.LocalDateTime;

public class WalletPayment implements PaymentStrategy {

    @Override
    public PaymentMethod getType() {
        return PaymentMethod.WALLET;
    }

    @Override
    public PaymentResponse process(double amount, PaymentRequest request) {

        return PaymentResponse.builder()
                .paymentId("TXN_WALLET_" + System.currentTimeMillis())
                .bookingId(request.getBookingId())
                .amount(amount)
                .paymentMethod("WALLET")
                .status("SUCCESS")
                .transactionId("TXN_WALLET_" + System.currentTimeMillis())
                .message("Wallet Payment Successful")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public void refund(String transactionId, double amount) {
        // implement later
    }
}
