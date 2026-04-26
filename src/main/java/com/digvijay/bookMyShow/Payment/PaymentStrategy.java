package com.digvijay.bookMyShow.Payment;

import com.digvijay.bookMyShow.dto.PaymentRequest;
import com.digvijay.bookMyShow.dto.PaymentResponse;
import com.digvijay.bookMyShow.enums.PaymentMethod;

public interface PaymentStrategy {

    PaymentMethod getType();

    PaymentResponse process(double amount, PaymentRequest request);

    PaymentResponse refund(String transactionId, double amount);
}
