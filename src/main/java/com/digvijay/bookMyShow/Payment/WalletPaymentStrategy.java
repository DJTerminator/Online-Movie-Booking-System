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
public class WalletPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod getType() {
        return PaymentMethod.WALLET;
    }

    @Override
    public PaymentResponse process(double amount, PaymentRequest request) {
        log.info("Processing WALLET payment for booking: {}, amount: {}", request.getBookingId(), amount);
        String txnId = "WALLET-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("WALLET payment successful. TransactionId: {}", txnId);
        return PaymentResponse.builder()
                .bookingId(request.getBookingId())
                .amount(amount)
                .paymentMethod("WALLET")
                .status("SUCCESS")
                .transactionId(txnId)
                .message("Wallet payment successful")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public PaymentResponse refund(String transactionId, double amount) {
        String refundTxnId = "REFUND-WALLET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId(refundTxnId)
                .message("Wallet refund processed. Amount credited immediately.")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
