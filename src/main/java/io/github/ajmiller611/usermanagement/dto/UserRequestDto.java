package io.github.ajmiller611.usermanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.ajmiller611.usermanagement.annotation.ValidEmail;
import io.github.ajmiller611.usermanagement.dto.UserRequestDto.Second;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for user registration data.
 *
 * <p>This object encapsulates the necessary registration data, including the username,
 * password, and email, required from the client during the user registration process.
 * The DTO is received in the controller layer and processed through the service layer
 * to create a new user in the system.
 * </p>
 *
 * <p>Validation constraints ensure that each field meets specific criteria before the
 * data is processed, improving data integrity and user experience. Validation groups
 * are defined to sequence constraints to ensure the order the validation occurs.
 * </p>
 *
 * <p>This class also utilizes a custom annotation, <code>@ValidEmail</code>,
 * for advanced email validation.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties
@GroupSequence(value = {UserRequestDto.class, Second.class})
public class UserRequestDto {

  /**
   * Secondary validation group for staging additional constraints.
   */
  interface Second {}

  /**
   * Username of the user, required and validated to ensure it is between 3 and 20 characters.
   */
  @NotBlank(message = "Username is required") // Belongs to the default group and validates first
  @Size(
      min = 3,
      max = 20,
      message = "Username must be between 3 and 20 characters",
      groups = Second.class
  )
  private String username;

  /**
   * Password of the user, required and validated to be at least 8 characters.
   */
  @NotBlank(message = "Password is required") // Belongs to the default group and validates first
  @Size(min = 8, message = "Password must be at least 8 characters", groups = Second.class)
  private String password;

  /**
   * Email of the user, validated to ensure it is formatted correctly.
   */
  @ValidEmail // custom annotation to valid email
  private String email;

  /**
   * A method to print out the values of nonsensitive fields.
   *
   * @return a String representation of the DTO without sensitive fields.
   */
  @Override
  public String toString() {
    return "UserRequestDto(username=" + this.username + ", email=" + this.email + ")";
  }
}
