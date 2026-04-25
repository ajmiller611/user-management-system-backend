package io.github.ajmiller611.usermanagement.dto;

import io.github.ajmiller611.usermanagement.model.Role;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing a user.
 *
 * <p>This DTO is used to encapsulate user information such as user ID, username, email,
 * account creation time, and assigned roles (authorities).
 *
 * <p>Used to transfer user data, without sensitive information, from the backend to the client
 * in a structured format.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

  private Long userId;
  private String username;
  private String email;
  private LocalDateTime createdAt;
  private Set<Role> authorities;
}
