package com.digvijay.bookMyShow.entity;

import com.digvijay.bookMyShow.enums.SeatStatus;
import com.digvijay.bookMyShow.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seats", indexes = {
        @Index(name = "idx_seat_show", columnList = "show_id"),
        @Index(name = "idx_seat_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"show", "booking"})
@ToString(exclude = {"show", "booking"})
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
    private SeatType seatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Column(nullable = false)
    private Double price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "lock_expiry")
    private LocalDateTime lockExpiry;

    // Optimistic locking — prevents concurrent modifications at DB level
    @Version
    private Long version;

    public boolean isLockExpired() {
        return lockExpiry != null && LocalDateTime.now().isAfter(lockExpiry);
    }

    public boolean isLockedBy(String userId) {
        return status == SeatStatus.LOCKED && userId.equals(lockedBy);
    }

    public boolean isAvailableForLocking() {
        return status == SeatStatus.AVAILABLE ||
                (status == SeatStatus.LOCKED && isLockExpired());
    }
}