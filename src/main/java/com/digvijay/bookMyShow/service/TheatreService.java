package com.digvijay.bookMyShow.service;

import com.digvijay.bookMyShow.dto.TheatreDTO;
import java.util.List;

public interface TheatreService {
    List<TheatreDTO> getAllTheatres();
    TheatreDTO getTheatreById(Long id);
    List<TheatreDTO> getTheatresByCity(String city);
    TheatreDTO createTheatre(TheatreDTO request);
}
