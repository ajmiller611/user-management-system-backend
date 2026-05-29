package io.github.ajmiller611.usermanagement.exception;

import lombok.Getter;

/** Custom exception thrown when an invitation has already been used. */
@Getter
public class InvitationAlreadyUsedException extends RuntimeException {

  /**
   * Constructs a new {@code InvitationAlreadyUsedException} with the specified detail message.
   *
   * @param message the detail message explaining the reason for the exception
   */
  public InvitationAlreadyUsedException(String message) {
    super(message);
  }
}
