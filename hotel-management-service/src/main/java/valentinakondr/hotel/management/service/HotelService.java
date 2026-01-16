package valentinakondr.hotel.management.service;

import valentinakondr.hotel.management.controller.api.HotelDto;

import java.util.List;

public interface HotelService {
    HotelDto createHotel(HotelDto hotelDto);
    List<HotelDto> getAllHotels();
}