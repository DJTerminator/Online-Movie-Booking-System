package com.digvijay.bookMyShow.repository;

import com.digvijay.bookMyShow.entity.Booking;
import com.digvijay.bookMyShow.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b JOIN FETCH b.show s JOIN FETCH s.movie JOIN FETCH s.theatre WHERE b.user.id = :userId ORDER BY b.bookingDateTime DESC")
    List<Booking> findByUserIdWithDetails(@Param("userId") Long userId);

    List<Booking> findByShowId(Long showId);

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    // For expiry cleanup scheduler
    List<Booking> findByStatusAndLockExpiryBefore(BookingStatus status, LocalDateTime now);
}
