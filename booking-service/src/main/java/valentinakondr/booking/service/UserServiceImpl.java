package valentinakondr.booking.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import valentinakondr.booking.domain.User;
import valentinakondr.booking.dto.AuthDto;
import valentinakondr.booking.dto.TokenDto;
import valentinakondr.booking.dto.UserDto;
import valentinakondr.booking.exception.InvalidRequestException;
import valentinakondr.booking.exception.ResourceAlreadyExistsException;
import valentinakondr.booking.exception.ResourceNotFoundException;
import valentinakondr.booking.repo.UserRepository;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Override
    public TokenDto register(AuthDto authDto) {
        if (authDto == null) {
            log.warn("[auth.register.fail] reason=null_request");
            throw new InvalidRequestException("Request body is null");
        }

        if (authDto.username() == null || authDto.username().isBlank()) {
            log.warn("[auth.register.fail] reason=blank_username");
            throw new InvalidRequestException("Username is null or blank");
        }

        if (authDto.password() == null || authDto.password().isBlank()) {
            log.warn("[auth.register.fail] user={} reason=blank_password", authDto.username());
            throw new InvalidRequestException("Password is null or blank");
        }

        if (userRepo.existsByUsername(authDto.username())) {
            log.info("[auth.register.fail] user={} reason=already_exists", authDto.username());
            throw new ResourceAlreadyExistsException(
                    "User already exists with username: " + authDto.username()
            );
        }

        User user = new User();
        user.setUsername(authDto.username());
        user.setPassword(passwordEncoder.encode(authDto.password()));
        user.setRole("USER");

        User saved = userRepo.save(user);

        log.info("[auth.register.success] userId={} username={} role={}",
                saved.getId(), saved.getUsername(), saved.getRole());

        String token = generateToken(saved.getUsername(), saved.getRole());

        log.debug("[auth.token.issued] username={} purpose=register", saved.getUsername());

        return new TokenDto(token);
    }

    @Override
    public TokenDto login(AuthDto authDto) {
        if (authDto == null) {
            log.warn("[auth.login.fail] reason=null_request");
            throw new InvalidRequestException("Request body is null");
        }

        User user = userRepo.findByUsername(authDto.username())
                .orElseThrow(() -> {
                    log.info("[auth.login.fail] username={} reason=invalid_credentials", authDto.username());
                    return new RuntimeException("Invalid credentials");
                });

        if (!passwordEncoder.matches(authDto.password(), user.getPassword())) {
            log.info("[auth.login.fail] username={} reason=invalid_credentials", user.getUsername());
            throw new RuntimeException("Invalid credentials");
        }

        String token = generateToken(user.getUsername(), user.getRole());

        log.info("[auth.login.success] userId={} username={} role={}",
                user.getId(), user.getUsername(), user.getRole());

        log.debug("[auth.token.issued] username={} purpose=login", user.getUsername());

        return new TokenDto(token);
    }

    @Transactional
    @Override
    public UserDto createUser(UserDto userDto) {
        if (userRepo.existsByUsername(userDto.username())) {
            log.info("[user.create.fail] username={} reason=already_exists", userDto.username());
            throw new ResourceAlreadyExistsException(
                    "User already exists with username: " + userDto.username()
            );
        }

        User userToBeCreated = toDomain(userDto);
        userToBeCreated.setPassword(passwordEncoder.encode(userDto.password()));

        User user = userRepo.save(userToBeCreated);

        log.info("[user.create.success] userId={} username={} role={}",
                user.getId(), user.getUsername(), user.getRole());

        return fromDomain(user);
    }

    @Transactional
    @Override
    public UserDto updateUser(UserDto userDto) {
        if (userDto.id() == null) {
            log.warn("[user.update.fail] reason=null_id");
            throw new InvalidRequestException("User ID must not be null");
        }

        User existingUser = userRepo.findById(userDto.id())
                .orElseThrow(() -> {
                    log.info("[user.update.fail] userId={} reason=not_found", userDto.id());
                    return new ResourceNotFoundException(
                            "User not found with id: " + userDto.id()
                    );
                });

        log.info("[user.update.start] userId={} usernameOld={} usernameNew={}",
                existingUser.getId(), existingUser.getUsername(), userDto.username());

        existingUser.setUsername(userDto.username());

        if (userDto.password() != null && !userDto.password().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userDto.password()));
            log.debug("[user.update.password] userId={} updated=true", existingUser.getId());
        } else {
            log.debug("[user.update.password] userId={} updated=false", existingUser.getId());
        }

        existingUser.setRole(userDto.role());

        User updatedUser = userRepo.save(existingUser);

        log.info("[user.update.success] userId={} username={} role={}",
                updatedUser.getId(), updatedUser.getUsername(), updatedUser.getRole());

        return fromDomain(updatedUser);
    }

    @Transactional
    @Override
    public void deleteUser(UUID id) {
        if (id == null) {
            log.warn("[user.delete.fail] reason=null_id");
            throw new InvalidRequestException("User ID must not be null");
        }

        if (!userRepo.existsById(id)) {
            log.info("[user.delete.fail] userId={} reason=not_found", id);
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        userRepo.deleteById(id);

        log.info("[user.delete.success] userId={}", id);
    }

    public String generateToken(String username, String role) {
        log.debug("[auth.token.generate] username={} role={}", username, role);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }
}