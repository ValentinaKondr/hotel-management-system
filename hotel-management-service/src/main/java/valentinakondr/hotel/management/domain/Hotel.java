package valentinakondr.hotel.management.domain;

import valentinakondr.hotel.management.controller.api.HotelDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hotel")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL)
    private List<Room> rooms = new ArrayList<>();


    public static Hotel fromDto(HotelDto dto) {
        Hotel hotel = new Hotel();
        hotel.setName(dto.name());
        hotel.setAddress(dto.address());
        return hotel;
    }
}
