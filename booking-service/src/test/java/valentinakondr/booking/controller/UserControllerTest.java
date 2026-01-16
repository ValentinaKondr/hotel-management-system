package valentinakondr.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import valentinakondr.booking.dto.AuthDto;
import valentinakondr.booking.dto.TokenDto;
import valentinakondr.booking.dto.UserDto;
import valentinakondr.booking.exception.InvalidRequestException;
import valentinakondr.booking.exception.ResourceAlreadyExistsException;
import valentinakondr.booking.exception.ResourceNotFoundException;
import valentinakondr.booking.service.UserService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtRole(String role) {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return jwt().authorities(List.of(new SimpleGrantedAuthority(authority)));
    }

    // --------------------
    // POST /user/register (permitAll)
    // --------------------

    @Test
    void register_shouldReturn201_whenOk() throws Exception {
        AuthDto request = new AuthDto("user1", "pass1");
        TokenDto response = new TokenDto("token-123");

        when(userService.register(any())).thenReturn(response);

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-123"));

        verify(userService).register(any());
    }

    @Test
    void register_shouldReturn400_withErrorBody_whenInvalidRequest() throws Exception {
        AuthDto request = new AuthDto("   ", "pass1");

        when(userService.register(any()))
                .thenThrow(new InvalidRequestException("Username is null or blank"));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Username is null or blank"));

        verify(userService).register(any());
    }

    @Test
    void register_shouldReturn409_withErrorBody_whenUserAlreadyExists() throws Exception {
        AuthDto request = new AuthDto("user1", "pass1");

        when(userService.register(any()))
                .thenThrow(new ResourceAlreadyExistsException("User already exists with username: user1"));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User already exists with username: user1"));

        verify(userService).register(any());
    }

    // --------------------
    // POST /user/auth (permitAll)
    // --------------------

    @Test
    void auth_shouldReturn200_whenOk() throws Exception {
        AuthDto request = new AuthDto("user1", "pass1");
        TokenDto response = new TokenDto("token-xyz");

        when(userService.login(any())).thenReturn(response);

        mockMvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-xyz"));

        verify(userService).login(any());
    }

    @Test
    void auth_shouldReturn401_withErrorBody_whenAuthenticationException() throws Exception {
        AuthDto request = new AuthDto("user1", "wrong");

        when(userService.login(any()))
                .thenThrow(new AuthenticationException("Invalid credentials") {});

        mockMvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(userService).login(any());
    }

    @Test
    void auth_shouldReturn400_withErrorBody_whenInvalidRequest() throws Exception {
        AuthDto request = new AuthDto(" ", "pass1");

        when(userService.login(any()))
                .thenThrow(new InvalidRequestException("Username is null or blank"));

        mockMvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Username is null or blank"));

        verify(userService).login(any());
    }

    // --------------------
    // ADMIN endpoints: /user, PATCH /user, DELETE /user/{id}
    // --------------------

    @Test
    void createUser_shouldReturn403_whenNoToken() throws Exception {
        UserDto request = new UserDto(null, "newUser", "pass", "USER");

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(userService);
    }

    @Test
    void createUser_shouldReturn403_whenNotAdmin() throws Exception {
        UserDto request = new UserDto(null, "newUser", "pass", "USER");

        mockMvc.perform(post("/user")
                        .with(jwtRole("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void createUser_shouldReturn200_whenOk() throws Exception {
        UserDto request = new UserDto(null, "newUser", "pass", "USER");
        UserDto response = new UserDto(UUID.randomUUID(), "newUser", null, "USER");

        when(userService.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/user")
                        .with(jwtRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newUser"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userService).createUser(any());
    }

    @Test
    void createUser_shouldReturn409_withErrorBody_whenAlreadyExists() throws Exception {
        UserDto request = new UserDto(null, "newUser", "pass", "USER");

        when(userService.createUser(any()))
                .thenThrow(new ResourceAlreadyExistsException("User already exists with username: newUser"));

        mockMvc.perform(post("/user")
                        .with(jwtRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User already exists with username: newUser"));

        verify(userService).createUser(any());
    }

    @Test
    void updateUser_shouldReturn403_whenNoToken() throws Exception {
        UserDto request = new UserDto(UUID.randomUUID(), "updUser", "newpass", "ADMIN");

        mockMvc.perform(patch("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUser_shouldReturn403_whenNotAdmin() throws Exception {
        UserDto request = new UserDto(UUID.randomUUID(), "updUser", "newpass", "ADMIN");

        mockMvc.perform(patch("/user")
                        .with(jwtRole("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUser_shouldReturn200_whenOk() throws Exception {
        UUID id = UUID.randomUUID();
        UserDto request = new UserDto(id, "updUser", "newpass", "ADMIN");
        UserDto response = new UserDto(id, "updUser", null, "ADMIN");

        when(userService.updateUser(any())).thenReturn(response);

        mockMvc.perform(patch("/user")
                        .with(jwtRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.username").value("updUser"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(userService).updateUser(any());
    }

    @Test
    void updateUser_shouldReturn400_withErrorBody_whenIdNull() throws Exception {
        UserDto request = new UserDto(null, "updUser", "newpass", "ADMIN");

        when(userService.updateUser(any()))
                .thenThrow(new InvalidRequestException("User ID must not be null"));

        mockMvc.perform(patch("/user")
                        .with(jwtRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("User ID must not be null"));

        verify(userService).updateUser(any());
    }

    @Test
    void updateUser_shouldReturn404_withErrorBody_whenUserNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        UserDto request = new UserDto(id, "updUser", "newpass", "ADMIN");

        when(userService.updateUser(any()))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + id));

        mockMvc.perform(patch("/user")
                        .with(jwtRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: " + id));

        verify(userService).updateUser(any());
    }

    @Test
    void deleteUser_shouldReturn403_whenNoToken() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/user/{id}", id))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(userService);
    }

    @Test
    void deleteUser_shouldReturn403_whenNotAdmin() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/user/{id}", id)
                        .with(jwtRole("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void deleteUser_shouldReturn200_whenOk() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/user/{id}", id)
                        .with(jwtRole("ADMIN")))
                .andExpect(status().isOk());

        verify(userService).deleteUser(id);
    }

    @Test
    void deleteUser_shouldReturn404_withErrorBody_whenUserNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("User not found with id: " + id))
                .when(userService)
                .deleteUser(id);

        mockMvc.perform(delete("/user/{id}", id)
                        .with(jwtRole("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: " + id));

        verify(userService).deleteUser(id);
    }

    @Test
    void deleteUser_shouldReturn403_withErrorBody_whenAccessDenied() throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new AccessDeniedException("Access denied"))
                .when(userService)
                .deleteUser(id);

        mockMvc.perform(delete("/user/{id}", id)
                        .with(jwtRole("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access Denied"));

        verify(userService).deleteUser(id);
    }

    @Test
    void register_shouldReturn500_withErrorBody_whenUnexpectedException() throws Exception {
        AuthDto request = new AuthDto("user1", "pass1");

        when(userService.register(any()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("boom"));

        verify(userService).register(any());
    }
}