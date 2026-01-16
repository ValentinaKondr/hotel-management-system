package valentinakondr.booking.service;

import valentinakondr.booking.domain.User;
import valentinakondr.booking.dto.AuthDto;
import valentinakondr.booking.dto.TokenDto;
import valentinakondr.booking.dto.UserDto;

import java.util.UUID;

public interface UserService {
    default UserDto fromDomain(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                "USER"
        );
    }

    default User toDomain(UserDto dto) {
        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(dto.password());
        user.setRole(dto.role());
        return user;
    }

    TokenDto register(AuthDto userDto);

    TokenDto login(AuthDto userDto);

    UserDto createUser(UserDto userDto);

    UserDto updateUser(UserDto userDto);

    void deleteUser(UUID id);
}
