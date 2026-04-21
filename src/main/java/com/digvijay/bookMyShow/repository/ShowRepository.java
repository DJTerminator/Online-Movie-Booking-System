package com.digvijay.bookMyShow.repository;

import com.digvijay.bookMyShow.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieId(Long movieId);

    List<Show> findByTheatreId(Long theatreId);

    @Query("SELECT s FROM Show s WHERE s.movie.id = :movieId " +
            "AND s.theatre.city = :city " +
            "AND DATE(s.showDateTime) = DATE(:date)")
    List<Show> findShowsByMovieAndCityAndDate(
            @Param("movieId") Long movieId,
            @Param("city") String city,
            @Param("date") LocalDateTime date
    );

    @Query("SELECT s FROM Show s WHERE s.movie.id = :movieId " +
            "AND s.theatre.city = :city " +
            "AND s.showDateTime BETWEEN :startDate AND :endDate")
    List<Show> findShowsByMovieAndCityBetweenDates(
            @Param("movieId") Long movieId,
            @Param("city") String city,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    List<Show> findByShowDateTimeBetween(LocalDateTime start, LocalDateTime end);
}
