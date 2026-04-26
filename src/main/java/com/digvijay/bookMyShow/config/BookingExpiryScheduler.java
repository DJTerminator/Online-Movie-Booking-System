package com.digvijay.bookMyShow.config;

import com.digvijay.bookMyShow.entity.Booking;
import com.digvijay.bookMyShow.enums.BookingStatus;
import com.digvijay.bookMyShow.repository.BookingRepository;
import com.digvijay.bookMyShow.service.impl.BookingServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingServiceImpl bookingService;

    // Runs every 2 minutes — finds and expires stale PENDING bookings
    @Scheduled(fixedDelay = 120_000)
    public void expireStaleBookings() {
        List<Booking> expired = bookingRepository
                .findByStatusAndLockExpiryBefore(BookingStatus.PENDING, LocalDateTime.now());

        if (!expired.isEmpty()) {
            log.info("Expiry job: Found {} stale PENDING bookings to expire", expired.size());
            expired.forEach(booking -> {
                try {
                    bookingService.expireBooking(booking);
                } catch (Exception e) {
                    log.error("Failed to expire booking {}: {}", booking.getBookingReference(), e.getMessage());
                }
            });
        }
    }
}
