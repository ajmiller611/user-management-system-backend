package io.github.ajmiller611.usermanagement.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing the response after creation of an invitation.
 *
 * <p>This DTO contains the token and metadata like email and expiration date for
 * confirmation purposes.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvitationResponseDto {

  private String token;
  private String email;
  private LocalDateTime expiresAt;

}
