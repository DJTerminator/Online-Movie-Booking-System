package com.digvijay.bookMyShow.entity;

import com.digvijay.bookMyShow.enums.SeatStatus;
import com.digvijay.bookMyShow.enums.SeatType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

    @Entity
    @Table(name = "seats")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class Seat {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "show_id", nullable = false)
        private Show show;

        @Column(nullable = false)
        private String seatNumber;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private SeatType seatType; // REGULAR, PREMIUM, VIP

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private SeatStatus status; // AVAILABLE, BOOKED, LOCKED

        @Column(nullable = false)
        private Double price;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "booking_id")
        private Booking booking;

        private String lockedBy;        // User ID who locked
        private LocalDateTime lockedAt;
        private LocalDateTime lockExpiry;

        @Version  // For optimistic locking as backup
        private Long version;

        public synchronized boolean tryLock(String userId) {
            if (status != SeatStatus.AVAILABLE) {
                // Check if lock has expired
                if (status == SeatStatus.LOCKED && isLockExpired()) {
                    // Lock expired, can be re-locked
                    unlock();
                } else {
                    return false;
                }
            }

            this.status = SeatStatus.LOCKED;
            this.lockedBy = userId;
            this.lockedAt = LocalDateTime.now();
            this.lockExpiry = LocalDateTime.now().plusMinutes(10);
            return true;
        }

        public synchronized void unlock() {
            if (status == SeatStatus.LOCKED) {
                this.status = SeatStatus.AVAILABLE;
                this.lockedBy = null;
                this.lockedAt = null;
                this.lockExpiry = null;
            }
        }

        public synchronized boolean book(String userId) {
            if (status != SeatStatus.LOCKED || !userId.equals(lockedBy)) {
                return false;
            }
            this.status = SeatStatus.BOOKED;
            return true;
        }

        public boolean isLockExpired() {
            return lockExpiry != null && LocalDateTime.now().isAfter(lockExpiry);
        }

        public boolean isLockedBy(String userId) {
            return status == SeatStatus.LOCKED && userId.equals(lockedBy);
        }
    }
