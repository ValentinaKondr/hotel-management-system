package valentinakondr.booking.client;

import java.time.LocalDate;
import java.util.UUID;

public record AvailabilityRequestDto(
        UUID requestId,
        LocalDate startDate,
        LocalDate endDate
) {
}
