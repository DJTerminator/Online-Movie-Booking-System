package com.digvijay.bookMyShow.Payment;

import com.digvijay.bookMyShow.dto.PaymentRequest;
import com.digvijay.bookMyShow.dto.PaymentResponse;
import com.digvijay.bookMyShow.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class UpiPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod getType() {
        return PaymentMethod.UPI;
    }

    @Override
    public PaymentResponse process(double amount, PaymentRequest request) {
        log.info("Processing UPI payment for booking: {}, amount: {}", request.getBookingId(), amount);

        // Simulate UPI ID validation
        if (request.getUpiId() == null || !request.getUpiId().contains("@")) {
            return PaymentResponse.builder()
                    .bookingId(request.getBookingId())
                    .amount(amount)
                    .paymentMethod("UPI")
                    .status("FAILED")
                    .message("Invalid UPI ID format. Expected format: user@bank")
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        // FIX: Generate single UUID and reuse for both paymentId and transactionId
        String txnId = "UPI-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        log.info("UPI payment successful. TransactionId: {}", txnId);
        return PaymentResponse.builder()
                .bookingId(request.getBookingId())
                .amount(amount)
                .paymentMethod("UPI")
                .status("SUCCESS")
                .transactionId(txnId)
                .message("UPI payment successful")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public PaymentResponse refund(String transactionId, double amount) {
        log.info("Processing UPI refund for transaction: {}", transactionId);
        String refundTxnId = "REFUND-UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId(refundTxnId)
                .message("UPI refund processed. Amount will reflect in 1-2 business days.")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
