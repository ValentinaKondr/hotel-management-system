package valentinakondr.hotel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import valentinakondr.hotel.management.controller.api.HotelDto;
import valentinakondr.hotel.management.service.HotelService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HotelService hotelService;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtRole(String role) {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return jwt().authorities(List.of(new SimpleGrantedAuthority(authority)));
    }

    // --------------------
    // POST /api/hotels (ADMIN)
    // --------------------

    @Test
    void createHotel_shouldReturn403_whenNoToken() throws Exception {
        HotelDto request = new HotelDto(null, "Hotel One", "London");

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(hotelService);
    }

    @Test
    void createHotel_shouldReturn403_whenNotAdmin() throws Exception {
        HotelDto request = new HotelDto(null, "Hotel One", "London");

        mockMvc.perform(post("/api/hotels")
                        .with(jwtRole("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(hotelService);
    }

    @Test
    void createHotel_shouldReturn201_whenAdmin_andOk() throws Exception {
        HotelDto request = new HotelDto(null, "Hotel One", "London");
        UUID id = UUID.randomUUID();
        HotelDto response = new HotelDto(id, "Hotel One", "London");

        when(hotelService.createHotel(any())).thenReturn(response);

        mockMvc.perform(post("/api/hotels")
                        .with(jwtRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Hotel One"))
                .andExpect(jsonPath("$.address").value("London"));

        verify(hotelService).createHotel(any());
    }

    @Test
    void createHotel_shouldReturn500_withErrorBody_whenAdmin_andUnexpectedException() throws Exception {
        HotelDto request = new HotelDto(null, "Hotel One", "London");

        when(hotelService.createHotel(any()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/hotels")
                        .with(jwtRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("boom"));

        verify(hotelService).createHotel(any());
    }

    // --------------------
    // GET /api/hotels (public)
    // --------------------

    @Test
    void getAllHotels_shouldReturn200_whenOk() throws Exception {
        HotelDto h1 = new HotelDto(UUID.randomUUID(), "H1", "Addr1");
        HotelDto h2 = new HotelDto(UUID.randomUUID(), "H2", "Addr2");

        when(hotelService.getAllHotels()).thenReturn(List.of(h1, h2));

        mockMvc.perform(get("/api/hotels")
                        .with(jwtRole("USER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(h1.id().toString()))
                .andExpect(jsonPath("$[0].name").value("H1"))
                .andExpect(jsonPath("$[0].address").value("Addr1"))
                .andExpect(jsonPath("$[1].id").value(h2.id().toString()))
                .andExpect(jsonPath("$[1].name").value("H2"))
                .andExpect(jsonPath("$[1].address").value("Addr2"));

        verify(hotelService).getAllHotels();
    }
}