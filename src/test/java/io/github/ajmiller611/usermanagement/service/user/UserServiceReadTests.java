package io.github.ajmiller611.usermanagement.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.dto.UserResponseDto;
import io.github.ajmiller611.usermanagement.exception.RoleNotFoundException;
import io.github.ajmiller611.usermanagement.exception.UnauthorizedOperationException;
import io.github.ajmiller611.usermanagement.exception.UserNotFoundException;
import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import io.github.ajmiller611.usermanagement.service.UserService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link UserService} class.
 *
 * <p>This test class validates the {@code UserService}'s read functionalities,
 * focusing on correct implementation of user retrieval operations, pagination logic,
 * and proper handling of edge cases and exceptions.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Pagination and Filtering:</strong> Ensures accurate pagination behavior,
 *     including retrieving non-admin users, returning correct page content, and handling
 *     empty datasets appropriately.
 *   </li>
 *   <li>
 *     <strong>Role-Based Exclusion:</strong> Confirms that admin users are excluded
 *     from user retrieval operations.
 *   </li>
 *   <li>
 *     <strong>Error Handling:</strong> Verifies exceptions such as {@link RoleNotFoundException},
 *     {@link UserNotFoundException}, and {@link UnauthorizedOperationException} are thrown
 *     and handled correctly when invalid operations are performed.
 *   </li>
 *   <li>
 *     <strong>DTO Mapping:</strong> Validates conversion of {@link User} objects
 *     to {@link UserResponseDto} objects, ensuring consistency in returned data.
 *   </li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceReadTests {

  @InjectMocks private UserService userService;
  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;

  Role adminRole;
  Role userRole;
  Long userId;
  User user;
  Pageable pageable;
  Page<User> pagedUsers;

  /** Initializes test data before each test case. */
  @BeforeEach
  void setUp() {
    userId = 2L;
    adminRole = new Role(1, "ADMIN");
    userRole = new Role(2, "USER");
    user = new User(
        userId,
        "testUser",
        "password",
        "test@example.com",
        LocalDateTime.now(),
        Set.of(userRole)
    );

    // Create a test page of users for use in each test case
    pageable = PageRequest.of(0, 10);
    pagedUsers = createPagedUsers(0, 10, 2);
  }

  /** Ensures {@link User} objects are converted to {@link UserResponseDto} objects. */
  @Test
  void givenUserWhenMapToUserResponseDtoThenReturnUserResponseDto() {
    UserResponseDto responseDto = userService.mapToUserResponseDto(user);

    assertNotNull(responseDto, "The mapped UserResponseDto should not be null.");

    assertEquals(user.getUserId(), responseDto.getUserId(),
        "User ID should match between entity and DTO.");

    assertEquals(user.getUsername(), responseDto.getUsername(),
        "Username should match between entity and DTO.");

    assertEquals(user.getEmail(), responseDto.getEmail(),
        "Email should match between entity and DTO.");
  }

  /** Verify an {@link RoleNotFoundException} is thrown when the 'ADMIN' role does not exist. */
  @Test
  void givenRoleNotFoundWhenGetUsersThenThrowRoleNotFoundException() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.empty());

    assertThrows(RoleNotFoundException.class,
        () -> userService.getUsers(0, 10),
        "Expected getUsers to throw a RoleNotFoundException for role not found");
  }

  /**
   * Verifies a valid page and size request retrieves non-admin users, converts them to DTOs,
   * and returns a paginated result.
   */
  @Test
  void givenValidPageAndSizeWhenGetUsersThenReturnPageOfUsers() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(adminRole));
    when(userRepository.findAllWithoutRole(pageable, adminRole)).thenReturn(pagedUsers);

    Page<UserResponseDto> fetchedUsers = userService.getUsers(0, 10);

    verify(userRepository, times(1))
        .findAllWithoutRole(pageable, adminRole);
    assertNotNull(fetchedUsers, "Fetched users should not be null.");
    assertEquals(pagedUsers.getNumber(), fetchedUsers.getNumber(), "Page number should match.");
    assertEquals(pagedUsers.getTotalPages(), fetchedUsers.getTotalPages(),
        "Total pages should match.");

    assertEquals(pagedUsers.getTotalElements(), fetchedUsers.getTotalElements(),
        "Total elements should match.");

    assertEquals(pagedUsers.getContent().size(), fetchedUsers.getContent().size(),
        "Content size should match.");

    assertTrue(fetchedUsers.getContent().stream()
        .noneMatch(dto -> "admin".equals(dto.getUsername())),
        "No admin users should be included.");

    assertIterableEquals(pagedUsers.getContent().stream()
        .map(userService::mapToUserResponseDto)
        .toList(), fetchedUsers.getContent(), "Fetched users should match the paged content.");
  }

  /** Verifies when no users exist then an empty page is returned. */
  @Test
  void givenNoUsersWhenGetUsersThenReturnsEmptyPage() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(adminRole));
    Page<User> emptyPage =
        new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
    when(userRepository.findAllWithoutRole(pageable, adminRole)).thenReturn(emptyPage);

    Page<UserResponseDto> fetchedUsers
        = userService.getUsers(0, 10);

    verify(userRepository, times(1))
        .findAllWithoutRole(pageable, adminRole);
    assertNotNull(fetchedUsers, "Fetched users should not be null; "
        + "the service should always return a page, even if empty.");
    assertTrue(fetchedUsers.isEmpty(), "Fetched users should be an empty page");
    assertEquals(0, fetchedUsers.getNumber(), "Current page number should be 0");
    assertEquals(0, fetchedUsers.getTotalPages(), "Total pages should be 0");
    assertEquals(0, fetchedUsers.getTotalElements(), "Total elements should be 0");
    assertEquals(0, fetchedUsers.getContent().size(), "Content size should be 0");
  }

  /** Verifies correct number of users is returned given a specific page size. */
  @Test
  void givenSpecificPageSizeWhenGetUsersThenReturnCorrectNumberOfUsers() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(adminRole));
    pageable = PageRequest.of(0, 1);
    Page<User> sizeOnePage = createPagedUsers(0, 1, 2);
    when(userRepository.findAllWithoutRole(pageable, adminRole)).thenReturn(sizeOnePage);

    Page<UserResponseDto> fetchedUsers = userService.getUsers(0, 1);

    verify(userRepository, times(1))
        .findAllWithoutRole(pageable, adminRole);
    assertNotNull(fetchedUsers, "Fetched users should not be null.");
    assertEquals(sizeOnePage.getContent().size(), fetchedUsers.getContent().size(),
        "Content size should match.");

    assertEquals(sizeOnePage.getNumber(), fetchedUsers.getNumber(),
        "Page number should match.");

    assertEquals(sizeOnePage.getTotalPages(), fetchedUsers.getTotalPages(),
        "Total pages should match.");

    assertEquals(sizeOnePage.getTotalElements(), fetchedUsers.getTotalElements(),
        "Total elements should match.");
  }

  /** Verifies correct subset of users is returned given a specific page number. */
  @Test
  void givenSpecificPageNumberWhenGetUsersThenReturnCorrectPage() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(adminRole));
    pageable = PageRequest.of(1, 1);

    final User expectedUser = new User(
        3L,
        "testUser2",
        "encodedPassword",
        "test2@example.com",
        LocalDateTime.now(),
        Set.of(userRole)
    );

    Page<User> pageOne = createPagedUsers(1, 1, 3);
    when(userRepository.findAllWithoutRole(pageable, adminRole)).thenReturn(pageOne);

    Page<UserResponseDto> fetchedUsers = userService.getUsers(1, 1);

    verify(userRepository, times(1))
        .findAllWithoutRole(pageable, adminRole);
    assertNotNull(fetchedUsers, "Fetched users should not be null.");
    assertEquals(pageOne.getNumber(), fetchedUsers.getNumber(), "Page number should match.");
    assertEquals(pageOne.getTotalPages(), fetchedUsers.getTotalPages(),
        "Total pages should match.");

    assertEquals(pageOne.getTotalElements(), fetchedUsers.getTotalElements(),
        "Total elements should match.");

    assertEquals(1, fetchedUsers.getContent().size(), "Content size should be 1.");
    UserResponseDto expectedUserDto = userService.mapToUserResponseDto(expectedUser);
    assertEquals(expectedUserDto, fetchedUsers.getContent().getFirst(),
        "The first user in the page should match.");
  }

  /** Verifies the last page gets the remaining users correctly. */
  @Test
  void givenLastPageNumberWhenGetUsersThenReturnLastPage() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(adminRole));
    int totalUsers = 8;
    int pageSize = 3;
    int lastPageNumber = (totalUsers / pageSize);

    Pageable lastPageable = PageRequest.of(lastPageNumber, pageSize);
    Page<User> lastPage =
        createPagedUsers(lastPageNumber, pageSize, totalUsers);
    when(userRepository.findAllWithoutRole(lastPageable, adminRole)).thenReturn(lastPage);

    Page<UserResponseDto> fetchedUsers = userService.getUsers(lastPageNumber, pageSize);

    verify(userRepository, times(1))
        .findAllWithoutRole(lastPageable, adminRole);
    assertNotNull(fetchedUsers, "Fetched users should not be null.");
    assertEquals(lastPageNumber, fetchedUsers.getNumber(), "Page number should match.");
    assertEquals((totalUsers + pageSize - 1) / pageSize, fetchedUsers.getTotalPages(),
        "Total pages should match.");

    assertEquals(totalUsers, fetchedUsers.getTotalElements(), "Total elements should match.");

    int remainingUsers = totalUsers % pageSize;
    assertEquals(remainingUsers, fetchedUsers.getContent().size(),
        "Remaining users in the last page should match.");

    List<UserResponseDto> expectedDtos = lastPage.stream()
        .map(userService::mapToUserResponseDto)
        .toList();

    assertEquals(expectedDtos, fetchedUsers.getContent(),
        "Fetched users should match expected DTOs.");
  }

  /**
   * Creates a paginated response of {@link User} objects for testing purposes.
   * This method simulates the creation of a pageable dataset of users.
   * This simulates an admin user exists assigned an ID of 1, and regular users are assigned
   * sequential IDs starting from 2.
   *
   * <p>The method calculates the subset of users based on the specified page and page size,
   * simulating pagination behavior consistent with typical database queries.
   *
   * @param page the page number to simulate (0-based index)
   * @param pageSize the number of users per page
   * @param totalUsers the total number of users to generate
   * @return a {@link Page} containing the {@link User} objects for the specified page
   *     and size
   */
  private Page<User> createPagedUsers(int page, int pageSize, int totalUsers) {
    List<User> usersList = new ArrayList<>();

    int currentId = 1; // Start with ID 1 to account for an admin user with ID 1 existing.
    for (int i = 1; i <= totalUsers; i++) {
      usersList.add(new User(
          (long) ++currentId,
          "testUser" + i,
          "encodedPassword",
          "test" + i + "@example.com",
          LocalDateTime.now(),
          Set.of(userRole))
      );
    }

    // Simulate pagination
    int start = Math.min(page * pageSize, usersList.size());
    int end = Math.min(start + pageSize, usersList.size());
    List<User> pageContent = usersList.subList(start, end);

    return new PageImpl<>(pageContent, PageRequest.of(page, pageSize), pageContent.size());
  }

  /** Verifies that a valid id returns the user's data. */
  @Test
  void givenValidIdWhenGetUserByIdThenReturnUser() {
    Optional<User> optionalUser = Optional.of(user);
    when(userRepository.findById(userId)).thenReturn(optionalUser);

    UserResponseDto result = userService.getUserById(userId);

    verify(userRepository, times(1)).findById(userId);
    assertNotNull(result, "The result should not be null when a valid user ID is provided.");

    assertEquals(user.getUserId(), result.getUserId(),
        "The user ID in the response should match the input ID.");

    assertEquals(user.getUsername(), result.getUsername(),
        "The username in the response should match the input username.");

    assertEquals(user.getEmail(), result.getEmail(),
        "The email in the response should match the input email.");
  }

  /** Verifies that an admin user ID throws {@link UnauthorizedOperationException}. */
  @Test
  void givenAdminIdTypeWhenGetUserByIdThenThrowsUnauthorizedOperationException() {
    Long adminUserId = 1L;
    User adminUser = mock(User.class);
    when(adminUser.hasRole("ADMIN")).thenReturn(true);

    when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));

    UnauthorizedOperationException exception = assertThrows(UnauthorizedOperationException.class,
        () -> userService.getUserById(adminUserId));

    verify(userRepository, times(1)).findById(1L);
    assertNotNull(exception, "An UnauthorizedOperationException should be thrown when "
        + "attempting to access an admin user.");

    assertEquals(
        String.format("Unauthorized user cannot update admin user with id %d", adminUserId),
        exception.getMessage(),
        "The exception message should indicate the unauthorized attempt to "
            + "access an admin user.");

    assertEquals(adminUserId, exception.getId(),
        "The exception should reference the correct user ID.");
  }
}
