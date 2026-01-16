package valentinakondr.booking.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import valentinakondr.booking.client.AvailabilityRequestDto;
import valentinakondr.booking.client.HotelClient;
import valentinakondr.booking.client.RoomDto;
import valentinakondr.booking.domain.User;
import valentinakondr.booking.domain.booking.Booking;
import valentinakondr.booking.domain.booking.BookingStatus;
import valentinakondr.booking.dto.BookingDto;
import valentinakondr.booking.dto.CreateBookingRequestDto;
import valentinakondr.booking.exception.InvalidRequestException;
import valentinakondr.booking.exception.ResourceNotFoundException;
import valentinakondr.booking.repo.BookingRepository;
import valentinakondr.booking.repo.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final HotelClient hotel;
    private final UserRepository userRepo;
    private final BookingRepository bookingRepo;

    @Override
    public BookingDto create(Authentication authentication, CreateBookingRequestDto bookingDto) {
        if (bookingDto == null) {
            log.warn("[booking.create.fail] reason=null_request user={}", authentication != null ? authentication.getName() : "unknown");
            throw new InvalidRequestException("CreateBookingDto is null");
        }
        if (bookingDto.startDate() == null || bookingDto.endDate() == null) {
            log.warn("[booking.create.fail] reason=missing_dates user={} startDate={} endDate={}",
                    authentication.getName(), bookingDto.startDate(), bookingDto.endDate());
            throw new InvalidRequestException("Start date or end date is null");
        }
        if (bookingDto.startDate().isAfter(bookingDto.endDate())) {
            log.warn("[booking.create.fail] reason=start_after_end user={} startDate={} endDate={}",
                    authentication.getName(), bookingDto.startDate(), bookingDto.endDate());
            throw new InvalidRequestException("Start date must be before end date");
        }

        String username = authentication.getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[booking.create.fail] reason=user_not_found user={}", username);
                    return new ResourceNotFoundException("User not found");
                });

        UUID roomId = bookingDto.roomId();
        if (bookingDto.autoSelect()) {
            log.debug("[booking.create] user={} autoSelect=true", username);
            roomId = autoSelect();
        }

        if (roomId == null) {
            log.info("[booking.create.fail] user={} reason=no_available_rooms", username);
            throw new ResourceNotFoundException("No available rooms");
        }

        Booking createdBooking = bookingRepo.save(newBooking(user, bookingDto.startDate(), bookingDto.endDate(), roomId));

        log.info("[booking.create.success] bookingId={} requestId={} user={} roomId={} status={}",
                createdBooking.getId(), createdBooking.getRequestId(), username, createdBooking.getRoomId(), createdBooking.getStatus());

        try {
            confirm(createdBooking);

            createdBooking.setStatus(BookingStatus.CONFIRMED);
            bookingRepo.save(createdBooking);

            log.info("[booking.confirm.success] bookingId={} requestId={} user={} roomId={}",
                    createdBooking.getId(), createdBooking.getRequestId(), username, createdBooking.getRoomId());

        } catch (Exception e) {
            log.error("[booking.confirm.fail] bookingId={} requestId={} user={} roomId={} error={}",
                    createdBooking.getId(), createdBooking.getRequestId(), username, createdBooking.getRoomId(), e.getMessage(), e);

            releaseRoom(createdBooking);

            createdBooking.setStatus(BookingStatus.CANCELLED);
            bookingRepo.save(createdBooking);

            log.info("[booking.cancel.auto] bookingId={} requestId={} user={} reason=confirm_failed",
                    createdBooking.getId(), createdBooking.getRequestId(), username);
        }

        return this.toDto(createdBooking);
    }

    @Override
    public List<BookingDto> findAll(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[booking.list.fail] reason=user_not_found user={}", username);
                    return new ResourceNotFoundException("User not found");
                });

        log.debug("[booking.list] user={} sort=createdAt_desc", username);

        return bookingRepo.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public BookingDto findById(Authentication authentication, UUID id) {
        String username = authentication.getName();

        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> {
                    log.info("[booking.get.fail] bookingId={} user={} reason=not_found", id, username);
                    return new RuntimeException("Booking not found");
                });

        if (!booking.getUser().getUsername().equals(username)) {
            log.warn("[booking.get.fail] bookingId={} user={} reason=access_denied owner={}",
                    id, username, booking.getUser().getUsername());
            throw new RuntimeException("Access denied");
        }

        log.debug("[booking.get] bookingId={} user={} status={}", id, username, booking.getStatus());

        return toDto(booking);
    }

    @Override
    public void cancel(Authentication authentication, UUID id) {
        String username = authentication.getName();

        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> {
                    log.info("[booking.cancel.fail] bookingId={} user={} reason=not_found", id, username);
                    return new ResourceNotFoundException("Booking not found");
                });

        if (!booking.getUser().getUsername().equals(username)) {
            log.warn("[booking.cancel.fail] bookingId={} user={} reason=access_denied owner={}",
                    id, username, booking.getUser().getUsername());
            throw new AccessDeniedException("Access denied");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            log.warn("[booking.cancel.fail] bookingId={} user={} reason=invalid_status status={}",
                    id, username, booking.getStatus());
            throw new InvalidRequestException("Only confirmed bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        releaseRoom(booking);

        log.info("[booking.cancel.success] bookingId={} requestId={} user={} roomId={}",
                booking.getId(), booking.getRequestId(), username, booking.getRoomId());
    }

    @Retryable(
            retryFor = {FeignException.class, Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public void confirm(Booking booking) {
        // NB: Spring Retry сам залогирует "attempt" не будет, поэтому делаем понятный лог на каждый заход
        log.info("[booking.confirm.start] bookingId={} requestId={} roomId={} startDate={} endDate={}",
                booking.getId(), booking.getRequestId(), booking.getRoomId(), booking.getStartDate(), booking.getEndDate());

        AvailabilityRequestDto request = new AvailabilityRequestDto(
                booking.getRequestId(),
                booking.getStartDate(),
                booking.getEndDate()
        );

        hotel.confirm(booking.getRoomId(), request);

        log.info("[booking.confirm.sent] bookingId={} requestId={} roomId={}",
                booking.getId(), booking.getRequestId(), booking.getRoomId());
    }

    private UUID selectRoomAutomatically() {
        List<RoomDto> rooms = hotel.getRooms();
        if (rooms.isEmpty()) {
            log.debug("[booking.room.select.fail] reason=no_rooms");
            return null;
        }
        UUID selected = rooms.get(0).id();
        log.debug("[booking.room.select] selectedRoomId={} strategy=first_available", selected);
        return selected;
    }

    private void releaseRoom(Booking booking) {
        try {
            hotel.release(booking.getRoomId(), booking.getRequestId());
            log.info("[booking.release.success] bookingId={} requestId={} roomId={}",
                    booking.getId(), booking.getRequestId(), booking.getRoomId());
        } catch (Exception e) {
            log.error("[booking.release.fail] bookingId={} requestId={} roomId={} error={}",
                    booking.getId(), booking.getRequestId(), booking.getRoomId(), e.getMessage(), e);
        }
    }

    public UUID autoSelect() {
        List<RoomDto> rooms = hotel.getRooms();
        if (rooms.isEmpty()) {
            log.info("[booking.autoSelect.fail] reason=no_rooms");
            throw new ResourceNotFoundException("No rooms");
        }
        UUID selected = rooms.get(0).id();
        log.debug("[booking.autoSelect] selectedRoomId={} strategy=first_available", selected);
        return selected;
    }

    public static Booking newBooking(User user, LocalDate startDate, LocalDate endDate, UUID roomId) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setRoomId(roomId);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setRequestId(UUID.randomUUID());
        return booking;
    }
}
