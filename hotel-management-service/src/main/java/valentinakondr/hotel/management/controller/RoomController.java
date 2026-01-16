package valentinakondr.hotel.management.controller;

import valentinakondr.hotel.management.controller.api.AvailabilityDto;
import valentinakondr.hotel.management.controller.api.RoomDto;
import valentinakondr.hotel.management.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomDto createRoom(@RequestBody RoomDto dto) {
        return roomService.createRoom(dto);
    }

    @GetMapping
    public List<RoomDto> getRooms() {
        return roomService.getRooms();
    }

    @GetMapping("/recommend")
    public List<RoomDto> getRecommendedRooms() {
        return roomService.getRecommendedRooms();
    }

    @PostMapping("/{id}/confirm-availability")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable("id") UUID id, @RequestBody AvailabilityDto request) {
        roomService.confirm(id, request);
    }

    @PostMapping("/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable("id") UUID id, @RequestParam("requestId") UUID requestId) {
        roomService.release(id, requestId);
    }
}
