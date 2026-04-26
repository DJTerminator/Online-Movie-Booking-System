package com.digvijay.bookMyShow.entity;

import com.digvijay.bookMyShow.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_user", columnList = "user_id"),
        @Index(name = "idx_booking_reference", columnList = "booking_reference", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "seats")
@ToString(exclude = "seats")
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

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Seat> seats = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime bookingDateTime;

    // Gross total BEFORE discount
    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private Double discountApplied = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(unique = true, nullable = false)
    private String bookingReference;

    @Column(name = "lock_expiry")
    private LocalDateTime lockExpiry;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** Net amount after discount */
    public Double getFinalAmount() {
        if (totalAmount == null) return 0.0;
        return totalAmount - (discountApplied != null ? discountApplied : 0.0);
    }

    public boolean isExpired() {
        return lockExpiry != null && LocalDateTime.now().isAfter(lockExpiry);
    }
}
