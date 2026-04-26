package com.digvijay.bookMyShow.service;

import com.digvijay.bookMyShow.dto.MovieDTO;
import java.util.List;

public interface MovieService {
    List<MovieDTO> getAllMovies();
    MovieDTO getMovieById(Long id);
    MovieDTO createMovie(MovieDTO request);
    MovieDTO updateMovie(Long id, MovieDTO request);
    void deleteMovie(Long id);
    List<MovieDTO> searchMovies(String title, String genre, String language);
}
