package io.github.ajmiller611.usermanagement.controller;

import io.github.ajmiller611.usermanagement.dto.AuthTokensDto;
import io.github.ajmiller611.usermanagement.dto.LoginResponseDto;
import io.github.ajmiller611.usermanagement.dto.RegistrationRequestDto;
import io.github.ajmiller611.usermanagement.dto.UserDto;
import io.github.ajmiller611.usermanagement.dto.UserRequestDto;
import io.github.ajmiller611.usermanagement.dto.UserResponseDto;
import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.security.JwtService;
import io.github.ajmiller611.usermanagement.service.AuthenticationService;
import io.github.ajmiller611.usermanagement.service.InvitationService;
import io.github.ajmiller611.usermanagement.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller responsible for user authentication and refresh tokens endpoints.
 *
 * <p>This controller exposes two endpoints:
 * - POST /auth/login: Allows a user to login using their credentials in the form of a
 *   {@link UserRequestDto}.
 * - POST /refresh-token: Refreshes the authentication tokens for a user.
 * </p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final AuthenticationService authenticationService;
  private final InvitationService invitationService;
  private final JwtService jwtService;
  private final UserService userService;

  public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

  /**
   * Handles user login authentication.
   *
   * <p>This method accepts a {@link UserRequestDto} containing the user's credentials
   * (username and password) and calls the {@link AuthenticationService} to authenticate the user.
   * If successful, a {@link LoginResponseDto} is returned with the user details,
   * a JWT access token in the Authorization Header, and refresh token in an HTTP-Only cookie.
   * </p>
   *
   * @param body the login data transfer object containing the user's username and password
   * @return a {@link LoginResponseDto} containing the user details and the generated JWT token
   */
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> loginUser(
      @RequestBody UserRequestDto body,
      HttpServletResponse response) {

    logger.info("Login attempt for username={}", body.getUsername());

    AuthTokensDto authTokensDto = authenticationService.loginUser(body);

    if (authTokensDto.getAccessToken() != null && !authTokensDto.getAccessToken().isEmpty())  {
      response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authTokensDto.getAccessToken());

      // Refresh token cookie set to HTTP-only, secure, and SameSite=Lax
      // to reduce CSRF attack surface.
      ResponseCookie refreshCookie =
          ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, authTokensDto.getRefreshToken())
              .httpOnly(true)
              .secure(true)
              .path("/")
              .maxAge(Duration.ofDays(7))
              .sameSite("Lax")
              .build();
      response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

      List<String> roles = authTokensDto.getUserDto().getAuthorities()
          .stream()
          .map(Role::getAuthority)
          .toList();

      LoginResponseDto loginResponseDto = new LoginResponseDto(
          authTokensDto.getUserDto().getUserId(),
          authTokensDto.getUserDto().getUsername(),
          authTokensDto.getUserDto().getEmail(),
          roles
      );

      logger.info("Endpoint /auth/login response: {}", loginResponseDto);
      return ResponseEntity.ok(loginResponseDto);
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  /**
   * Refreshes the authentication tokens for a user by validating and processing a refresh token.
   *
   * <p>This method extracts the refresh token from the request cookies, verifies its validity, and
   * generates new access and refresh tokens. The new access token is added to the
   * Authorization header and the new refresh token is added as an HTTP-Only cookie to the response
   * for the client to use in subsequent requests.</p>
   *
   * <p>If the refresh token is missing, expired, or invalid, an appropriate error response is
   * returned.</p>
   *
   * @param request the {@link HttpServletRequest} object containing the client's request data,
   *                including cookies.
   * @param response the {@link HttpServletResponse} object used to send the response back to the
   *                 client, including setting the header and cookie with the new tokens.
   * @return a {@link ResponseEntity} indicating the result of the operation. A successful operation
   *         returns HTTP 200 with no body, while errors return HTTP 400 (Bad Request) or HTTP 403
   *         (Forbidden) with an error message.
   */
  @PostMapping("/refresh-token")
  public ResponseEntity<String> refreshToken(
      HttpServletRequest request,
      HttpServletResponse response) {

    try {
      String refreshToken = null;
      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
            refreshToken = cookie.getValue();
            break;
          }
        }
      }

      if (refreshToken == null) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body("Refresh Token is missing");
      }

      // Decode the refresh token to extract claims
      Jwt decodedJwt = jwtService.jwtDecoder().decode(refreshToken);

      // Extract the username or subject from the refresh token
      String username = decodedJwt.getSubject();

      // Check if the refresh token is expired or invalid
      if (decodedJwt.getExpiresAt() == null || decodedJwt.getExpiresAt().isBefore(Instant.now())) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body("Refresh token has expired");
      }

      UserDto userDto = userService.getUserByUsername(username);

      // Generate a new access token (JWT) for the user
      Authentication auth = new UsernamePasswordAuthenticationToken(
          username,
          null,
          userDto.getAuthorities()
      );
      String newAccessToken = jwtService.generateAccessToken(auth);
      String newRefreshToken = jwtService.generateRefreshToken(auth);

      response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken);

      // Create the refresh token cookie
      ResponseCookie refreshCookie =
          ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, newRefreshToken)
              .httpOnly(true)
              .secure(true)
              .path("/")
              .maxAge(Duration.ofDays(7))
              .sameSite("Lax")
              .build();

      response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

      // Return 200 ok
      return ResponseEntity.ok().build();
    } catch (JwtException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token");
    }
  }

  /**
   * Retrieves the currently authenticated user's information.
   *
   * <p>This endpoint returns basic details about the user associated with the
   * current authentication context. The user is identified from the
   * {@link Authentication} object populated by Spring Security after validating
   * the JWT access token provided in the request.</p>
   *
   * @param authentication the {@link Authentication} object representing the
   *                       currently authenticated user, injected by Spring Security
   * @return a {@link LoginResponseDto} containing the authenticated user's
   *         details and roles
   */
  @GetMapping("/me")
  public ResponseEntity<LoginResponseDto> getCurrentUser(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = authentication.getName();

    UserDto userDto = userService.getUserByUsername(username);

    List<String> roles = userDto.getAuthorities()
        .stream()
        .map(Role::getAuthority)
        .toList();

    LoginResponseDto response = new LoginResponseDto(
        userDto.getUserId(),
        userDto.getUsername(),
        userDto.getEmail(),
        roles
    );

    return  ResponseEntity.ok(response);
  }

  /**
   * Logs out the currently authenticated user.
   *
   * <p>This clears the refresh token cookie so it cannot be used for future refresh requests.
   * Access token is held in the frontend memory and should be cleared by the client.</p>
   *
   * @param authentication the current authenticated user
   * @param response the HttpServletResponse to clear the cookie
   * @return 200 OK if logout was successful
   */
  @PostMapping("/logout")
  public ResponseEntity<String> logout(
      Authentication authentication,
      HttpServletResponse response
  ) {
    if (authentication != null && authentication.isAuthenticated()) {
      logger.info("User {} logging out", authentication.getName());
    }

    Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setSecure(true);
    refreshTokenCookie.setPath("/");
    refreshTokenCookie.setMaxAge(0);
    response.addCookie(refreshTokenCookie);

    return ResponseEntity.ok("Logged out successfully");
  }

  /**
   * Completes user registration using an invitation token.
   *
   * <p>This endpoint validates the invitation token, creates a new user with the
   * provided username and password, and returns the created user details.</p>
   *
   * @param requestDto the {@link RegistrationRequestDto} containing token, username, and password
   * @return a {@link UserResponseDto} containing the created user's details
   */
  @Transactional
  @PostMapping("/register/invitation")
  public ResponseEntity<UserResponseDto> completeRegistration(
      @Valid @RequestBody RegistrationRequestDto requestDto) {
    logger.info("Endpoint /register/invitation received POST request: {}", requestDto);

    UserDto userDto = authenticationService.completeRegistration(requestDto);

    URI location = ServletUriComponentsBuilder
        .fromCurrentContextPath()
        .path("/users/{id}")
        .buildAndExpand(userDto.getUserId())
        .toUri();

    UserResponseDto responseDto = new UserResponseDto(
        userDto.getUserId(),
        userDto.getUsername(),
        userDto.getEmail()
    );

    ResponseEntity<UserResponseDto> response =
        ResponseEntity.created(location).body(responseDto);

    logger.info("Endpoint /register/invitation response: {}", response);
    return response;
  }
}
