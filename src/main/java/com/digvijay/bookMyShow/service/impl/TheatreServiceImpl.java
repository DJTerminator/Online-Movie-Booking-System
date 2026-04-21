package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.entity.Screen;
import com.digvijay.bookMyShow.entity.Theatre;
import com.digvijay.bookMyShow.exceptions.ResourceNotFoundException;
import com.digvijay.bookMyShow.repository.ScreenRepository;
import com.digvijay.bookMyShow.repository.SeatRepository;
import com.digvijay.bookMyShow.repository.TheatreRepository;
import com.digvijay.bookMyShow.service.TheatreService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class TheatreServiceImpl implements TheatreService {

    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;

    // Create Theatre
    @Override
    public Theatre createTheatre(String theatreName) {

        Theatre theatre = new Theatre();
        theatre.setName(theatreName);

        return theatreRepository.save(theatre);
    }

    // Get Theatre
    @Override
    public Theatre getTheatre(String theatreId) {
        return theatreRepository.findById(Long.valueOf(theatreId))
                .orElseThrow(() ->
                        new ResourceNotFoundException("Theatre not found: " + theatreId));
    }

    // Create Screen
    @Override
    public Screen createScreenInTheatre(String screenName, Theatre theatre) {

        String screenId = UUID.randomUUID().toString();
        Screen screen = new Screen(screenId, screenName, theatre);
        return screenRepository.save(screen);
    }

    //  Get Screen
    @Override
    public Screen getScreen(String screenId) {
        return screenRepository.findById(screenId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Screen not found: " + screenId));
    }

        //  Create Seat
//    @Override
//    public Seat createSeatInScreen(Integer rowNo, Integer seatNo, Screen screen) {
//
//        String seatId = UUID.randomUUID().toString();
//        Seat seat = new Seat(seatId, "R" + rowNo + "S" + seatNo, screen);
//        screen.addSeat(seat); // maintain bidirectional relation
//        return seatRepository.save(seat);
//    }
}
