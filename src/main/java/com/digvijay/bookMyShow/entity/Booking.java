package com.digvijay.bookMyShow.entity;

import com.digvijay.bookMyShow.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<Seat> seats = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime bookingDateTime;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private Double discountApplied = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status; // PENDING, CONFIRMED, CANCELLED

    @Column(unique = true, nullable = false)
    private String bookingReference;

    // NEW FIELDS FOR ADVANCED BOOKING FLOW

    @Column(name = "lock_expiry")
    private LocalDateTime lockExpiry;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * Calculate final amount after discount
     */
    public Double getFinalAmount() {
        if (totalAmount == null) {
            return 0.0;
        }
        if (discountApplied == null) {
            return totalAmount;
        }
        return totalAmount - discountApplied;
    }

    public boolean isExpired() {
        return this.lockExpiry != null &&
                LocalDateTime.now().isAfter(this.lockExpiry);
    }
}

