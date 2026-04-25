package io.github.ajmiller611.usermanagement.dto;

import io.github.ajmiller611.usermanagement.controller.AuthenticationController;
import io.github.ajmiller611.usermanagement.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) representing authentication tokens and user information.
 *
 * <p>This DTO is used to encapsulate the authentication tokens and relevant user details
 * that are returned to the {@link AuthenticationController}.
 * Includes an access token for API requests, a refresh token for obtaining new access
 * tokens, and details about the authenticated user.
 * </p>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthTokensDto {

  /**
   * The access token JWT (JSON Web Token) issued after successful authentication.
   * This token is used for subsequent API requests to authenticate the user.
   */
  private String accessToken;

  /**
   * The refresh token JWT (JSON Web Token) issued after successful authentication.
   * This token is used to obtain a new access token when the current access token has expired.
   * It is mainly used when the client requests a new access token from the server without the need
   * to re-authenticate the user.
   */
  private String refreshToken;

  /**
   * The {@link User} details to be returned by the
   * service layer and used in the response body by the controller.
   */
  private UserDto userDto;

}
