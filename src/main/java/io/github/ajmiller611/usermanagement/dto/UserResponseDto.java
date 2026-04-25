package io.github.ajmiller611.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for representing user information in responses.
 *
 * <p>This DTO is used to encapsulate user-related data that is returned to the client,
 * providing only essential details such as user ID, username, and email.
 * </p>
 *
 * <p><strong>Security Note:</strong> This DTO deliberately omits sensitive information such as
 * the user's password and authorities to prevent exposure of this data in responses.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {

  private Long userId;
  private String username;
  private String email;
}
