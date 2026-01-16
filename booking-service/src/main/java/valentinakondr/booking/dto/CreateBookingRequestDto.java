package valentinakondr.booking.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequestDto(
        UUID roomId,
        Boolean autoSelect,
        LocalDate startDate,
        LocalDate endDate
) {
}
