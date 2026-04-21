package com.digvijay.bookMyShow.service;

import com.digvijay.bookMyShow.dto.SeatDTO;

import java.util.List;

public interface SeatLockingService {


    List<SeatDTO> lockSeats(String showId, List<String> seatIds, String userId);
}
