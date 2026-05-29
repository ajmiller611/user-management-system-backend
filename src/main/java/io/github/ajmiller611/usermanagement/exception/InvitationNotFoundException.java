package io.github.ajmiller611.usermanagement.exception;

import lombok.Getter;

/** Custom exception thrown when an invitation is not found in the database. */
@Getter
public class InvitationNotFoundException extends RuntimeException {

  /**
   * Constructs a new {@code InvitationNotFoundException} with the specified detail message.
   *
   * @param message the detail message explaining the reason for the exception
   */
  public InvitationNotFoundException(String message) {
    super(message);
  }
}
