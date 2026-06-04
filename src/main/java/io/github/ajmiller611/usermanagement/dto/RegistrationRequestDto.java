package io.github.ajmiller611.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for completing registration of a user.
 *
 * <p>This object encapsulates the necessary registration data, including the token of
 * an invitation, username, and password required to create a user. The DTO is received in the
 * controller layer and processed through the service layer to create a new user in the system.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDto {

  @NotBlank(message = "Token is required")
  private String token;

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
  private String username;

  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  private String password;

  /**
   * A method to print out values of nonsensitive fields.
   *
   * @return a String representation of the DTO without sensitive fields
   */
  @Override
  public String toString() {
    return "RegistrationRequestDto(username=" + this.username + ")";
  }
}
