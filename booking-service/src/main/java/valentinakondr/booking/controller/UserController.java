package valentinakondr.booking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import valentinakondr.booking.dto.AuthDto;
import valentinakondr.booking.dto.TokenDto;
import valentinakondr.booking.dto.UserDto;
import valentinakondr.booking.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenDto register(@RequestBody AuthDto request) {
        return userService.register(request);
    }

    @PostMapping("/auth")
    public TokenDto authenticate(@RequestBody AuthDto request) {
        return userService.login(request);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto createUser(@RequestBody UserDto userDto) {
        return userService.createUser(userDto);
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUser(@RequestBody UserDto userDto) {
        return userService.updateUser(userDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

}
