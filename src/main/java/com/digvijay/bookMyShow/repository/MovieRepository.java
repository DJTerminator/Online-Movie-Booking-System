package com.digvijay.bookMyShow.repository;

import com.digvijay.bookMyShow.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByActiveTrue();
    List<Movie> findByLanguageIgnoreCase(String language);
    List<Movie> findByGenreIgnoreCase(String genre);
    List<Movie> findByTitleContainingIgnoreCase(String title);
    boolean existsByTitleIgnoreCase(String title);
}
