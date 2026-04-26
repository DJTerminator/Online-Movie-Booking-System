package com.digvijay.bookMyShow.service;

import com.digvijay.bookMyShow.dto.PaymentRequest;
import com.digvijay.bookMyShow.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse getPaymentByBookingId(Long bookingId);
    PaymentResponse refundPayment(String paymentId);
}
