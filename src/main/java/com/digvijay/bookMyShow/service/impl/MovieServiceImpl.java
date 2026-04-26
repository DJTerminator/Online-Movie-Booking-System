package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.dto.MovieDTO;
import com.digvijay.bookMyShow.entity.Movie;
import com.digvijay.bookMyShow.exceptions.BookingException;
import com.digvijay.bookMyShow.exceptions.ResourceNotFoundException;
import com.digvijay.bookMyShow.repository.MovieRepository;
import com.digvijay.bookMyShow.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    /**
     * Cached: key "allMovies" in cache "movies".
     * Served from Caffeine on repeated calls — no DB hit.
     * Evicted whenever createMovie(), updateMovie(), or deleteMovie() is called.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "movies", key = "'allMovies'")
    public List<MovieDTO> getAllMovies() {
        log.info("Fetching all active movies");
        return movieRepository.findByActiveTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MovieDTO getMovieById(Long id) {
        log.info("Fetching movie by id: {}", id);
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "movies", key = "'allMovies'")
    public MovieDTO createMovie(MovieDTO request) {
        log.info("Creating movie: {} — cache 'movies' evicted", request.getTitle());
        if (movieRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new BookingException("Movie with title '" + request.getTitle() + "' already exists");
        }
        Movie movie = new Movie();
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setLanguage(request.getLanguage());
        movie.setGenre(request.getGenre());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setRating(request.getRating());
        movie.setActive(true);
        Movie saved = movieRepository.save(movie);
        log.info("Movie created with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "movies", key = "'allMovies'")
    public MovieDTO updateMovie(Long id, MovieDTO request) {
        Movie movie = findOrThrow(id);
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setLanguage(request.getLanguage());
        movie.setGenre(request.getGenre());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setRating(request.getRating());
        return toDTO(movieRepository.save(movie));
    }

    @Override
    @Transactional
    @CacheEvict(value = "movies", key = "'allMovies'")
    public void deleteMovie(Long id) {
        Movie movie = findOrThrow(id);
        movie.setActive(false); // Soft delete
        movieRepository.save(movie);
        log.info("Movie soft-deleted: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> searchMovies(String title, String genre, String language) {
        log.info("Searching movies — title: {}, genre: {}, language: {}", title, genre, language);
        List<Movie> movies;
        if (StringUtils.hasText(title)) {
            movies = movieRepository.findByTitleContainingIgnoreCase(title);
        } else if (StringUtils.hasText(genre)) {
            movies = movieRepository.findByGenreIgnoreCase(genre);
        } else if (StringUtils.hasText(language)) {
            movies = movieRepository.findByLanguageIgnoreCase(language);
        } else {
            movies = movieRepository.findByActiveTrue();
        }
        return movies.stream().filter(Movie::getActive).map(this::toDTO).collect(Collectors.toList());
    }

    private Movie findOrThrow(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }

    private MovieDTO toDTO(Movie m) {
        MovieDTO dto = new MovieDTO();
        dto.setId(m.getId());
        dto.setTitle(m.getTitle());
        dto.setDescription(m.getDescription());
        dto.setLanguage(m.getLanguage());
        dto.setGenre(m.getGenre());
        dto.setDurationMinutes(m.getDurationMinutes());
        dto.setRating(m.getRating());
        dto.setActive(m.getActive());
        return dto;
    }
}
