package io.github.ajmiller611.usermanagement.service;

import io.github.ajmiller611.usermanagement.dto.AuthTokensDto;
import io.github.ajmiller611.usermanagement.dto.UserDto;
import io.github.ajmiller611.usermanagement.dto.UserRequestDto;
import io.github.ajmiller611.usermanagement.exception.UserNotFoundException;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import io.github.ajmiller611.usermanagement.security.TokenService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class responsible for user authentication and registration.
 *
 * <p>This service handles the user registration process by delegating the creation
 * of a new user to the {@link UserService}. It also manages the login process by
 * authenticating a user using the {@link AuthenticationManager}, generating a
 * JWT token for successful logins, and returning the necessary response.
 * </p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final UserService userService;
  private final TokenService tokenService;

  /**
   * Authenticates a user and returns a {@link AuthTokensDto} object containing the
   * generated JWT tokens and the user's details if authentication is successful.
   *
   * <p>This method accepts a {@link UserRequestDto} object, which contains the
   * username and password, for the authentication process. If authentication is
   * successful, JWT tokens are generated and returned along with the user's details.
   * </p>
   *
   * @param userRequestDto the data transfer object containing the username and password for login
   * @return a {@link AuthTokensDto} containing the JWT access and refresh tokens as well as
   *     the user's details to be used in the controller
   */
  public AuthTokensDto loginUser(UserRequestDto userRequestDto) {
    logger.info("Login request received with username: {}", userRequestDto.getUsername());
    try {
      // Authenticate the user with the provided username and password.
      Authentication auth = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              userRequestDto.getUsername(),
              userRequestDto.getPassword())
      );
      logger.info("Authentication successful for {}", userRequestDto.getUsername());

      // Generate JWT tokens upon successful authentication
      Map<String, String> tokens = tokenService.generateTokens(auth);

      // Query the database for the user's data
      UserDto userDto = userService.mapToUserDto(
          userRepository.findByUsername(userRequestDto.getUsername())
              .orElseThrow(() -> new UserNotFoundException(
                  String.format("User with username '%s' does not exists.",
                      userRequestDto.getUsername()), "loginUser")
              )
      );

      logger.info("User details returned: {}", userDto);

      // Return a response containing the user and the generated token
      return new AuthTokensDto(
          tokens.get("accessToken"),
          tokens.get("refreshToken"),
          userDto
      );
    } catch (AuthenticationException | UserNotFoundException e) {
      // Return an empty tokens and null user dto in case of authentication failure
      return new AuthTokensDto("", "", null);
    }
  }
}
