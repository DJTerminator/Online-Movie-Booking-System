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

    @Query("SELECT s FROM Show s " +
           "JOIN FETCH s.movie m " +
           "JOIN FETCH s.theatre t " +
           "WHERE m.id = :movieId " +
           "AND LOWER(t.city) = LOWER(:city) " +
           "AND s.showDateTime BETWEEN :startDate AND :endDate " +
           "AND s.availableSeats > 0 " +
           "ORDER BY s.showDateTime ASC")
    List<Show> findAvailableShowsByMovieAndCityAndDateRange(
            @Param("movieId") Long movieId,
            @Param("city") String city,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT s FROM Show s JOIN FETCH s.movie JOIN FETCH s.theatre WHERE s.theatre.id = :theatreId")
    List<Show> findShowsByTheatreWithDetails(@Param("theatreId") Long theatreId);
}
