package com.digvijay.bookMyShow.repository;

import com.digvijay.bookMyShow.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    List<Theatre> findByCity(String city);

    List<Theatre> findByCityIgnoreCase(String city);

    List<Theatre> findByNameContainingIgnoreCase(String name);
}
