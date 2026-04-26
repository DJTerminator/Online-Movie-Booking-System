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
import com.digvijay.bookMyShow.exceptions.ResourceNotFoundException;
import com.digvijay.bookMyShow.repository.BookingRepository;
import com.digvijay.bookMyShow.repository.PaymentRepository;
import com.digvijay.bookMyShow.service.PaymentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    // Spring injects all @Component implementations of PaymentStrategy automatically
    private final List<PaymentStrategy> strategyList;

    private Map<PaymentMethod, PaymentStrategy> strategyMap;

    @PostConstruct
    public void init() {
        strategyMap = strategyList.stream()
                .collect(Collectors.toMap(
                        PaymentStrategy::getType,
                        Function.identity(),
                        (a, b) -> a,
                        () -> new EnumMap<>(PaymentMethod.class)
                ));
        log.info("Payment strategies initialized: {}", strategyMap.keySet());
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
//        log.info(">>> processPayment — bookingId: {}, method: {}", request.getBookingId(), request.getPaymentMethod());

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId()));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingException("Booking is not in PENDING state. Status: " + booking.getStatus());
        }

        if (booking.isExpired()) {
            throw new BookingException("Booking has expired. Please start a new booking.");
        }

        // Validate amount matches booking
        if (Math.abs(request.getAmount() - booking.getFinalAmount()) > 0.01) {
            throw new BookingException(String.format(
                    "Amount mismatch. Expected: ₹%.2f, Provided: ₹%.2f",
                    booking.getFinalAmount(), request.getAmount()));
        }

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BookingException("Unsupported payment method: " + request.getPaymentMethod());
        }

        PaymentStrategy strategy = strategyMap.get(method);
        if (strategy == null) {
            throw new BookingException("No payment handler found for method: " + method);
        }

        // Create PENDING payment record
        String paymentId = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .id(paymentId)
                .booking(booking)
                .amount(booking.getFinalAmount())
                .method(method)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        // Execute strategy
        PaymentResponse strategyResult = strategy.process(booking.getFinalAmount(), request);

        // FIX: Aligned with PaymentStatus.SUCCESS (was COMPLETED — mismatch caused confirm to always fail)
        if ("SUCCESS".equalsIgnoreCase(strategyResult.getStatus())) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(strategyResult.getTransactionId());
            paymentRepository.save(payment);

            // Update booking
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentId(paymentId);
            booking.setConfirmedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            log.info("<<< Payment SUCCESS — paymentId: {}, txn: {}", paymentId, strategyResult.getTransactionId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("<<< Payment FAILED — bookingId: {}", request.getBookingId());
        }

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .bookingId(booking.getId())
                .amount(payment.getAmount())
                .paymentMethod(method.name())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .message(strategyResult.getMessage())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + bookingId));
        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(String paymentId) {
        log.info(">>> refundPayment — paymentId: {}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BookingException("Only successful payments can be refunded");
        }

        PaymentStrategy strategy = strategyMap.get(payment.getMethod());
        PaymentResponse refundResult = strategy.refund(payment.getTransactionId(), payment.getAmount());

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        log.info("<<< Refund processed for paymentId: {}", paymentId);
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBooking().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getMethod().name())
                .status("REFUNDED")
                .transactionId(refundResult.getTransactionId())
                .message(refundResult.getMessage())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getId())
                .bookingId(p.getBooking().getId())
                .amount(p.getAmount())
                .paymentMethod(p.getMethod().name())
                .status(p.getStatus().name())
                .transactionId(p.getTransactionId())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
