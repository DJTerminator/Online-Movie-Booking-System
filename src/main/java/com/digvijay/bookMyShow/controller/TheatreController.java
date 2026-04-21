package com.digvijay.bookMyShow.controller;

import com.digvijay.bookMyShow.entity.Theatre;
import com.digvijay.bookMyShow.service.TheatreService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/theatre")
@RequiredArgsConstructor
@Slf4j
public class TheatreController {
    final private TheatreService theatreService;

    public Long createTheatre(@NonNull final String theatreName) {
        return theatreService.createTheatre(theatreName).getId();
    }

    public String createScreenInTheatre(@NonNull final String screenName, @NonNull final String theatreId) {
        final Theatre theatre = theatreService.getTheatre(theatreId);
        return theatreService.createScreenInTheatre(screenName, theatre).getId();
    }

//    public String createSeatInScreen(@NonNull final Integer rowNo, @NonNull final Integer seatNo, @NonNull final String screenId) {
//        final Screen screen = theatreService.getScreen(screenId);
//        return theatreService.createSeatInScreen(rowNo, seatNo, screen).getId();
//    }
}
