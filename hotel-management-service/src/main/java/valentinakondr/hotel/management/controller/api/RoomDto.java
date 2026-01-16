package valentinakondr.hotel.management.controller.api;

import valentinakondr.hotel.management.domain.Room;

import java.util.UUID;

public record RoomDto(
        UUID id,
        UUID hotelId,
        String number,
        Boolean available,
        Integer timesBooked
) {

    public RoomDto(Room room) {
        this(
                room.getId(),
                room.getHotel().getId(),
                room.getNumber(),
                room.getAvailable(),
                room.getTimesBooked()
        );
    }
}
