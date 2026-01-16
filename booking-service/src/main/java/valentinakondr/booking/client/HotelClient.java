package valentinakondr.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import valentinakondr.booking.config.feign.FeignAuthRequestInterceptor;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "hotel-service", configuration = FeignAuthRequestInterceptor.class)
public interface HotelClient {
    @PostMapping("/api/rooms/{id}/confirm-availability")
    void confirm(
            @PathVariable("id") UUID roomId,
            @RequestBody AvailabilityRequestDto request
    );

    @GetMapping("/api/rooms/recommend")
    List<RoomDto> getRooms();

    @PostMapping("/api/rooms/{id}/release")
    void release(
            @PathVariable("id") UUID roomId,
            @RequestParam("rqUid") UUID requestId
    );
}
