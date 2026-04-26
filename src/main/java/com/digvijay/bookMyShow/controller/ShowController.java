package com.digvijay.bookMyShow.controller;

import com.digvijay.bookMyShow.dto.CreateShowRequest;
import com.digvijay.bookMyShow.dto.SeatDTO;
import com.digvijay.bookMyShow.dto.ShowDTO;
import com.digvijay.bookMyShow.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@Slf4j
public class ShowController {

    private final ShowService showService;

    /**
     * GET /api/shows/browse?movieId=1&city=Bengaluru&date=2025-04-26
     * Public — Browse available shows by movie, city, date
     */
    @GetMapping("/browse")
    public ResponseEntity<List<ShowDTO>> browseShows(
            @RequestParam Long movieId,
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /api/shows/browse — movieId: {}, city: {}, date: {}", movieId, city, date);
        List<ShowDTO> shows = showService.browseShows(movieId, city, date);
        log.info("Found {} shows", shows.size());
        return ResponseEntity.ok(shows);
    }

    /**
     * GET /api/shows/{id}
     * Get show details by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShowDTO> getShowById(@PathVariable Long id) {
        log.info("GET /api/shows/{}", id);
        return ResponseEntity.ok(showService.getShowById(id));
    }

    /**
     * GET /api/shows/{id}/seats
     * Fetch all seats for a show — used by seat selection UI
     */
    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatDTO>> getSeatsByShow(@PathVariable Long id) {
        log.info("GET /api/shows/{}/seats", id);
        return ResponseEntity.ok(showService.getSeatsByShow(id));
    }

    /**
     * POST /api/shows — Admin only
     * Create a new show for a movie at a theatre
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowDTO> createShow(@Valid @RequestBody CreateShowRequest request) {
        log.info("POST /api/shows — movieId: {}, theatreId: {}", request.getMovieId(), request.getTheatreId());
        return ResponseEntity.status(HttpStatus.CREATED).body(showService.createShow(request));
    }
}
