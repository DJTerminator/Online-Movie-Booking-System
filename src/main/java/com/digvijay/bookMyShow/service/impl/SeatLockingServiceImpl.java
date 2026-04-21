package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.dto.LockResult;
import com.digvijay.bookMyShow.entity.Seat;
import com.digvijay.bookMyShow.enums.SeatStatus;
import com.digvijay.bookMyShow.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SeatLockingServiceImpl {

    private final SeatRepository seatRepository;

    // Pessimistic Locking with SELECT FOR UPDATE
    @Transactional
    public LockResult lockSeats(String showId, List<String> seatIds, String userId) {
        // Acquire row-level locks on the seats
        List<Seat> seats = seatRepository
                .findByShowIdAndSeatIdsWithLock(showId, seatIds);  // FOR UPDATE

        // Check if all seats are available
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                // Some seats not available - rollback will release locks
                return LockResult.failure("Seat " + seat.getId() + " is not available");
            }
        }

        // All seats available - lock them
        LocalDateTime lockExpiry = LocalDateTime.now().plusMinutes(10);
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.LOCKED);
            seat.setLockedBy(userId);
            seat.setLockedAt(LocalDateTime.now());
            seat.setLockExpiry(lockExpiry);
        }

        seatRepository.saveAll(seats);

        // Schedule unlock after timeout
//        scheduler.schedule(
//                () -> releaseExpiredLocks(showId, seatIds, userId),
//                10, TimeUnit.MINUTES
//        );

        return LockResult.success(seats, lockExpiry);
    }
}
