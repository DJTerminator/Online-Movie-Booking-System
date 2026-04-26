package com.digvijay.bookMyShow.controller;

import com.digvijay.bookMyShow.dto.MovieDTO;
import com.digvijay.bookMyShow.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {

    private final MovieService movieService;

    /** GET /api/movies — List all active movies */
    @GetMapping
    public ResponseEntity<List<MovieDTO>> getAllMovies() {
        log.info("GET /api/movies");
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    /** GET /api/movies/{id} — Get movie by ID */
    @GetMapping("/{id}")
    public ResponseEntity<MovieDTO> getMovieById(@PathVariable Long id) {
        log.info("GET /api/movies/{}", id);
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    /** GET /api/movies/search?title=&genre=&language= */
    @GetMapping("/search")
    public ResponseEntity<List<MovieDTO>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String language) {
        log.info("GET /api/movies/search — title: {}, genre: {}, language: {}", title, genre, language);
        return ResponseEntity.ok(movieService.searchMovies(title, genre, language));
    }

    /** POST /api/movies — Admin only */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieDTO> createMovie(@Valid @RequestBody MovieDTO request) {
        log.info("POST /api/movies — title: {}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.createMovie(request));
    }

    /** PUT /api/movies/{id} — Admin only */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieDTO> updateMovie(@PathVariable Long id, @Valid @RequestBody MovieDTO request) {
        log.info("PUT /api/movies/{}", id);
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    /** DELETE /api/movies/{id} — Admin only (soft delete) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        log.info("DELETE /api/movies/{}", id);
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}

