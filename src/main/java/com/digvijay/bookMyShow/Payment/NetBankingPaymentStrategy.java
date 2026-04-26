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
public class NetBankingPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod getType() {
        return PaymentMethod.NETBANKING;
    }

    @Override
    public PaymentResponse process(double amount, PaymentRequest request) {
        log.info("Processing NETBANKING payment for booking: {}, amount: {}", request.getBookingId(), amount);
        String txnId = "NB-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("NETBANKING payment successful. TransactionId: {}", txnId);
        return PaymentResponse.builder()
                .bookingId(request.getBookingId())
                .amount(amount)
                .paymentMethod("NETBANKING")
                .status("SUCCESS")
                .transactionId(txnId)
                .message("Net banking payment successful")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public PaymentResponse refund(String transactionId, double amount) {
        String refundTxnId = "REFUND-NB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId(refundTxnId)
                .message("Net banking refund processed. Amount will reflect in 3-5 business days.")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
