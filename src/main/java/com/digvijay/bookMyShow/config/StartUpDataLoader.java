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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

 // Data Initializer - Populates database with sample data
 // This runs on application startup

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
    public void run(String... args) {

        // Create users
        createUsers();

        // Create movies
        log.info("Creating sample movies...");
        Movie movie1 = createMovie("Shutter Island", "A mind-bending thriller", "English", "Sci-Fi", 148, "UA");
        Movie movie2 = createMovie("The War Machine", "Dystopian", "English", "Action", 152, "UA");
        Movie movie3 = createMovie("F1", "Racing", "English", "Action", 187, "UA");
        log.info("Created {} movies", 3);

        // Create theatres
        log.info("Creating sample theatres...");
        Theatre theatre1 = createTheatre("Cinepolis", "Bengaluru", "Nexus Shantiniketan, Whitefield", 110);
        Theatre theatre2 = createTheatre("INOX", "Bengaluru", "Phoneix Marketcity, Whitefield", 120);
        Theatre theatre3 = createTheatre("PVR", "Delhi", "DLF Mall of India, Noida", 150);
        log.info("Created {} theatres", 3);

        // Create shows
        log.info("Creating sample shows...");
        LocalDate today = LocalDate.now();
        createShowsForMovie(movie1, theatre1, today);
        createShowsForMovie(movie1, theatre2, today);
        createShowsForMovie(movie2, theatre1, today);
        createShowsForMovie(movie3, theatre3, today);

        log.info("Data Initialization Completed Successfully!");
        log.info("Total Movies: {}, Total Theatres: {}, Total Shows: {}",
                movieRepository.count(), theatreRepository.count(), showRepository.count());

    }

    private void createUsers() {
        if (userRepository.count() == 0) {
            log.debug("Creating sample users...");

            User user1 = new User();
            user1.setUsername("john");
            user1.setEmail("john@example.com");
            user1.setPassword(passwordEncoder.encode("password123"));
            Set<String> roles1 = new HashSet<>();
            roles1.add("USER");
            user1.setRoles(roles1);
            userRepository.save(user1);

            User user2 = new User();
            user2.setUsername("digvijay");
            user2.setEmail("digvijay@example.com");
            user2.setPassword(passwordEncoder.encode("admin123"));
            Set<String> roles2 = new HashSet<>();
            roles2.add("USER");
            roles2.add("ADMIN");
            user2.setRoles(roles2);
            userRepository.save(user2);

            log.info(" Created 2 sample users (john/password123, admin/admin123)");
        }
    }

    private Movie createMovie(String title, String description, String language,
                              String genre, Integer duration, String rating) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setLanguage(language);
        movie.setGenre(genre);
        movie.setDurationMinutes(duration);
        movie.setRating(rating);
        Movie saved = movieRepository.save(movie);
        log.debug("Created movie: {} ({})", title, genre);
        return saved;
    }

    private Theatre createTheatre(String name, String city, String address, Integer totalSeats) {
        Theatre theatre = new Theatre();
        theatre.setName(name);
        theatre.setCity(city);
        theatre.setAddress(address);
        theatre.setTotalSeats(totalSeats);
        Theatre saved = theatreRepository.save(theatre);
        log.debug("Created theatre: {} in {} ({} seats)", name, city, totalSeats);
        return saved;
    }

    private void createShowsForMovie(Movie movie, Theatre theatre, LocalDate date) {
        log.debug("Creating shows for '{}' at '{}'", movie.getTitle(), theatre.getName());

        // Morning show - 10:00 AM
        createShow(movie, theatre, date.atTime(10, 0), ShowType.MORNING, 200.0);

        // Afternoon show - 2:00 PM
        createShow(movie, theatre, date.atTime(14, 0), ShowType.AFTERNOON, 150.0);

        // Evening show - 6:30 PM
        createShow(movie, theatre, date.atTime(18, 30), ShowType.EVENING, 250.0);

        // Night show - 9:30 PM
        createShow(movie, theatre, date.atTime(21, 30), ShowType.NIGHT, 220.0);
    }

    private void createShow(Movie movie, Theatre theatre, LocalDateTime dateTime,
                            ShowType showType, Double basePrice) {
        Show show = new Show();
        show.setMovie(movie);
        show.setTheatre(theatre);
        show.setShowDateTime(dateTime);
        show.setShowType(showType);
        show.setBasePrice(basePrice);
        show.setAvailableSeats(theatre.getTotalSeats());
        show = showRepository.save(show);

        // Create seats for the show
        createSeatsForShow(show, theatre.getTotalSeats(), basePrice);
    }

    private void createSeatsForShow(Show show, Integer totalSeats, Double basePrice) {
        int regularSeats = (int) (totalSeats * 0.6);  // 60% regular
        int premiumSeats = (int) (totalSeats * 0.3);  // 30% premium
        int vipSeats = totalSeats - regularSeats - premiumSeats;  // 10% VIP

        int seatCounter = 1;

        // Create regular seats
        for (int i = 0; i < regularSeats; i++) {
            createSeat(show, "R" + seatCounter++, SeatType.REGULAR, basePrice);
        }

        // Create premium seats
        for (int i = 0; i < premiumSeats; i++) {
            createSeat(show, "P" + seatCounter++, SeatType.PREMIUM, basePrice * 1.5);
        }

        // Create VIP seats
        for (int i = 0; i < vipSeats; i++) {
            createSeat(show, "V" + seatCounter++, SeatType.VIP, basePrice * 2.0);
        }
    }

    private void createSeat(Show show, String seatNumber, SeatType seatType, Double price) {
        Seat seat = new Seat();
        seat.setShow(show);
        seat.setSeatNumber(seatNumber);
        seat.setSeatType(seatType);
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setPrice(price);
        seatRepository.save(seat);
    }
}