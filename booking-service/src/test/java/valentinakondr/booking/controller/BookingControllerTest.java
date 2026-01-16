package valentinakondr.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import valentinakondr.booking.dto.BookingDto;
import valentinakondr.booking.dto.CreateBookingRequestDto;
import valentinakondr.booking.exception.InvalidRequestException;
import valentinakondr.booking.exception.ResourceNotFoundException;
import valentinakondr.booking.service.BookingService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    BookingService bookingService;

    // --------------------
    // POST /booking
    // --------------------

    @Test
    void createBooking_shouldReturn4xx_whenNoAuth() throws Exception {
        CreateBookingRequestDto request = new CreateBookingRequestDto(
                UUID.randomUUID(),
                false,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3)
        );

        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createBooking_shouldReturn201_whenOk() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        CreateBookingRequestDto request = new CreateBookingRequestDto(
                roomId,
                false,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3)
        );

        BookingDto response = new BookingDto(
                bookingId,
                userId,
                "user1",
                roomId,
                request.startDate(),
                request.endDate(),
                "CONFIRMED",
                LocalDateTime.now()
        );

        when(bookingService.create(any(), any())).thenReturn(response);

        mockMvc.perform(post("/booking")
                        .with(user("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                .andExpect(jsonPath("$.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.startDate").value(request.startDate().toString()))
                .andExpect(jsonPath("$.endDate").value(request.endDate().toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(bookingService).create(any(), any());
    }

    @Test
    void createBooking_shouldReturn400_withErrorBody_whenInvalidRequest() throws Exception {
        CreateBookingRequestDto request = new CreateBookingRequestDto(
                UUID.randomUUID(),
                false,
                null,
                null
        );

        when(bookingService.create(any(), any()))
                .thenThrow(new InvalidRequestException("Start date or end date is null"));

        mockMvc.perform(post("/booking")
                        .with(user("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Start date or end date is null"));

        verify(bookingService).create(any(), any());
    }

    // --------------------
    // GET /bookings
    // --------------------

    @Test
    void getUserBookings_shouldReturn4xx_whenNoAuth() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getUserBookings_shouldReturn200_whenOk() throws Exception {
        BookingDto b1 = new BookingDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user1",
                UUID.randomUUID(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "CONFIRMED",
                LocalDateTime.now()
        );

        BookingDto b2 = new BookingDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user1",
                UUID.randomUUID(),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(6),
                "CANCELLED",
                LocalDateTime.now()
        );

        when(bookingService.findAll(any())).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/bookings")
                        .with(user("user1")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(b1.id().toString()))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].id").value(b2.id().toString()))
                .andExpect(jsonPath("$[1].status").value("CANCELLED"));

        verify(bookingService).findAll(any());
    }

    // --------------------
    // GET /booking/{id}
    // --------------------

    @Test
    void getBooking_shouldReturn4xx_whenNoAuth() throws Exception {
        mockMvc.perform(get("/booking/{id}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getBooking_shouldReturn200_whenOk() throws Exception {
        UUID bookingId = UUID.randomUUID();

        BookingDto response = new BookingDto(
                bookingId,
                UUID.randomUUID(),
                "user1",
                UUID.randomUUID(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "CONFIRMED",
                LocalDateTime.now()
        );

        when(bookingService.findById(any(), eq(bookingId))).thenReturn(response);

        mockMvc.perform(get("/booking/{id}", bookingId)
                        .with(user("user1")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                .andExpect(jsonPath("$.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(bookingService).findById(any(), eq(bookingId));
    }

    @Test
    void getBooking_shouldReturn404_withErrorBody_whenNotFound() throws Exception {
        UUID bookingId = UUID.randomUUID();

        when(bookingService.findById(any(), eq(bookingId)))
                .thenThrow(new ResourceNotFoundException("Booking not found"));

        mockMvc.perform(get("/booking/{id}", bookingId)
                        .with(user("user1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Booking not found"));

        verify(bookingService).findById(any(), eq(bookingId));
    }

    @Test
    void getBooking_shouldReturn403_withErrorBody_whenAccessDenied() throws Exception {
        UUID bookingId = UUID.randomUUID();

        when(bookingService.findById(any(), eq(bookingId)))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/booking/{id}", bookingId)
                        .with(user("user1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access Denied"));

        verify(bookingService).findById(any(), eq(bookingId));
    }

    // --------------------
    // DELETE /booking/{id}
    // --------------------

    @Test
    void cancelBooking_shouldReturn4xx_whenNoAuth() throws Exception {
        mockMvc.perform(delete("/booking/{id}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void cancelBooking_shouldReturn204_whenOk() throws Exception {
        UUID bookingId = UUID.randomUUID();

        mockMvc.perform(delete("/booking/{id}", bookingId)
                        .with(user("user1")))
                .andExpect(status().isNoContent());

        verify(bookingService).cancel(any(), eq(bookingId));
    }

    @Test
    void cancelBooking_shouldReturn404_withErrorBody_whenNotFound() throws Exception {
        UUID bookingId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Booking not found"))
                .when(bookingService)
                .cancel(any(), eq(bookingId));

        mockMvc.perform(delete("/booking/{id}", bookingId)
                        .with(user("user1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Booking not found"));

        verify(bookingService).cancel(any(), eq(bookingId));
    }

    @Test
    void cancelBooking_shouldReturn403_withErrorBody_whenAccessDenied() throws Exception {
        UUID bookingId = UUID.randomUUID();

        doThrow(new AccessDeniedException("Access denied"))
                .when(bookingService)
                .cancel(any(), eq(bookingId));

        mockMvc.perform(delete("/booking/{id}", bookingId)
                        .with(user("user1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access Denied"));

        verify(bookingService).cancel(any(), eq(bookingId));
    }
}