package valentinakondr.hotel.management.controller.api;

import java.time.LocalDate;
import java.util.UUID;

public record AvailabilityDto(
        UUID requestId,
        LocalDate startDate,
        LocalDate endDate
) {
}
