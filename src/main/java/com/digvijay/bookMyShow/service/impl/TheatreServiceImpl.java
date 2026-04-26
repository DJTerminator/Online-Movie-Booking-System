package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.dto.TheatreDTO;
import com.digvijay.bookMyShow.entity.Theatre;
import com.digvijay.bookMyShow.exceptions.BookingException;
import com.digvijay.bookMyShow.exceptions.ResourceNotFoundException;
import com.digvijay.bookMyShow.repository.TheatreRepository;
import com.digvijay.bookMyShow.service.TheatreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;     // FIX: Was missing — not registered as a bean
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TheatreServiceImpl implements TheatreService {

    private final TheatreRepository theatreRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TheatreDTO> getAllTheatres() {
        return theatreRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TheatreDTO getTheatreById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TheatreDTO> getTheatresByCity(String city) {
        log.info("Fetching theatres in city: {}", city);
        return theatreRepository.findByCityIgnoreCase(city).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TheatreDTO createTheatre(TheatreDTO request) {

        log.info("Creating theatre: {} in {}", request.getName(), request.getCity());
        if (theatreRepository.existsByNameIgnoreCaseAndCityIgnoreCase(request.getName(), request.getCity())) {
            throw new BookingException("Theatre '" + request.getName() + "' already exists in " + request.getCity());
        }

        Theatre theatre = new Theatre();
        theatre.setName(request.getName());
        theatre.setCity(request.getCity());
        theatre.setAddress(request.getAddress());
        theatre.setTotalSeats(request.getTotalSeats());
        Theatre saved = theatreRepository.save(theatre);
        log.info("Theatre created with id: {}", saved.getId());
        return toDTO(saved);
    }

    private Theatre findOrThrow(Long id) {
        return theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre", id));
    }

    private TheatreDTO toDTO(Theatre t) {
        TheatreDTO dto = new TheatreDTO();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setCity(t.getCity());
        dto.setAddress(t.getAddress());
        dto.setTotalSeats(t.getTotalSeats());
        return dto;
    }
}
