package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.dto.*;
import com.digvijay.bookMyShow.entity.*;
import com.digvijay.bookMyShow.enums.BookingStatus;
import com.digvijay.bookMyShow.enums.SeatStatus;
import com.digvijay.bookMyShow.enums.ShowType;
import com.digvijay.bookMyShow.exceptions.*;
import com.digvijay.bookMyShow.repository.*;
import com.digvijay.bookMyShow.service.BookingService;
import com.digvijay.bookMyShow.service.DiscountService;
import com.digvijay.bookMyShow.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final DiscountService discountService;
    private final PaymentService paymentService;

    private static final int LOCK_DURATION_MINUTES = 10;

    /**
     * PHASE 1: Lock seats and create a PENDING booking (10-min TTL).
     * Uses PESSIMISTIC_WRITE (SELECT FOR UPDATE) to prevent race conditions.
     */
    @Override
    @Transactional
    public BookingResponse lockSeats(LockSeatsRequest request, String username) {
        log.info(">>> lockSeats — user: {}, showId: {}, seats: {}", username, request.getShowId(), request.getSeatIds());

        User user = findUserOrThrow(username);
        Show show = findShowOrThrow(request.getShowId());

        if (show.getAvailableSeats() < request.getSeatIds().size()) {
            throw new BookingException("Not enough available seats. Available: " + show.getAvailableSeats());
        }

        // CONCURRENCY: Pessimistic write lock
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(
                request.getShowId(), request.getSeatIds());

        if (seats.size() != request.getSeatIds().size()) {
            throw new BookingException("Some seat IDs are invalid for this show");
        }

        // Validate all requested seats are available (or have expired locks)
        List<String> unavailable = seats.stream()
                .filter(s -> !s.isAvailableForLocking())
                .map(s -> s.getSeatNumber() + " (" + s.getStatus() + ")")
                .collect(Collectors.toList());

        if (!unavailable.isEmpty()) {
            throw new SeatAlreadyBookedException("Seats not available: " + unavailable);
        }

        // Lock all seats
        LocalDateTime lockExpiry = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        String userId = String.valueOf(user.getId());
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.LOCKED);
            seat.setLockedBy(userId);
            seat.setLockedAt(LocalDateTime.now());
            seat.setLockExpiry(lockExpiry);
        }
        seatRepository.saveAll(seats);

        // Calculate amounts — FIX: store GROSS totalAmount, discount separate
        double grossTotal = seats.stream().mapToDouble(Seat::getPrice).sum();
        boolean isAfternoon = show.getShowType() == ShowType.AFTERNOON;
        double discount = discountService.calculateDiscount(grossTotal, seats.size(), isAfternoon);

        // Create PENDING booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setSeats(seats);
        booking.setBookingDateTime(LocalDateTime.now());
        booking.setTotalAmount(grossTotal);         // FIX: Store GROSS amount
        booking.setDiscountApplied(discount);       // FIX: Store discount separately
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingReference(generateReference());
        booking.setLockExpiry(lockExpiry);
        Booking saved = bookingRepository.save(booking);

        // Update each seat's booking reference
        seats.forEach(s -> s.setBooking(saved));
        seatRepository.saveAll(seats);

        log.info("<<< Seats locked. BookingRef: {}, expires: {}", saved.getBookingReference(), lockExpiry);
        return toResponse(saved, "Seats locked for 10 minutes. Complete payment to confirm.");
    }

    /**
     * PHASE 2: Confirm booking — processes payment, marks seats BOOKED.
     */
    @Override
    @Transactional
    public BookingResponse confirmBooking(Long bookingId, PaymentRequest paymentRequest, String username) {
        log.info(">>> confirmBooking — user: {}, bookingId: {}", username, bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // Security: ensure booking belongs to the requesting user
        if (!booking.getUser().getUsername().equals(username)) {
            throw new BookingException("Access denied: booking does not belong to you");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingException("Booking is not in PENDING state. Current status: " + booking.getStatus());
        }

        if (booking.isExpired()) {
            expireBooking(booking);
            throw new BookingException("Booking has expired. Please start a new booking.");
        }

        // Process payment
        paymentRequest.setBookingId(bookingId);
        PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);

        if (!"SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
            throw new BookingException("Payment failed: " + paymentResponse.getMessage());
        }

        // Confirm seats
        List<Seat> seats = seatRepository.findByBookingId(bookingId);
        seats.forEach(seat -> {
            if (!seat.getLockedBy().equals(String.valueOf(booking.getUser().getId()))) {
                throw new SeatAlreadyBookedException("Seat " + seat.getSeatNumber() + " lock ownership mismatch");
            }
            seat.setStatus(SeatStatus.BOOKED);
            seat.setLockedBy(null);
            seat.setLockedAt(null);
            seat.setLockExpiry(null);
        });
        seatRepository.saveAll(seats);

        // Update show seat count
        Show show = booking.getShow();
        show.setAvailableSeats(show.getAvailableSeats() - seats.size());
        showRepository.save(show);

        // Confirm booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentResponse.getPaymentId());
        booking.setConfirmedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        log.info("<<< Booking confirmed. Ref: {}, PaymentId: {}", booking.getBookingReference(), paymentResponse.getPaymentId());
        return toResponse(booking, "Booking confirmed successfully!");
    }

    /**
     * Direct single-step booking (lock + pay atomically — for simple clients).
     */
    @Override
    @Transactional
    public BookingResponse bookTickets(BookingRequest request, String username) {
        log.info(">>> bookTickets (direct) — user: {}, showId: {}, seats: {}", username, request.getShowId(), request.getSeatIds());

        User user = findUserOrThrow(username);
        Show show = findShowOrThrow(request.getShowId());

        if (show.getAvailableSeats() < request.getSeatIds().size()) {
            throw new BookingException("Not enough seats available. Available: " + show.getAvailableSeats());
        }

        // CONCURRENCY: Pessimistic write lock
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(
                request.getShowId(), request.getSeatIds());

        if (seats.size() != request.getSeatIds().size()) {
            throw new BookingException("Some seat IDs are invalid");
        }

        // Validate availability
        List<String> unavailable = seats.stream()
                .filter(s -> !s.isAvailableForLocking())
                .map(Seat::getSeatNumber)
                .collect(Collectors.toList());
        if (!unavailable.isEmpty()) {
            throw new SeatAlreadyBookedException("Seats already booked or locked: " + unavailable);
        }

        // Calculate amounts
        // FIX: Store GROSS in totalAmount; getFinalAmount() deducts discount
        double grossTotal = seats.stream().mapToDouble(Seat::getPrice).sum();
        boolean isAfternoon = show.getShowType() == ShowType.AFTERNOON;
        double discount = discountService.calculateDiscount(grossTotal, seats.size(), isAfternoon);

        // Create CONFIRMED booking directly
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setBookingDateTime(LocalDateTime.now());
        booking.setTotalAmount(grossTotal);      // FIX: Gross amount
        booking.setDiscountApplied(discount);    // FIX: Discount stored separately
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference(generateReference());
        booking.setConfirmedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // Mark seats as BOOKED
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setBooking(saved);
        });
        seatRepository.saveAll(seats);
        saved.setSeats(seats);

        // Update available seat count
        show.setAvailableSeats(show.getAvailableSeats() - seats.size());
        showRepository.save(show);

        log.info("<<< Direct booking confirmed. Ref: {}, Gross: ₹{}, Discount: ₹{}, Net: ₹{}",
                saved.getBookingReference(), grossTotal, discount, saved.getFinalAmount());

        return toResponse(saved, "Booking successful!");
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, String username) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getUsername().equals(username)) {
            throw new BookingException("Access denied");
        }
        return toResponse(booking, null);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String reference, String username) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + reference));
        if (!booking.getUser().getUsername().equals(username)) {
            throw new BookingException("Access denied");
        }
        return toResponse(booking, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String username) {
        User user = findUserOrThrow(username);
        return bookingRepository.findByUserIdWithDetails(user.getId()).stream()
                .map(b -> toResponse(b, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String username) {
        log.info(">>> cancelBooking — user: {}, bookingId: {}", username, bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new BookingException("Access denied");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new BookingException("Expired bookings cannot be cancelled");
        }

        // Release seats
        List<Seat> seats = seatRepository.findByBookingId(bookingId);
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setBooking(null);
            seat.setLockedBy(null);
            seat.setLockedAt(null);
            seat.setLockExpiry(null);
        });
        seatRepository.saveAll(seats);

        // Restore available seats
        Show show = booking.getShow();
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            show.setAvailableSeats(show.getAvailableSeats() + seats.size());
            showRepository.save(show);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        log.info("<<< Booking cancelled. Ref: {}", booking.getBookingReference());
        return toResponse(booking, "Booking cancelled successfully. Refund will be processed if applicable.");
    }

    // Called by expiry scheduler
    @Transactional
    public void expireBooking(Booking booking) {
        List<Seat> seats = seatRepository.findByBookingId(booking.getId());
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setBooking(null);
            seat.setLockedBy(null);
            seat.setLockedAt(null);
            seat.setLockExpiry(null);
        });
        seatRepository.saveAll(seats);
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);
        log.info("Booking expired: {}", booking.getBookingReference());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Show findShowOrThrow(Long showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
    }

    private String generateReference() {
        return "BMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BookingResponse toResponse(Booking b, String message) {
        List<Seat> seats = b.getSeats() != null ? b.getSeats() : List.of();
        return BookingResponse.builder()
                .bookingId(b.getId())
                .bookingReference(b.getBookingReference())
                .showId(b.getShow().getId())
                .movieTitle(b.getShow().getMovie().getTitle())
                .theatreName(b.getShow().getTheatre().getName())
                .showDateTime(b.getShow().getShowDateTime())
                .seatNumbers(seats.stream().map(Seat::getSeatNumber).collect(Collectors.toList()))
                .totalAmount(b.getTotalAmount())
                .discountApplied(b.getDiscountApplied())
                .finalAmount(b.getFinalAmount())   // Computed: total - discount
                .status(b.getStatus().name())
                .bookingDateTime(b.getBookingDateTime())
                .lockExpiry(b.getLockExpiry())
                .paymentId(b.getPaymentId())
                .message(message)
                .build();
    }
}
