package valentinakondr.booking.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingDto(
        UUID id,
        UUID userId,
        String username,
        UUID roomId,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime createdAt
) {
}
