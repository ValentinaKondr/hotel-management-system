package valentinakondr.booking.config.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import valentinakondr.booking.domain.User;
import valentinakondr.booking.domain.booking.Booking;
import valentinakondr.booking.domain.booking.BookingStatus;
import valentinakondr.booking.repo.BookingRepository;
import valentinakondr.booking.repo.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInjectRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0 || bookingRepository.count() > 0) {
            log.info("[data.inject.skip] users={} bookings={}",
                    userRepository.count(), bookingRepository.count());
            return;
        }

        log.info("[data.inject.start]");

        // -----------------------
        // Users
        // -----------------------
        User admin = new User(
                null,
                "valentinakondr.admin",
                passwordEncoder.encode("Admin123!"),
                "ADMIN"
        );

        User manager = new User(
                null,
                "hotel.manager",
                passwordEncoder.encode("Manager123!"),
                "ADMIN"
        );

        User user1 = new User(
                null,
                "alex.petrov",
                passwordEncoder.encode("Pass123!"),
                "USER"
        );

        User user2 = new User(
                null,
                "katya.smirnova",
                passwordEncoder.encode("Pass123!"),
                "USER"
        );

        User user3 = new User(
                null,
                "dima.volkov",
                passwordEncoder.encode("Pass123!"),
                "USER"
        );

        admin = userRepository.save(admin);
        manager = userRepository.save(manager);
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);
        user3 = userRepository.save(user3);

        log.info("[data.inject.users] created users: {}",
                userRepository.count());

        // -----------------------
        // Rooms (фиксированные UUID чтобы удобно тестить)
        // -----------------------
        UUID room101 = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID room205 = UUID.fromString("00000000-0000-0000-0000-000000000205");
        UUID room310 = UUID.fromString("00000000-0000-0000-0000-000000000310");
        UUID room401 = UUID.fromString("00000000-0000-0000-0000-000000000401");

        // -----------------------
        // Bookings
        // -----------------------
        Booking booking1 = new Booking(
                null,
                user1,
                room101,
                LocalDate.now().plusDays(2),          // скоро
                LocalDate.now().plusDays(5),
                BookingStatus.CONFIRMED,
                LocalDateTime.now().minusHours(10),
                UUID.randomUUID()
        );

        Booking booking2 = new Booking(
                null,
                user1,
                room205,
                LocalDate.now().plusDays(20),         // будущая бронь
                LocalDate.now().plusDays(23),
                BookingStatus.PENDING,
                LocalDateTime.now().minusHours(3),
                UUID.randomUUID()
        );

        Booking booking3 = new Booking(
                null,
                user2,
                room310,
                LocalDate.now().minusDays(7),         // уже была (но отменили)
                LocalDate.now().minusDays(4),
                BookingStatus.CANCELLED,
                LocalDateTime.now().minusDays(8),
                UUID.randomUUID()
        );

        Booking booking4 = new Booking(
                null,
                user2,
                room401,
                LocalDate.now().plusDays(1),          // завтра
                LocalDate.now().plusDays(2),
                BookingStatus.CONFIRMED,
                LocalDateTime.now().minusHours(6),
                UUID.randomUUID()
        );

        Booking booking5 = new Booking(
                null,
                user3,
                room205,
                LocalDate.now().plusDays(14),         // через две недели
                LocalDate.now().plusDays(16),
                BookingStatus.CONFIRMED,
                LocalDateTime.now().minusDays(1).minusHours(5),
                UUID.randomUUID()
        );

        bookingRepository.save(booking1);
        bookingRepository.save(booking2);
        bookingRepository.save(booking3);
        bookingRepository.save(booking4);
        bookingRepository.save(booking5);

        log.info("[data.inject.bookings] created bookings: {}",
                bookingRepository.count());

        log.info("[data.inject.done] test accounts:");
        log.info("  ADMIN:   valentinakondr.admin / Admin123!");
        log.info("  ADMIN:   hotel.manager / Manager123!");
        log.info("  USER:    alex.petrov / Pass123!");
        log.info("  USER:    katya.smirnova / Pass123!");
        log.info("  USER:    dima.volkov / Pass123!");
    }
}