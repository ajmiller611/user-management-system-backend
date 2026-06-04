package io.github.ajmiller611.usermanagement.exception;

import lombok.Getter;

/** Custom exception throw when an invitation has expired. */
@Getter
public class InvitationExpiredException extends RuntimeException {

  /**
   * Constructs a new {@code InvitationEpiredException} with specified detail message.
   *
   * @param message the detail message explaining the reason for the exception
   */
  public InvitationExpiredException(String message) {
    super(message);
  }
}
