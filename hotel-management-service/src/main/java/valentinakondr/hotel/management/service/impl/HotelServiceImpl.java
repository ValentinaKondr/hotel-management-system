package valentinakondr.hotel.management.service.impl;

import valentinakondr.hotel.management.controller.api.HotelDto;
import valentinakondr.hotel.management.dao.HotelRepository;
import valentinakondr.hotel.management.domain.Hotel;
import valentinakondr.hotel.management.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;

    @Transactional
    @Override
    public HotelDto createHotel(HotelDto dto) {
        log.info("[hotel.create.start] name={}", dto.name());

        Hotel hotel = Hotel.fromDto(dto);
        Hotel saved = hotelRepository.save(hotel);

        log.info("[hotel.create.success] hotelId={} name={}",
                saved.getId(), saved.getName());

        return new HotelDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public List<HotelDto> getAllHotels() {
        log.debug("[hotel.list] start");
        return hotelRepository.findAll().stream()
                .map(HotelDto::new)
                .collect(Collectors.toList());
    }
}