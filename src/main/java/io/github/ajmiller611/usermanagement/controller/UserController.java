package io.github.ajmiller611.usermanagement.controller;

import io.github.ajmiller611.usermanagement.dto.UserDto;
import io.github.ajmiller611.usermanagement.dto.UserPaginationRequestDto;
import io.github.ajmiller611.usermanagement.dto.UserRequestDto;
import io.github.ajmiller611.usermanagement.dto.UserResponseDto;
import io.github.ajmiller611.usermanagement.dto.UserUpdateRequestDto;
import io.github.ajmiller611.usermanagement.response.PaginatedData;
import io.github.ajmiller611.usermanagement.response.ResponseWrapper;
import io.github.ajmiller611.usermanagement.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Controller for handling user-related HTTP requests.
 *
 * <p>This controller exposes endpoints for users with the 'user' role access level.
 * It also facilitates interaction with the {@link UserService} for user-related
 * operations.
 * </p>
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final UserService userService;

  private static final String NONPOSITIVE_USER_ID_ERROR_MESSAGE =
      "User id must be greater than zero";

  /**
   * Registers a new user in the system.
   *
   * <p>This endpoint receives a {@link UserRequestDto} object containing the user's registration
   * details, including username, password, and email. It delegates user creation to the
   * {@link UserService} and, upon successful registration, returns a response with the
   * user’s information and the location of the new user resource.
   * </p>
   *
   * @param body the {@link UserRequestDto} containing the user's registration data
   * @return a {@link ResponseEntity} containing the newly created {@link UserResponseDto}
   *     with a `Location` header for the created resource
   */
  @PostMapping({"/", ""})
  public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody UserRequestDto body) {
    logger.info("Endpoint /users received POST request: {}", body);

    // Delegate user creation to the user service
    UserDto userDto = userService.createAndSaveUser(body);

    // Build the URI location of the newly created user resource
    URI location = ServletUriComponentsBuilder
        .fromCurrentContextPath()
        .path("/users/{id}")
        .buildAndExpand(userDto.getUserId())
        .toUri();

    // Map the created user details to the response DTO
    UserResponseDto userResponseDto = new UserResponseDto(
        userDto.getUserId(),
        userDto.getUsername(),
        userDto.getEmail()
    );

    // Return a response entity with 201 Created status and user details
    ResponseEntity<UserResponseDto> response =
        ResponseEntity.created(location).body((userResponseDto));
    logger.info("Endpoint /users response: {}", response);
    return response;
  }

  /**
   * Retrieves a paginated list of users.
   *
   * <p>This endpoint allows fetching users in a paginated manner. By default,
   * the first page with a size of 10 users is returned if no query parameters are provided.
   * Specific page number and page size can be provided as parameters.</p>
   *
   * <p>Invalid parameters (e.g., negative page number or non-positive size) are handled
   * through parameter validation and will result in an appropriate error response.</p>
   *
   * @param paginationRequest a {@link UserPaginationRequestDto} containing the
   *                          page number to retrieve (default: 0) and the number of users to
   *                          retrieve per page (default: 10)
   * @return a {@link ResponseEntity} containing a {@link ResponseWrapper} with a
   *     paginated list of {@link UserResponseDto}.
   */
  @GetMapping({"/", ""})
  public ResponseEntity<ResponseWrapper<PaginatedData<UserResponseDto>>> getUsers(
      @Valid UserPaginationRequestDto paginationRequest) {

    logger.info("Fetching users with page: {}, size: {}",
        paginationRequest.getPage(), paginationRequest.getSize());

    Page<UserResponseDto> pagedUsers =
        userService.getUsers(paginationRequest.getPage(), paginationRequest.getSize());
    PaginatedData<UserResponseDto> paginatedData = new PaginatedData<>(
        pagedUsers.getContent(),
        pagedUsers.getNumber(),
        pagedUsers.getTotalPages(),
        pagedUsers.getTotalElements()
    );

    logger.info("Retrieved users for page {}: {} users ({} total pages, {} total users)",
        pagedUsers.getNumber(),
        pagedUsers.getContent().size(),
        pagedUsers.getTotalPages(),
        pagedUsers.getTotalElements());

    return ResponseEntity.ok(
        ResponseWrapper.success(paginatedData, "Users retrieved successfully"));
  }

  /**
   * Retrieves a user by id.
   */
  @GetMapping("/{id}")
  public ResponseEntity<ResponseWrapper<UserResponseDto>> getUserById(
      @PathVariable(name = "id") @Positive(message = NONPOSITIVE_USER_ID_ERROR_MESSAGE) Long id) {

    logger.info("Endpoint '/user/{id}' received GET request with id = {}", id);
    UserResponseDto responseDto = userService.getUserById(id);
    return ResponseEntity.ok(
        ResponseWrapper.success(responseDto, "User retrieved successfully"));
  }

  /**
   * Updates a user.
   */
  @PutMapping("/{id}")
  public ResponseEntity<ResponseWrapper<UserResponseDto>> updateUser(
      @PathVariable(name = "id") @Positive(message = NONPOSITIVE_USER_ID_ERROR_MESSAGE) Long id,
      @Valid @RequestBody UserUpdateRequestDto updateRequestDto) {

    logger.info("Endpoint '/users/{id}' received PUT request with id = {}", id);
    UserResponseDto responseDto = userService.updateUser(id, updateRequestDto);
    return ResponseEntity.ok(
        ResponseWrapper.success(responseDto, "User updated successfully"));
  }

  /**
   * Deletes a user.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ResponseWrapper<UserResponseDto>> deleteUser(
      @PathVariable(name = "id") @Positive(message = NONPOSITIVE_USER_ID_ERROR_MESSAGE) Long id) {

    logger.info("Endpoint '/users/{id}' received DELETE request with id = {}", id);
    userService.deleteUser(id);
    return ResponseEntity.ok(
        ResponseWrapper.success(null, "User deleted successfully"));
  }
}
