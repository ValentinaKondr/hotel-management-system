package valentinakondr.booking.service;

import org.springframework.security.core.Authentication;
import valentinakondr.booking.domain.booking.Booking;
import valentinakondr.booking.dto.BookingDto;
import valentinakondr.booking.dto.CreateBookingRequestDto;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    default BookingDto toDto(Booking booking) {
        return new BookingDto(
                booking.getId(),
                booking.getUser().getId(),
                booking.getUser().getUsername(),
                booking.getRoomId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getStatus().name(),
                booking.getCreatedAt()
        );
    }

    BookingDto create(Authentication authentication, CreateBookingRequestDto bookingDto);

    List<BookingDto> findAll(Authentication authentication);

    BookingDto findById(Authentication authentication, UUID id);

    void cancel(Authentication authentication, UUID id);
}
