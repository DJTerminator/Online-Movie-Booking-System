package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.dto.CreateShowRequest;
import com.digvijay.bookMyShow.dto.SeatDTO;
import com.digvijay.bookMyShow.dto.ShowDTO;
import com.digvijay.bookMyShow.entity.*;
import com.digvijay.bookMyShow.enums.ShowType;
import com.digvijay.bookMyShow.exceptions.ResourceNotFoundException;
import com.digvijay.bookMyShow.repository.*;
import com.digvijay.bookMyShow.service.ShowService;
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
public class ShowServiceImpl implements ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ShowDTO> browseShows(Long movieId, String city, LocalDate date) {
        log.info("Browsing shows — movie: {}, city: {}, date: {}", movieId, city, date);
        movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", movieId));

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Show> shows = showRepository.findAvailableShowsByMovieAndCityAndDateRange(movieId, city, start, end);
        log.info("Found {} shows", shows.size());
        return shows.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ShowDTO getShowById(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        return toDTO(show);
    }

    @Override
    @Transactional
    public ShowDTO createShow(CreateShowRequest request) {
        log.info("Creating show for movie: {}, theatre: {}", request.getMovieId(), request.getTheatreId());
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", request.getMovieId()));
        Theatre theatre = theatreRepository.findById(request.getTheatreId())
                .orElseThrow(() -> new ResourceNotFoundException("Theatre", request.getTheatreId()));

        Show show = new Show();
        show.setMovie(movie);
        show.setTheatre(theatre);
        show.setShowDateTime(request.getShowDateTime());
        show.setBasePrice(request.getBasePrice());
        show.setShowType(ShowType.valueOf(request.getShowType().toUpperCase()));
        show.setAvailableSeats(theatre.getTotalSeats());
        Show saved = showRepository.save(show);
        log.info("Show created with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatDTO> getSeatsByShow(Long showId) {
        log.info("Fetching seats for show: {}", showId);
        showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        return seatRepository.findByShowId(showId).stream()
                .map(this::toSeatDTO)
                .collect(Collectors.toList());
    }

    private ShowDTO toDTO(Show s) {
        ShowDTO dto = new ShowDTO();
        dto.setId(s.getId());
        dto.setMovieId(s.getMovie().getId());
        dto.setMovieTitle(s.getMovie().getTitle());
        dto.setTheatreId(s.getTheatre().getId());
        dto.setTheatreName(s.getTheatre().getName());
        dto.setTheatreCity(s.getTheatre().getCity());
        dto.setTheatreAddress(s.getTheatre().getAddress());
        dto.setShowDateTime(s.getShowDateTime());
        dto.setBasePrice(s.getBasePrice());
        dto.setShowType(s.getShowType().name());
        dto.setAvailableSeats(s.getAvailableSeats());
        return dto;
    }

    private SeatDTO toSeatDTO(Seat s) {
        SeatDTO dto = new SeatDTO();
        dto.setId(s.getId());
        dto.setSeatNumber(s.getSeatNumber());
        dto.setSeatType(s.getSeatType().name());
        dto.setStatus(s.getStatus().name());
        dto.setPrice(s.getPrice());
        return dto;
    }
}
