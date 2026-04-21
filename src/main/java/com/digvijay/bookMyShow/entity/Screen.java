package com.digvijay.bookMyShow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "screen")
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Screen {

    @Id
    private final String id;
    private final String name;
    @ManyToOne
    private final Theatre theatre;
    //Other screen metadata.

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL)
    private final List<Seat> seats;

    public Screen(String id, String name, Theatre theatre) {
        this.id = id;
        this.name = name;
        this.theatre = theatre;
        this.seats = new ArrayList<>();
    }

    public void addSeat(@NotNull Seat seat) {
        this.seats.add(seat);
    }

}
