package com.digvijay.bookMyShow.repository;

import com.digvijay.bookMyShow.entity.Seat;
import com.digvijay.bookMyShow.enums.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowId(Long showId);

    List<Seat> findByShowIdAndStatus(Long showId, SeatStatus status);

    List<Seat> findByBookingId(Long bookingId);

    // Uses PESSIMISTIC_WRITE for row-level DB lock (SELECT FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.show.id = :showId AND s.id IN :seatIds")
    List<Seat> findByShowIdAndSeatIdsWithLock(
            @Param("showId") Long showId,
            @Param("seatIds") List<Long> seatIds
    );

    // Find expired locks for scheduled cleanup job
    @Query("SELECT s FROM Seat s WHERE s.status = 'LOCKED' AND s.lockExpiry < :now")
    List<Seat> findExpiredLocks(@Param("now") LocalDateTime now);

    // Count available seats for a show
    long countByShowIdAndStatus(Long showId, SeatStatus status);
}
