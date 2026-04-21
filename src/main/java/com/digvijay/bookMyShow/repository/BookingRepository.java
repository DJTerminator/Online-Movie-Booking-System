package com.digvijay.bookMyShow.repository;

import com.digvijay.bookMyShow.entity.Booking;
import com.digvijay.bookMyShow.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByShowId(Long showId);

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);
}
