package com.digvijay.bookMyShow.config;

import com.digvijay.bookMyShow.entity.*;
import com.digvijay.bookMyShow.enums.SeatStatus;
import com.digvijay.bookMyShow.enums.SeatType;
import com.digvijay.bookMyShow.enums.ShowType;
import com.digvijay.bookMyShow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartUpDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        if (movieRepository.count() > 0) {
            log.info("Database already seeded. Skipping data initialization.");
            return;
        }

        log.info("=== Starting database seeding ===");
        createUsers();

        Movie m1 = createMovie("Shutter Island", "A mind-bending psychological thriller", "English", "Thriller", 148, "UA");
        Movie m2 = createMovie("The War Machine", "Dystopian future warfare epic", "English", "Action", 152, "UA");
        Movie m3 = createMovie("F1", "High-octane Formula 1 racing drama", "English", "Action", 187, "UA");
        log.info("Created 3 movies");

        Theatre t1 = createTheatre("Cinepolis", "Bengaluru", "Nexus Shantiniketan, Whitefield", 110);
        Theatre t2 = createTheatre("INOX", "Bengaluru", "Phoenix Marketcity, Whitefield", 120);
        Theatre t3 = createTheatre("PVR", "Delhi", "DLF Mall of India, Noida", 150);
        log.info("Created 3 theatres");

        LocalDate today = LocalDate.now();
        createShowsForMovie(m1, t1, today);
        createShowsForMovie(m1, t2, today);
        createShowsForMovie(m2, t1, today);
        createShowsForMovie(m3, t3, today);

        log.info("=== Seeding complete — Movies: {}, Theatres: {}, Shows: {}, Seats: {} ===",
                movieRepository.count(), theatreRepository.count(),
                showRepository.count(), seatRepository.count());
        log.info("Test credentials — john/password123 (USER), digvijay/admin123 (USER+ADMIN)");
    }

    private void createUsers() {
        if (userRepository.count() > 0) return;

        User john = new User();
        john.setUsername("john");
        john.setEmail("john@example.com");
        john.setPassword(passwordEncoder.encode("password123"));
        john.setRoles(new HashSet<>(Set.of("USER")));
        userRepository.save(john);

        User admin = new User();
        admin.setUsername("digvijay");
        admin.setEmail("digvijay@example.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRoles(new HashSet<>(Set.of("USER", "ADMIN")));
        userRepository.save(admin);

        log.info("Created users: john (USER), digvijay (USER+ADMIN)");
    }

    private Movie createMovie(String title, String desc, String lang, String genre, int duration, String rating) {
        Movie m = new Movie();
        m.setTitle(title);
        m.setDescription(desc);
        m.setLanguage(lang);
        m.setGenre(genre);
        m.setDurationMinutes(duration);
        m.setRating(rating);
        m.setActive(true);
        return movieRepository.save(m);
    }

    private Theatre createTheatre(String name, String city, String address, int seats) {
        Theatre t = new Theatre();
        t.setName(name);
        t.setCity(city);
        t.setAddress(address);
        t.setTotalSeats(seats);
        return theatreRepository.save(t);
    }

    private void createShowsForMovie(Movie movie, Theatre theatre, LocalDate date) {
        createShow(movie, theatre, date.atTime(10, 0), ShowType.MORNING, 200.0);
        createShow(movie, theatre, date.atTime(14, 0), ShowType.AFTERNOON, 150.0);
        createShow(movie, theatre, date.atTime(18, 30), ShowType.EVENING, 250.0);
        createShow(movie, theatre, date.atTime(21, 30), ShowType.NIGHT, 220.0);
    }

    private void createShow(Movie movie, Theatre theatre, LocalDateTime dt, ShowType type, double basePrice) {
        Show show = new Show();
        show.setMovie(movie);
        show.setTheatre(theatre);
        show.setShowDateTime(dt);
        show.setShowType(type);
        show.setBasePrice(basePrice);
        show.setAvailableSeats(theatre.getTotalSeats());
        Show saved = showRepository.save(show);
        createSeatsForShow(saved, theatre.getTotalSeats(), basePrice);
    }

    private void createSeatsForShow(Show show, int total, double basePrice) {
        int regular = (int) (total * 0.6);
        int premium = (int) (total * 0.3);
        int vip = total - regular - premium;

        // FIX: Collect all seats then saveAll in one batch — was N individual save() calls
        List<Seat> seats = new ArrayList<>();
        int counter = 1;
        for (int i = 0; i < regular; i++){
            seats.add(makeSeat(show, "R" + counter++, SeatType.REGULAR, basePrice));
        }
        for (int i = 0; i < premium; i++){
            seats.add(makeSeat(show, "P" + counter++, SeatType.PREMIUM, basePrice * 1.5));
        }
        for (int i = 0; i < vip; i++) {
            seats.add(makeSeat(show, "V" + counter++, SeatType.VIP, basePrice * 2.0));
        }
        seatRepository.saveAll(seats);   // FIX: One batch insert instead of N individual inserts
    }

    private Seat makeSeat(Show show, String number, SeatType type, double price) {
        Seat s = new Seat();
        s.setShow(show);
        s.setSeatNumber(number);
        s.setSeatType(type);
        s.setStatus(SeatStatus.AVAILABLE);
        s.setPrice(price);
        return s;
    }
}
