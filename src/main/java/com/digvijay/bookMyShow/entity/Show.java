package com.digvijay.bookMyShow.entity;

import com.digvijay.bookMyShow.enums.ShowType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shows", indexes = {
        @Index(name = "idx_show_movie", columnList = "movie_id"),
        @Index(name = "idx_show_theatre", columnList = "theatre_id"),
        @Index(name = "idx_show_datetime", columnList = "show_date_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"seats", "bookings"})
@ToString(exclude = {"seats", "bookings"})
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theatre_id", nullable = false)
    private Theatre theatre;

    @Column(name = "show_date_time", nullable = false)
    private LocalDateTime showDateTime;

    @Column(nullable = false)
    private Double basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowType showType;

    @Column(nullable = false)
    private Integer availableSeats;

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();
}
