package com.digvijay.bookMyShow.service;

import com.digvijay.bookMyShow.dto.*;
import java.util.List;

public interface BookingService {
    // Phase 1: Lock seats (10 min TTL), create PENDING booking
    BookingResponse lockSeats(LockSeatsRequest request, String username);

    // Phase 2: Confirm booking after payment
    BookingResponse confirmBooking(Long bookingId, PaymentRequest paymentRequest, String username);

    // Direct book (lock + pay in one step for simplicity)
    BookingResponse bookTickets(BookingRequest request, String username);

    BookingResponse getBookingById(Long bookingId, String username);

    BookingResponse getBookingByReference(String reference, String username);

    List<BookingResponse> getMyBookings(String username);

    BookingResponse cancelBooking(Long bookingId, String username);
}
