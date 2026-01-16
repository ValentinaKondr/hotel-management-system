package valentinakondr.booking.dto;

import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String password,
        String role
) {
}