package com.digvijay.bookMyShow.controller;
import org.springframework.security.core.Authentication;
import com.digvijay.bookMyShow.dto.BookingRequest;
import com.digvijay.bookMyShow.dto.BookingResponse;
import com.digvijay.bookMyShow.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    /**
     * WRITE SCENARIO: Book movie tickets by selecting a theatre, timing, and preferred seats
     */
    @PostMapping
    public ResponseEntity<BookingResponse> bookTickets(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info(">>> Incoming Request: POST /api/bookings");
        log.info("User: {}, Show ID: {}, Seats: {}", username, request.getShowId(), request.getSeatIds());

        BookingResponse response = bookingService.bookTickets(request, username);

        log.info("<<< Response: Booking successful - Reference: {}, Final Amount: ₹{}",
                response.getBookingReference(), response.getFinalAmount());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
