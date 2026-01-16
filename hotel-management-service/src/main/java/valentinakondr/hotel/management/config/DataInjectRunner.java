package valentinakondr.hotel.management.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import valentinakondr.hotel.management.dao.HotelRepository;
import valentinakondr.hotel.management.dao.RoomRepository;
import valentinakondr.hotel.management.domain.Hotel;
import valentinakondr.hotel.management.domain.Room;

import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInjectRunner implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        if (hotelRepository.count() > 0 || roomRepository.count() > 0) {
            log.info("[seed.skip] hotels={} rooms={}", hotelRepository.count(), roomRepository.count());
            return;
        }

        log.info("[seed.start]");

        // ---- Hotels ----
        Hotel theHoxton = new Hotel();
        theHoxton.setName("The Hoxton, Holborn");
        theHoxton.setAddress("199-206 High Holborn, London WC1V 7BD");

        Hotel citizenM = new Hotel();
        citizenM.setName("citizenM London Shoreditch");
        citizenM.setAddress("6 Holywell Ln, London EC2A 3ET");

        Hotel premierInn = new Hotel();
        premierInn.setName("Premier Inn London City (Aldgate)");
        premierInn.setAddress("66 Alie St, London E1 8PX");

        Hotel pointA = new Hotel();
        pointA.setName("Point A Hotel London Kings Cross");
        pointA.setAddress("324 Gray's Inn Rd, London WC1X 8BU");

        theHoxton = hotelRepository.save(theHoxton);
        citizenM = hotelRepository.save(citizenM);
        premierInn = hotelRepository.save(premierInn);
        pointA = hotelRepository.save(pointA);

        // ---- Rooms ----
        List<Room> rooms = List.of(
                room(theHoxton, "H-101", true, 12),
                room(theHoxton, "H-102", true, 4),
                room(theHoxton, "H-103", false, 18),
                room(theHoxton, "H-104", true, 1),

                room(citizenM, "C-201", true, 0),
                room(citizenM, "C-202", true, 7),
                room(citizenM, "C-203", false, 22),

                room(premierInn, "P-301", true, 3),
                room(premierInn, "P-302", true, 9),
                room(premierInn, "P-303", true, 2),
                room(premierInn, "P-304", false, 15),

                room(pointA, "KX-401", true, 5),
                room(pointA, "KX-402", true, 0),
                room(pointA, "KX-403", false, 11)
        );

        roomRepository.saveAll(rooms);

        log.info("[seed.hotels] count={}", hotelRepository.count());
        log.info("[seed.rooms] count={}", roomRepository.count());
        log.info("[seed.done]");
    }

    private static Room room(Hotel hotel, String number, boolean available, int timesBooked) {
        Room r = new Room();
        r.setHotel(hotel);
        r.setNumber(number);
        r.setAvailable(available);
        r.setTimesBooked(timesBooked);
        return r;
    }
}