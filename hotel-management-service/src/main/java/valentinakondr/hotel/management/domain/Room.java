package valentinakondr.hotel.management.domain;

import valentinakondr.hotel.management.controller.api.RoomDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "room")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private String number;

    @Column(nullable = false)
    private Boolean available = true;

    @Column(nullable = false)
    private Integer timesBooked = 0;

    @Version
    private Long version;

    public static Room fromDto(Hotel hotel, RoomDto dto, Integer timesBooked) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(dto.number());
        room.setAvailable(true);
        room.setTimesBooked(timesBooked);
        return room;
    }
}
