package io.github.ajmiller611.usermanagement.dto;

import io.github.ajmiller611.usermanagement.annotation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for creating an invitation.
 * Contains the email and role required to generate a new invitation.
 *
 * <p>This class also utilizes a custom annotation, {@link ValidEmail} for email validation.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRequestDto {

  @NotBlank
  @ValidEmail
  private String email;

  @NotBlank
  @Pattern(regexp = "ADMIN|USER")
  private String role;
}
