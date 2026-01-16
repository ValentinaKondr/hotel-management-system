package valentinakondr.hotel.management.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import valentinakondr.hotel.management.controller.api.AvailabilityDto;
import valentinakondr.hotel.management.controller.api.RoomDto;
import valentinakondr.hotel.management.dao.HotelRepository;
import valentinakondr.hotel.management.dao.IdempotencyRequestRepository;
import valentinakondr.hotel.management.dao.RoomRepository;
import valentinakondr.hotel.management.domain.Hotel;
import valentinakondr.hotel.management.domain.IdempotencyRequest;
import valentinakondr.hotel.management.domain.Room;
import valentinakondr.hotel.management.service.RoomService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final IdempotencyRequestRepository idempotencyRequestRepository;

    @Transactional
    @Override
    public RoomDto createRoom(RoomDto dto) {
        Hotel hotel = hotelRepository.findById(dto.hotelId())
                .orElseThrow(() -> new RuntimeException("Hotel not found"));

        Room room = Room.fromDto(hotel, dto, 0);
        Room saved = roomRepository.save(room);

        log.info("[room.create] roomId={} hotelId={} number={}",
                saved.getId(), hotel.getId(), saved.getNumber());

        return new RoomDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public List<RoomDto> getRooms() {
        log.debug("[room.list] filtering available=true");
        return roomRepository.findByAvailableTrue().stream()
                .map(RoomDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<RoomDto> getRecommendedRooms() {
        log.debug("[room.recommended.list] start");
        return roomRepository.findAvailableRoomsRecommended().stream()
                .map(RoomDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void confirm(UUID roomId, AvailabilityDto request) {
        UUID requestId = request.requestId();

        log.info("[room.confirm.start] roomId={} requestId={}", roomId, requestId);

        // 1) Idempotency lock (DB)
        try {
            idempotencyRequestRepository.save(
                    IdempotencyRequest.builder()
                            .requestId(requestId)
                            .roomId(roomId)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            log.debug("[room.confirm.skip] roomId={} requestId={} reason=already_processed",
                    roomId, requestId);
            return;
        }

        // 2) Business logic
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.warn("[room.confirm.fail] roomId={} requestId={} reason=room_not_found",
                            roomId, requestId);
                    return new RuntimeException("Room not found");
                });

        if (!room.getAvailable()) {
            idempotencyRequestRepository.deleteByRequestId(requestId);

            log.warn("[room.confirm.fail] roomId={} requestId={} reason=not_available",
                    roomId, requestId);

            throw new RuntimeException("Room is not available");
        }

        int before = room.getTimesBooked();
        room.setTimesBooked(before + 1);
        roomRepository.save(room);

        log.info("[room.confirm.success] roomId={} requestId={} timesBookedBefore={} timesBookedAfter={}",
                roomId, requestId, before, room.getTimesBooked());
    }

    @Transactional
    @Override
    public void release(UUID roomId, UUID rqUid) {
        log.info("[room.release.start] roomId={} requestId={}", roomId, rqUid);

        var idem = idempotencyRequestRepository.findByRequestId(rqUid);
        if (idem.isEmpty()) {
            log.debug("[room.release.skip] roomId={} requestId={} reason=request_not_found",
                    roomId, rqUid);
            return;
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.warn("[room.release.fail] roomId={} requestId={} reason=room_not_found",
                            roomId, rqUid);
                    return new RuntimeException("Room not found");
                });

        int before = room.getTimesBooked();

        if (before > 0) {
            room.setTimesBooked(before - 1);
            roomRepository.save(room);

            log.info("[room.release.success] roomId={} requestId={} timesBookedBefore={} timesBookedAfter={}",
                    roomId, rqUid, before, room.getTimesBooked());
        } else {
            log.debug("[room.release.skip] roomId={} requestId={} reason=timesBooked_already_zero",
                    roomId, rqUid);
        }

        idempotencyRequestRepository.deleteByRequestId(rqUid);
    }
}