package com.digvijay.bookMyShow.service;

import com.digvijay.bookMyShow.dto.ShowDTO;

import java.time.LocalDate;
import java.util.List;

public interface BrowsingService {

    List<ShowDTO> browseShowsByMovieCityAndDate(Long movieId, String city, LocalDate date);
}
