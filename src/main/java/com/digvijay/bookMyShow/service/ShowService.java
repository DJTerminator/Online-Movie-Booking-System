package com.digvijay.bookMyShow.service;

import com.digvijay.bookMyShow.dto.CreateShowRequest;
import com.digvijay.bookMyShow.dto.SeatDTO;
import com.digvijay.bookMyShow.dto.ShowDTO;
import java.time.LocalDate;
import java.util.List;

public interface ShowService {
    List<ShowDTO> browseShows(Long movieId, String city, LocalDate date);
    ShowDTO getShowById(Long showId);
    ShowDTO createShow(CreateShowRequest request);
    List<SeatDTO> getSeatsByShow(Long showId);
}
