package com.digvijay.bookMyShow.controller;

import com.digvijay.bookMyShow.dto.BookingRequest;
import com.digvijay.bookMyShow.dto.BookingResponse;
import com.digvijay.bookMyShow.dto.LockSeatsRequest;
import com.digvijay.bookMyShow.dto.PaymentRequest;
import com.digvijay.bookMyShow.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/lock")
    public ResponseEntity<BookingResponse> lockSeats(
            @Valid @RequestBody LockSeatsRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("POST /api/bookings/lock — user: {}, showId: {}, seats: {}",
                username, request.getShowId(), request.getSeatIds());
        BookingResponse response = bookingService.lockSeats(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/bookings/{id}/confirm
     * Phase 2: Pay and confirm a PENDING booking
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest paymentRequest,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("POST /api/bookings/{}/confirm — user: {}", id, username);
        BookingResponse response = bookingService.confirmBooking(id, paymentRequest, username);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/bookings
     * Direct single-step booking (lock + confirm atomically)
     * Used for simple clients that don't need the two-phase flow
     */
    @PostMapping
    public ResponseEntity<BookingResponse> bookTickets(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("POST /api/bookings — user: {}, showId: {}, seats: {}",
                username, request.getShowId(), request.getSeatIds());
        BookingResponse response = bookingService.bookTickets(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/bookings/my
     * Current user's full booking history
     */
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        String username = authentication.getName();
        log.info("GET /api/bookings/my — user: {}", username);
        return ResponseEntity.ok(bookingService.getMyBookings(username));
    }

    /**
     * GET /api/bookings/{id}
     * Get booking by ID (owner only)
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("GET /api/bookings/{} — user: {}", id, authentication.getName());
        return ResponseEntity.ok(bookingService.getBookingById(id, authentication.getName()));
    }

    /**
     * GET /api/bookings/reference/{ref}
     * Ticket lookup by booking reference (e.g. BMS-A1B2C3D4)
     */
    @GetMapping("/reference/{ref}")
    public ResponseEntity<BookingResponse> getByReference(
            @PathVariable String ref,
            Authentication authentication) {
        log.info("GET /api/bookings/reference/{} — user: {}", ref, authentication.getName());
        return ResponseEntity.ok(bookingService.getBookingByReference(ref, authentication.getName()));
    }

    /**
     * DELETE /api/bookings/{id}
     * Cancel a booking (owner only)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("DELETE /api/bookings/{} — user: {}", id, username);
        return ResponseEntity.ok(bookingService.cancelBooking(id, username));
    }
}
