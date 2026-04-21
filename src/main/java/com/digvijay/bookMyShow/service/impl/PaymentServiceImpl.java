package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.Payment.PaymentStrategy;
import com.digvijay.bookMyShow.dto.PaymentRequest;
import com.digvijay.bookMyShow.dto.PaymentResponse;
import com.digvijay.bookMyShow.entity.Booking;
import com.digvijay.bookMyShow.entity.Payment;
import com.digvijay.bookMyShow.enums.BookingStatus;
import com.digvijay.bookMyShow.enums.PaymentMethod;
import com.digvijay.bookMyShow.enums.PaymentStatus;
import com.digvijay.bookMyShow.exceptions.BookingException;
import com.digvijay.bookMyShow.repository.BookingRepository;
import com.digvijay.bookMyShow.repository.PaymentRepository;
import com.digvijay.bookMyShow.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final List<PaymentStrategy> strategyList;
    private Map<PaymentMethod, PaymentStrategy> strategyMap;

//    @PostConstruct
//    public void init() {
//        strategyMap = strategyList.stream()
//                .collect(Collectors.toMap(
//                        PaymentStrategy::getType,
//                        Function.identity()
//                ));
//    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        // Fetch booking
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingException(
                        "Booking not found: " + request.getBookingId()
                ));

        // Validate booking state
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingException("Booking is not in PENDING state");
        }

        if (booking.isExpired()) {
            throw new BookingException("Booking has expired");
        }

        // Resolve payment method
        PaymentMethod method = PaymentMethod.valueOf(request.getPaymentMethod());

        PaymentStrategy strategy = strategyMap.get(method);

        if (strategy == null) {
            throw new BookingException("Unsupported payment method");
        }

        // Create payment entry (PENDING)
        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .booking(booking)
                .amount(booking.getTotalAmount())
                .method(method)
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        PaymentResponse result = strategy.process(
                booking.getTotalAmount(),
                request
        );

        if (!"SUCCESS".equalsIgnoreCase(result.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            return buildResponse(payment, "Payment failed");
        }

        // Payment success
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN_" + System.currentTimeMillis());
        paymentRepository.save(payment);

        // Confirm booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(payment.getId());
        booking.setConfirmedAt(LocalDateTime.now());

        bookingRepository.save(booking);

        return buildResponse(payment, "Payment successful");
    }

    // Helper: build response
    private PaymentResponse buildResponse(Payment payment, String message) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBooking().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getMethod().name())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .message(message)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
