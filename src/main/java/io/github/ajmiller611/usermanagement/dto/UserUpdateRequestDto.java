package io.github.ajmiller611.usermanagement.dto;

import io.github.ajmiller611.usermanagement.annotation.ValidEmail;
import io.github.ajmiller611.usermanagement.dto.UserUpdateRequestDto.Second;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) with data to be updated.
 *
 * <p>This object encapsulates the necessary data to be updated, including the username and email.
 * The DTO is received in the controller layer and processed through the service layer to update the
 * user's details with the new data.</p>
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
@GroupSequence(value = {UserUpdateRequestDto.class, Second.class})
public class UserUpdateRequestDto {

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
   * Email of the user, validated to ensure it is formatted correctly.
   */
  @ValidEmail // custom annotation to valid email
  private String email;
}
