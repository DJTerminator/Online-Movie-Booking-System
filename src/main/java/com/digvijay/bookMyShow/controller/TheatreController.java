package com.digvijay.bookMyShow.controller;

import com.digvijay.bookMyShow.dto.TheatreDTO;
import com.digvijay.bookMyShow.service.TheatreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theatres")
@RequiredArgsConstructor
@Slf4j
public class TheatreController {

    private final TheatreService theatreService;

    /** GET /api/theatres — List all theatres */
    @GetMapping
    public ResponseEntity<List<TheatreDTO>> getAllTheatres() {
        log.info("GET /api/theatres");
        return ResponseEntity.ok(theatreService.getAllTheatres());
    }

    /** GET /api/theatres/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<TheatreDTO> getTheatreById(@PathVariable Long id) {
        log.info("GET /api/theatres/{}", id);
        return ResponseEntity.ok(theatreService.getTheatreById(id));
    }

    /** GET /api/theatres/city/{city} */
    @GetMapping("/city/{city}")
    public ResponseEntity<List<TheatreDTO>> getTheatresByCity(@PathVariable String city) {
        log.info("GET /api/theatres/city/{}", city);
        return ResponseEntity.ok(theatreService.getTheatresByCity(city));
    }

    /** POST /api/theatres — Admin only */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TheatreDTO> createTheatre(@Valid @RequestBody TheatreDTO request) {
        log.info("POST /api/theatres — name: {}, city: {}", request.getName(), request.getCity());
        return ResponseEntity.status(HttpStatus.CREATED).body(theatreService.createTheatre(request));
    }
}
