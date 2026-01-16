package valentinakondr.hotel.management.service;

import valentinakondr.hotel.management.controller.api.AvailabilityDto;
import valentinakondr.hotel.management.controller.api.RoomDto;

import java.util.List;
import java.util.UUID;

public interface RoomService {
    RoomDto createRoom(RoomDto roomDto);
    List<RoomDto> getRooms();
    List<RoomDto> getRecommendedRooms();
    void confirm(UUID id, AvailabilityDto availabilityDto);
    void release(UUID id, UUID rqUid);
}
