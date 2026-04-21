package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.dto.ShowDTO;
import com.digvijay.bookMyShow.entity.Show;
import com.digvijay.bookMyShow.exceptions.ResourceNotFoundException;
import com.digvijay.bookMyShow.repository.MovieRepository;
import com.digvijay.bookMyShow.repository.ShowRepository;
import com.digvijay.bookMyShow.service.BrowsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BrowsingServiceImpl implements BrowsingService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;

    @Override
    public List<ShowDTO> browseShowsByMovieCityAndDate(Long movieId, String city, LocalDate date) {
        log.info("Browsing shows - Movie ID: {}, City: {}, Date: {}", movieId, city, date);

        // Validate movie exists
        movieRepository.findById(movieId)
                .orElseThrow(() -> {
                    log.error("Movie not found - Movie ID: {}", movieId);
                    return new ResourceNotFoundException("Movie not found with id: " + movieId);
                });

        log.debug("Movie validated successfully - Movie ID: {}", movieId);

        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

        log.debug("Searching shows between {} and {}", startDateTime, endDateTime);

        List<Show> shows = showRepository.findShowsByMovieAndCityBetweenDates(
                movieId, city, startDateTime, endDateTime
        );

        log.info("Found {} shows for Movie ID: {} in City: {} on Date: {}",
                shows.size(), movieId, city, date);

        if (shows.isEmpty()) {
            log.warn("No shows found for the given criteria - Movie ID: {}, City: {}, Date: {}",
                    movieId, city, date);
        }

        List<ShowDTO> showDTOs = shows.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.debug("Successfully converted {} shows to DTOs", showDTOs.size());

        return showDTOs;
    }

    private ShowDTO convertToDTO(Show show) {
        ShowDTO dto = new ShowDTO();
        dto.setId(show.getId());
        dto.setMovieId(show.getMovie().getId());
        dto.setMovieTitle(show.getMovie().getTitle());
        dto.setTheatreId(show.getTheatre().getId());
        dto.setTheatreName(show.getTheatre().getName());
        dto.setTheatreCity(show.getTheatre().getCity());
        dto.setTheatreAddress(show.getTheatre().getAddress());
        dto.setShowDateTime(show.getShowDateTime());
        dto.setBasePrice(show.getBasePrice());
        dto.setShowType(show.getShowType().name());
        dto.setAvailableSeats(show.getAvailableSeats());
        return dto;
    }
}