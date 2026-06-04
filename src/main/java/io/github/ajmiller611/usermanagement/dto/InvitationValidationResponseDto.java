package io.github.ajmiller611.usermanagement.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing the response to a valid invitation check.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationValidationResponseDto {

  private String email;
  private String role;
  private LocalDateTime expiresAt;
}
