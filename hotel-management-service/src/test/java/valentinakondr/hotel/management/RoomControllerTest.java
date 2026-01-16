package valentinakondr.hotel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import valentinakondr.hotel.management.controller.api.AvailabilityDto;
import valentinakondr.hotel.management.controller.api.RoomDto;
import valentinakondr.hotel.management.service.RoomService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureMockMvc
class RoomControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean RoomService roomService;

    @MockBean JwtDecoder jwtDecoder;

    private Jwt jwtToken(String role) {

        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("user1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(Map.of("sub", "user1", "role", role)))
                .build();
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor authJwtRole(String role) {
        // jwt() ставит SecurityContext с JwtAuthenticationToken и authorities,
        // поэтому @PreAuthorize(hasRole) будет работать, если дать ROLE_ADMIN/ROLE_USER.
        var jwtObj = jwtToken(role);
        when(jwtDecoder.decode(anyString())).thenReturn(jwtObj);

        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return jwt()
                .jwt(jwtObj)
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority));
    }

    // --------------------
    // POST /api/rooms (ADMIN)
    // --------------------

    @Test
    void createRoom_shouldReturn401_whenNoToken() throws Exception {
        RoomDto req = mock(RoomDto.class);

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(roomService);
    }

    @Test
    void createRoom_shouldReturn403_whenNotAdmin() throws Exception {
        RoomDto req = mock(RoomDto.class);

        mockMvc.perform(post("/api/rooms")
                        .with(authJwtRole("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(roomService);
    }

    @Test
    void createRoom_shouldReturn201_whenAdmin() throws Exception {
        // подстрой под реальный RoomDto конструктор/record
        UUID roomId = UUID.randomUUID();
        UUID hotelId = UUID.randomUUID();

        RoomDto request = new RoomDto(
                null,
                hotelId,
                "101",
                true,
                0
        );

        RoomDto response = new RoomDto(
                roomId,
                hotelId,
                "101",
                true,
                0
        );

        when(roomService.createRoom(any())).thenReturn(response);

        mockMvc.perform(post("/api/rooms")
                        .with(authJwtRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(roomId.toString()))
                .andExpect(jsonPath("$.hotelId").value(hotelId.toString()))
                .andExpect(jsonPath("$.number").value("101"));

        verify(roomService).createRoom(any());
    }

    // --------------------
    // GET /api/rooms (authenticated)
    // --------------------

    @Test
    void getRooms_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(roomService);
    }

    @Test
    void getRooms_shouldReturn200_whenAuthenticated() throws Exception {
        UUID h1 = UUID.randomUUID();
        RoomDto r1 = new RoomDto(UUID.randomUUID(), h1, "101", true, 0);
        RoomDto r2 = new RoomDto(UUID.randomUUID(), h1, "102", true, 2);

        when(roomService.getRooms()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/rooms")
                        .with(authJwtRole("USER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(r1.id().toString()))
                .andExpect(jsonPath("$[0].number").value("101"))
                .andExpect(jsonPath("$[1].id").value(r2.id().toString()))
                .andExpect(jsonPath("$[1].number").value("102"));

        verify(roomService).getRooms();
    }

    // --------------------
    // GET /api/rooms/recommend (authenticated)
    // --------------------

    @Test
    void getRecommendedRooms_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/rooms/recommend"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(roomService);
    }

    @Test
    void getRecommendedRooms_shouldReturn200_whenAuthenticated() throws Exception {
        UUID hotelId = UUID.randomUUID();
        RoomDto r1 = new RoomDto(UUID.randomUUID(), hotelId, "201", true, 5);

        when(roomService.getRecommendedRooms()).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/rooms/recommend")
                        .with(authJwtRole("USER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(r1.id().toString()))
                .andExpect(jsonPath("$[0].number").value("201"));

        verify(roomService).getRecommendedRooms();
    }

    // --------------------
    // POST /api/rooms/{id}/confirm-availability (permitAll)
    // --------------------

    @Test
    void confirmAvailability_shouldReturn204_whenOk_andNoAuthNeeded() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        AvailabilityDto req = new AvailabilityDto(requestId, null, null);

        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(roomService).confirm(eq(roomId), any());
    }

    // --------------------
    // POST /api/rooms/{id}/release?requestId=... (permitAll)
    // --------------------

    @Test
    void release_shouldReturn204_whenOk_andNoAuthNeeded() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        mockMvc.perform(post("/api/rooms/{id}/release", roomId)
                        .param("requestId", requestId.toString()))
                .andExpect(status().isNoContent());

        verify(roomService).release(roomId, requestId);
    }
}