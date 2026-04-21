package com.digvijay.bookMyShow.controller;

import com.digvijay.bookMyShow.dto.ShowDTO;
import com.digvijay.bookMyShow.service.BrowsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@Slf4j
public class ShowController {

    private final BrowsingService browsingService;


 // READ SCENARIO: Browse theatres showing a selected movie in a town with show timings by date

    @GetMapping("/browse")
    public ResponseEntity<List<ShowDTO>> browseShows(
            @RequestParam Long movieId,
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Parameters - Movie ID: {}, City: {}, Date: {}", movieId, city, date);

        List<ShowDTO> shows = browsingService.browseShowsByMovieCityAndDate(movieId, city, date);

        log.info("<<< Response: {} shows found", shows.size());
        return ResponseEntity.ok(shows);
    }
}
