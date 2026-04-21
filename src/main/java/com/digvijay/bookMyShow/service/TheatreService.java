package com.digvijay.bookMyShow.service;

import com.digvijay.bookMyShow.entity.Screen;
import com.digvijay.bookMyShow.entity.Theatre;

public interface TheatreService {

    Theatre createTheatre(String theatreName);

    Theatre getTheatre(String theatreId);

    Screen createScreenInTheatre(String screenName, Theatre theatre);

    Screen getScreen(String screenId);

//    Seat createSeatInScreen(Integer rowNo, Integer seatNo, Screen screen);
}
