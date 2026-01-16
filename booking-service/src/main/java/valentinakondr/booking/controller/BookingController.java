package valentinakondr.booking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import valentinakondr.booking.dto.BookingDto;
import valentinakondr.booking.dto.CreateBookingRequestDto;
import valentinakondr.booking.service.BookingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping("/booking")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto createBooking(
            Authentication authentication,
            @RequestBody CreateBookingRequestDto request) {
        return bookingService.create(authentication, request);
    }

    @GetMapping("/bookings")
    public List<BookingDto> getUserBookings(Authentication authentication) {
        return bookingService.findAll(authentication);
    }

    @GetMapping("/booking/{id}")
    public BookingDto getBooking(
            Authentication authentication,
            @PathVariable("id") UUID id
    ) {
        return bookingService.findById(authentication, id);
    }

    @DeleteMapping("/booking/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBooking(
            Authentication authentication,
            @PathVariable("id") UUID id
    ) {
        bookingService.cancel(authentication, id);
    }
}