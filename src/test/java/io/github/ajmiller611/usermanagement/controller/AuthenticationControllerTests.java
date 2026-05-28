package io.github.ajmiller611.usermanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ajmiller611.usermanagement.config.AppConfig;
import io.github.ajmiller611.usermanagement.config.SecurityConfig;
import io.github.ajmiller611.usermanagement.dto.AuthTokensDto;
import io.github.ajmiller611.usermanagement.dto.UserDto;
import io.github.ajmiller611.usermanagement.dto.UserRequestDto;
import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.security.JwtService;
import io.github.ajmiller611.usermanagement.service.AuthenticationService;
import io.github.ajmiller611.usermanagement.service.UserService;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for {@link AuthenticationController}, validating the functionality
 * of authentication-related endpoints, including login flows.
 *
 * <p>This test class covers both successful and error scenarios for each endpoint,
 * ensuring proper response statuses, response structure, and handling of edge cases.
 * </p>
 *
 * <p>Test cases include:
 * <ul>
 *     <li><b>Login Endpoint:</b> Verifies successful login and invalid login attempts.</li>
 *     <li><b>Refresh Token Endpoint:</b> Verifies responses to valid and invalid refresh tokens.
 *     </li>
 * </ul>
 * </p>
 *
 * <p>Uses {@link MockMvc} to simulate HTTP requests and responses, enabling
 * comprehensive validation of controller behavior without needing to start the full application.
 * </p>
 */
@WebMvcTest(AuthenticationController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
class AuthenticationControllerTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private AuthenticationService authenticationService;
  @MockBean private UserService userService;
  @MockBean private JwtService jwtService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;

  LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
  Clock fixedClock =
      Clock.fixed(
          fixedTimestamp.atZone(ZoneId.systemDefault()).toInstant(),
          ZoneId.systemDefault());

  /**
   * Test that the /auth/login endpoint will respond with 200 Ok, access token in Authorization
   * header and refresh token in the form of an HTTP-Only cookie, and the user's details.
   */
  @Test
  void givenValidCredentialsWhenLoginUserThenReturnTokensAndUserDetails() throws Exception {
    Role userRole = new Role("USER");
    UserDto testUser = new UserDto(
        2L,
        "testUser",
        "test@example.com",
        fixedTimestamp,
        Set.of(userRole)
    );

    AuthTokensDto authTokensDto = new AuthTokensDto(
        "access_token_value",
        "refresh_token_value",
        testUser
    );

    when(authenticationService.loginUser(any(UserRequestDto.class))).thenReturn(authTokensDto);

    UserRequestDto requestDto = new UserRequestDto(
        "testUser",
        "password",
        "test@example.com"
    );
    String validJson = objectMapper.writeValueAsString(requestDto);

    try (LogCaptor logCaptor = LogCaptor.forClass(AuthenticationController.class)) {
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(validJson))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer access_token_value"))
          .andExpect(cookie().value("refresh_token", "refresh_token_value"))
          .andExpect(cookie().httpOnly("refresh_token", true))
          .andExpect(cookie().secure("refresh_token", true))
          .andExpect(cookie().maxAge("refresh_token", 7 * 24 * 60 * 60))
          .andExpect(cookie().sameSite("refresh_token", "Lax"))
          .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
          .andExpect(jsonPath("$.username").value(testUser.getUsername()))
          .andExpect(jsonPath("$.email").value(testUser.getEmail()))
          .andExpect(jsonPath("$.roles[0]").value("USER"));

      verify(authenticationService).loginUser(any(UserRequestDto.class));

      System.out.println(logCaptor.getLogs());

      // Verify the log entry of received request
      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Expected logs to include information about the received "
              + "login request but it was missing.")
          .anyMatch(log -> log.contains("Login attempt for username")
              && log.contains(testUser.getUsername()));

      // Verify the log entry of the response
      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Expected logs to include information about the login "
              + "response but it was missing.")
          .anyMatch(log -> log.contains("Endpoint /auth/login response:")
              && log.contains(testUser.getUserId().toString())
              && log.contains(testUser.getUsername())
              && log.contains(testUser.getEmail()));
    }
  }

  /**
   * Test that the /auth/login endpoint will respond with 401 Unauthorized when invalid credentials
   * are received.
   */
  @Test
  void givenInvalidCredentialsWhenLoginUserThenReturnUnauthorizedResponse() throws Exception {
    AuthTokensDto nullAuthTokensDto = new AuthTokensDto(null, null, null);
    when(authenticationService.loginUser(any(UserRequestDto.class))).thenReturn(nullAuthTokensDto);

    UserRequestDto requestDto = new UserRequestDto(
        "invalidUser",
        "wrongPassword",
        "test@example.com"
    );
    String validJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson))
        .andExpect(status().isUnauthorized());

    verify(authenticationService).loginUser(any(UserRequestDto.class));

    // Reset the configurations of the Mockito.when method
    reset(authenticationService);

    AuthTokensDto emptyAuthTokensDto = new AuthTokensDto("", "", null);
    when(authenticationService.loginUser(any(UserRequestDto.class))).thenReturn(emptyAuthTokensDto);

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validJson))
        .andExpect(status().isUnauthorized());

    verify(authenticationService).loginUser(any(UserRequestDto.class));
  }

  /**
   * Test that the /auth/refresh-token endpoint respond with 200 Ok with the access token in the
   * Authorization header and refresh token in the form of an HTTP-Only cookie.
   */
  @Test
  void givenValidRefreshTokenWhenRefreshTokenThenReturnOkResponse() throws Exception {
    Role role = new Role("USER");

    UserDto userDto = new UserDto(
        2L,
        "testUser",
        "test@example.com",
        fixedTimestamp,
        Set.of(role)
    );
    Cookie validRefreshTokenCookie = new Cookie("refresh_token", "validToken");

    Jwt validJwt = mock(Jwt.class);
    when(validJwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(60 * 60));
    when(validJwt.getSubject()).thenReturn("testUser");

    when(jwtDecoder.decode("validToken")).thenReturn(validJwt);
    when(jwtService.jwtDecoder()).thenReturn(jwtDecoder);

    when(userService.getUserByUsername("testUser"))
        .thenReturn(userDto);

    when(jwtService.generateAccessToken(any(Authentication.class)))
        .thenReturn("newAccessToken");
    when(jwtService.generateRefreshToken(any(Authentication.class)))
        .thenReturn("newRefreshToken");

    mockMvc.perform(post("/auth/refresh-token")
        .cookie(validRefreshTokenCookie))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer newAccessToken"))
        .andExpect(cookie().value("refresh_token", "newRefreshToken"))
        .andExpect(cookie().httpOnly("refresh_token", true))
        .andExpect(cookie().secure("refresh_token", true))
        .andExpect(cookie().maxAge("refresh_token", 7 * 24 * 60 * 60))
        .andExpect(cookie().sameSite("refresh_token", "Lax"));
  }

  /**
   * Test that the /auth/refresh-token endpoint respond with 400 Bad Request when
   * missing the refresh token.
   */
  @Test
  void givenMissingRefreshTokenWhenRefreshTokenThenReturnBadRequest() throws Exception {
    mockMvc.perform(post("/auth/refresh-token"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Refresh Token is missing"));
  }

  /**
   * Test that the /auth/refresh-token endpoint responds with 403 Forbidden when
   * expired refresh token is received.
   */
  @Test
  void givenExpiredRefreshTokenWhenRefreshTokenThenReturnForbiddenResponse() throws Exception {
    Cookie expiredRefreshTokenCookie = new Cookie("refresh_token", "expiredToken");

    Jwt expiredJwt = mock(Jwt.class);
    when(expiredJwt.getExpiresAt()).thenReturn(Instant.now().minusSeconds(60));
    when(expiredJwt.getSubject()).thenReturn("testUser");

    when(jwtDecoder.decode("expiredToken")).thenReturn(expiredJwt);
    when(jwtService.jwtDecoder()).thenReturn(jwtDecoder);

    mockMvc.perform(post("/auth/refresh-token")
        .cookie(expiredRefreshTokenCookie))
        .andExpect(status().isForbidden())
        .andExpect(content().string("Refresh token has expired"));
  }

  /**
   * Test that the /auth/refresh-token endpoint responds with 400 Bad Request when
   * invalid refresh token is received.
   */
  @Test
  void givenInvalidRefreshTokenWhenRefreshTokenThenReturnBadRequest() throws Exception {
    Cookie invalidRefreshTokenCookie = new Cookie("refresh_token", "invalidToken");
    when(jwtDecoder.decode("invalidToken")).thenThrow(new JwtException("Invalid token"));
    when(jwtService.jwtDecoder()).thenReturn(jwtDecoder);

    mockMvc.perform(post("/auth/refresh-token")
            .cookie(invalidRefreshTokenCookie))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Invalid token"));
  }

  /**
   * Test that the /auth/me/ endpoint responds with the current authenticated user details.
   */
  @Test
  @WithMockUser(username = "testUser", roles = {"USER"})
  void givenAuthenticatedUserWhenGetCurrentUserThenReturnUserDetails() throws Exception {
    Role role = new Role("USER");

    UserDto userDto = new UserDto(
        1L,
        "testUser",
        "test@example.com",
        fixedTimestamp,
        Set.of(role)
    );

    when(userService.getUserByUsername("testUser")).thenReturn(userDto);

    mockMvc.perform(get("/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("testUser"))
        .andExpect(jsonPath("$.email").value("test@example.com"));

  }

  /**
   * Test that the /auth/me endpoint responds with 401 Unauthorized
   * when no authenticated user is present.
   *
   */
  @Test
  void givenNoAuthenticationWhenGetCurrentUserThenReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/auth/me"))
        .andExpect(status().isUnauthorized());
  }

  /**
   * Test that an authenticated user calling /auth/logout
   * clears the refresh token cookie and returns 200 OK with success message.
   */
  @Test
  @WithMockUser(username = "testUser", roles = "USER")
  void givenAuthenticatedUserWhenLogoutThenRefreshTokenCookieCleared() throws Exception {
    mockMvc.perform(post("/auth/logout"))
        .andExpect(status().isOk())
        .andExpect(content().string("Logged out successfully"))
        .andExpect(cookie().maxAge("refresh_token", 0))
        .andExpect(cookie().httpOnly("refresh_token", true))
        .andExpect(cookie().secure("refresh_token", true));
  }

  /**
   * Test that calling /auth/logout without authentication
   * still returns 200 OK and clears the refresh token cookie.
   */
  @Test
  void givenNoAuthenticationWhenLogoutThenReturnOk() throws Exception {
    mockMvc.perform(post("/auth/logout"))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge("refresh_token", 0));
  }
}
