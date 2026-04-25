package io.github.ajmiller611.usermanagement.dto;

import io.github.ajmiller611.usermanagement.controller.UserController;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object representing the request data for a "get users" request.
 *
 * <p>This DTO encapsulates the request data, including the page number and page size, to retrieve a
 * paginated list of users. It is processed in the controller and service layers to fetch a page
 * of users. See the
 * {@link UserController UserController} method
 * {@code getUsers(UserPaginationRequestDto)} for the handling of the request.</p>
 *
 * <p>Validation constraints ensure that the page number and size meets the specified criteria
 * before the request can be processed. Default values are provided for requests without
 * page and size data.</p>
 */
@Getter
@Setter
@AllArgsConstructor
public class UserPaginationRequestDto {

  @Min(value = 0, message = "Page number must not be negative")
  private int page;

  @Min(value = 1, message = "Size must be a positive number")
  private int size;

  /** No-argument constructor that initializes default values for page and size. */
  public UserPaginationRequestDto() {
    this.page = 0; // Default page
    this.size = 10; // Default size
  }
}
