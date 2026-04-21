package com.digvijay.bookMyShow.service;

import com.digvijay.bookMyShow.dto.BookingRequest;
import com.digvijay.bookMyShow.dto.BookingResponse;

public interface BookingService {

    BookingResponse bookTickets(BookingRequest request, String username);
}
