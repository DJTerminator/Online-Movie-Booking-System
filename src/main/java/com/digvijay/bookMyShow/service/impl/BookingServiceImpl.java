package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.dto.*;
import com.digvijay.bookMyShow.entity.Booking;
import com.digvijay.bookMyShow.entity.Seat;
import com.digvijay.bookMyShow.entity.Show;
import com.digvijay.bookMyShow.entity.User;
import com.digvijay.bookMyShow.enums.BookingStatus;
import com.digvijay.bookMyShow.enums.SeatStatus;
import com.digvijay.bookMyShow.enums.ShowType;
import com.digvijay.bookMyShow.exceptions.BookingException;
import com.digvijay.bookMyShow.exceptions.ResourceNotFoundException;
import com.digvijay.bookMyShow.repository.BookingRepository;
import com.digvijay.bookMyShow.repository.SeatRepository;
import com.digvijay.bookMyShow.repository.ShowRepository;
import com.digvijay.bookMyShow.repository.UserRepository;
import com.digvijay.bookMyShow.service.BookingService;
import com.digvijay.bookMyShow.service.DiscountService;
import com.digvijay.bookMyShow.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final User user;
    private final Show show;
    private static final int MAX_SEATS_PER_BOOKING = 10;
    private static final int LOCK_DURATION_MINUTES = 10;

    @Override
    @Transactional
    public BookingResponse bookTickets(BookingRequest request, String username) {

        log.info("User: {}, Show ID: {}, Seat IDs: {}",
                username, request.getShowId(), request.getSeatIds());

        // Fetch user
        log.debug("Fetching user details for username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new ResourceNotFoundException("User not found: " + username);
                });
        log.debug("User found - ID: {}, Email: {}", user.getId(), user.getEmail());

        // Fetch show
        log.debug("Fetching show details for Show ID: {}", request.getShowId());
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> {
                    log.error("Show not found - Show ID: {}", request.getShowId());
                    return new ResourceNotFoundException("Show not found: " + request.getShowId());
                });
        log.info("Show found - Movie: {}, Theatre: {}, DateTime: {}, Available Seats: {}",
                show.getMovie().getTitle(), show.getTheatre().getName(),
                show.getShowDateTime(), show.getAvailableSeats());

        // Fetch and validate seats
        log.debug("Fetching {} seats", request.getSeatIds().size());
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());

        if (seats.size() != request.getSeatIds().size()) {
            log.error("Seat count mismatch - Requested: {}, Found: {}",
                    request.getSeatIds().size(), seats.size());
            throw new BookingException("Some seats were not found");
        }
        log.debug("All {} seats found successfully", seats.size());

        // Validate all seats are available
        log.debug("Validating seat availability");
        validateSeatsAvailable(seats);
        log.debug("All seats are available for booking");

        // Calculate total amount
        double totalAmount = seats.stream()
                .mapToDouble(Seat::getPrice)
                .sum();
        log.info("Total amount calculated: ₹{} for {} seats", totalAmount, seats.size());

        // Apply discount strategy
        boolean isAfternoonShow = show.getShowType() == ShowType.AFTERNOON;
        log.debug("Applying discount strategy - Afternoon Show: {}, Seat Count: {}", isAfternoonShow, seats.size());
        double discount = discountService.calculateDiscount(totalAmount, seats.size(), isAfternoonShow);
        log.info("Discount applied: ₹{} ({}%)", discount,
                String.format("%.2f", (discount / totalAmount) * 100));

        // Create booking
        log.debug("Creating booking entity");
        Booking booking = createBooking(user, show, seats, totalAmount, discount);

        // Update seat status
        log.debug("Updating seat status to BOOKED");
        updateSeatStatus(seats, booking);

        // Update show available seats
        int previousAvailableSeats = show.getAvailableSeats();
        show.setAvailableSeats(show.getAvailableSeats() - seats.size());
        showRepository.save(show);
        log.debug("Show available seats updated: {} -> {}",
                previousAvailableSeats, show.getAvailableSeats());

        // Save booking
        booking = bookingRepository.save(booking);

        log.info("=== Booking Completed Successfully ===");
        log.info("Booking Reference: {}, Total Amount: ₹{}, Discount: ₹{}, Final Amount: ₹{}",
                booking.getBookingReference(), booking.getTotalAmount(),
                booking.getDiscountApplied(), booking.getFinalAmount());

        return convertToBookingResponse(booking);
    }

    private void validateSeatsAvailable(List<Seat> seats) {
        List<Seat> unavailableSeats = seats.stream()
                .filter(seat -> seat.getStatus() != SeatStatus.AVAILABLE)
                .collect(Collectors.toList());

        if (!unavailableSeats.isEmpty()) {
            String seatNumbers = unavailableSeats.stream()
                    .map(Seat::getSeatNumber)
                    .collect(Collectors.joining(", "));
            log.error("Seat validation failed - Unavailable seats: {}", seatNumbers);
            throw new BookingException("Seats not available: " + seatNumbers);
        }
    }

    private Booking createBooking(User user, Show show, List<Seat> seats,
                                  double totalAmount, double discount) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setSeats(seats);
        booking.setBookingDateTime(LocalDateTime.now());
        booking.setTotalAmount(totalAmount - discount);
        booking.setDiscountApplied(discount);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference(generateBookingReference());
        return booking;
    }

    private void updateSeatStatus(List<Seat> seats, Booking booking) {
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setBooking(booking);
            seatRepository.save(seat);
        });
    }

    /**
     * Generates a unique booking reference
     */
    private String generateBookingReference() {
        return "BMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** Converts booking entity to response DTO */
    private BookingResponse convertToBookingResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setBookingId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setShowId(booking.getShow().getId());
        response.setMovieTitle(booking.getShow().getMovie().getTitle());
        response.setTheatreName(booking.getShow().getTheatre().getName());
        response.setShowDateTime(booking.getShow().getShowDateTime());
        response.setSeatNumbers(booking.getSeats().stream()
                .map(Seat::getSeatNumber)
                .collect(Collectors.toList()));
        response.setTotalAmount(booking.getTotalAmount());
        response.setDiscountApplied(booking.getDiscountApplied());
        response.setStatus(booking.getStatus().name());
        response.setBookingDateTime(booking.getBookingDateTime());
        return response;
    }


    @Transactional
    public LockResult lockSeats(String showId, List<String> seatIds, String userId) {
        // Validate seat count
        if (seatIds.size() > MAX_SEATS_PER_BOOKING) {
            return LockResult.failure("Maximum " + MAX_SEATS_PER_BOOKING + " seats allowed");
        }

        // Acquire pessimistic locks
        List<Seat> seats = seatRepository
                .findByShowIdAndSeatIdsWithLock(showId, seatIds);

        if (seats.size() != seatIds.size()) {
            return LockResult.failure("Some seats not found");
        }

        // Check availability and lock
        List<String> unavailableSeats = new ArrayList<>();
        for (Seat seat : seats) {
            if (!seat.tryLock(userId)) {
                unavailableSeats.add(String.valueOf(seat.getId()));
            }
        }

        if (!unavailableSeats.isEmpty()) {
            // Rollback - unlock any seats we locked
            for (Seat seat : seats) {
                if (seat.isLockedBy(userId)) {
                    seat.unlock();
                }
            }
            return LockResult.failure("Seats unavailable: " + unavailableSeats);
        }

        seatRepository.saveAll(seats);

        // Create pending booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setSeats(seats);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(calculateTotal(seats));
        booking.setLockExpiry(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));

        bookingRepository.save(booking);

        // Schedule expiry
//        scheduleExpiry(booking.getId(), LOCK_DURATION_MINUTES);

        return LockResult.success(booking);
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId, PaymentRequest paymentRequest) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Book not found:{}"+ bookingId));

        // Validate booking state
        if (booking.getStatus() != BookingStatus.PENDING) {
            return BookingResponse.failure("Booking is not in pending state");
        }

        if (booking.isExpired()) {
            return BookingResponse.failure("Booking has expired. Please start over.");
        }

        // Process payment
        PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);

        if (!"SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
            throw new BookingException("Payment failed: " + paymentResponse.getMessage());
        }

        // Confirm seats
        for (Seat seat : booking.getSeats()) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setBooking(booking);
        }


        // Update booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentResponse.getPaymentId());
        booking.setConfirmedAt(LocalDateTime.now());

        bookingRepository.save(booking);
        seatRepository.saveAll(booking.getSeats());

        // Notify (optional)
        notifyConfirmed(booking);

        // Return DTO (IMPORTANT)
        return convertToBookingResponse(booking);
    }

    @Transactional
    public void handleExpiry(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null || booking.getStatus() != BookingStatus.PENDING) {
            return; // Already processed
        }

        // Release seats
        for (Seat seat : booking.getSeats()) {
            seat.unlock();
        }

        booking.setStatus(BookingStatus.EXPIRED);

        bookingRepository.save(booking);
        seatRepository.saveAll(booking.getSeats());
    }

    private double calculateTotal(List<Seat> seats) {
        double subtotal = seats.stream()
                .mapToDouble(Seat::getPrice)
                .sum();
        double convenienceFee = 1.50;
        return subtotal + convenienceFee;
    }

    private void notifyConfirmed(Booking booking) {
//        for (BookingObserver observer : observers) {
//            try {
//                observer.onBookingConfirmed(booking);
//            } catch (Exception e) {
//                // Log but don't fail the booking
//                log.error("Observer notification failed", e);
//            }
//        }
    }
}
