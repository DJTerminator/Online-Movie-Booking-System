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

    // Pessimistic write lock - blocks other transactions
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM Seat ss WHERE ss.show.id = :showId " +
            "AND ss.seat.id IN :seatIds")
    List<Seat> findByShowIdAndSeatIdsWithLock(
            @Param("showId") String showId,
            @Param("seatIds") List<String> seatIds
    );


    // Regular query for display (no lock)
    @Query("SELECT ss FROM Seat ss WHERE ss.show.id = :showId")
    List<Seat> findByShowId(@Param("showId") String showId);

    // Find expired locks for cleanup
    @Query("SELECT ss FROM Seat ss WHERE ss.status = 'LOCKED' " +
            "AND ss.lockExpiry < :now")
    List<Seat> findExpiredLocks(@Param("now") LocalDateTime now);
}