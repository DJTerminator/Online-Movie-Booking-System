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
public class CardPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod getType() {
        return PaymentMethod.CARD;
    }

    @Override
    public PaymentResponse process(double amount, PaymentRequest request) {
        log.info("Processing CARD payment for booking: {}, amount: {}", request.getBookingId(), amount);

        // Simulate card validation
        if (request.getCardNumber() != null && request.getCardNumber().length() < 16) {
            return PaymentResponse.builder()
                    .bookingId(request.getBookingId())
                    .amount(amount)
                    .paymentMethod("CARD")
                    .status("FAILED")
                    .message("Invalid card number")
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        // FIX: Generate unique transaction ID — was hardcoded "TXN_123" causing unique constraint violations
        String txnId = "CARD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        log.info("CARD payment successful. TransactionId: {}", txnId);
        return PaymentResponse.builder()
                .bookingId(request.getBookingId())
                .amount(amount)
                .paymentMethod("CARD")
                .status("SUCCESS")
                .transactionId(txnId)
                .message("Card payment successful")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public PaymentResponse refund(String transactionId, double amount) {
        log.info("Processing CARD refund for transaction: {}, amount: {}", transactionId, amount);
        String refundTxnId = "REFUND-CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId(refundTxnId)
                .message("Card refund processed successfully")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
