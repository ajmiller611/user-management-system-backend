package io.github.ajmiller611.usermanagement.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ajmiller611.usermanagement.config.AppConfig;
import io.github.ajmiller611.usermanagement.config.SecurityConfig;
import io.github.ajmiller611.usermanagement.controller.UserController;
import io.github.ajmiller611.usermanagement.dto.UserResponseDto;
import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import io.github.ajmiller611.usermanagement.security.JwtService;
import io.github.ajmiller611.usermanagement.service.UserService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for the {@link UserController} class.
 *
 * <p>This test class comprehensively verifies the behavior and functionality of the
 * {@code UserController}'s endpoints. It ensures proper integration with security
 * configurations, request mappings, validation mechanisms, exception handling, and response
 * formatting. The tests also validate role-based access control, parameter validation, routing
 * accuracy, and logging.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Role-Based Access Control:</strong> Ensures only authorized roles (e.g., "USER",
 *     "ADMIN") can access endpoints, while unauthorized roles or unauthenticated requests are
 *     denied with appropriate HTTP status codes.
 *   </li>
 *   <li>
 *     <strong>Pagination Handling:</strong> Validates default and custom pagination parameters for
 *     list endpoints, including edge cases like invalid or missing parameters.
 *   </li>
 *   <li>
 *     <strong>Request Validation:</strong> Confirms that invalid path variables, request
 *     parameters, or data types are correctly rejected with meaningful error responses.
 *   </li>
 *   <li>
 *     <strong>Routing Validation:</strong> Verifies accurate URI mappings for controller methods,
 *     ensuring correct endpoint-to-method associations.
 *   </li>
 *   <li>
 *     <strong>Response Consistency:</strong> Ensures all API responses adhere to a consistent
 *     structure, including status, message, and data fields.
 *   </li>
 *   <li>
 *     <strong>Logging:</strong> Validates that significant actions and exceptions are logged
 *     appropriately for auditing and debugging purposes.
 *   </li>
 *   <li>
 *     <strong>Exception Handling:</strong> Ensures application-level exceptions, such as
 *     validation or runtime errors, are handled gracefully with meaningful error messages in
 *     the response.
 *   </li>
 *   <li>
 *     <strong>Specific Endpoint Behavior:</strong> Confirms the correct functionality of
 *     individual endpoints, such as retrieving users by ID and validating list filtering.
 *   </li>
 *   <li>
 *     <strong>Empty Data Handling:</strong> Tests edge cases where the data set is empty, ensuring
 *     the API responds appropriately with no errors and correct pagination metadata.
 *   </li>
 * </ul>
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
class UserControllerReadTests {

  @InjectMocks private UserController userController;
  @Autowired private MockMvc mockMvc;
  @MockBean private UserService userService;
  @MockBean private JwtService jwtService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private RoleRepository roleRepository;
  @MockBean private UserRepository userRepository;

  Long testUserId;
  User user;
  UserResponseDto userResponseDto;

  /** Initializes test data and mocks before each test case. */
  @BeforeEach
  void setUp() {
    Page<UserResponseDto> pagedUsers = createPagedUserResponseDtos(0, 10);
    when(userService.getUsers(0, 10)).thenReturn(pagedUsers);

    Role userRole = new Role("USER");
    user = new User(
        2L,
        "testUser",
        "password",
        "test@example.com",
        LocalDateTime.now(),
        Set.of(userRole)
    );
    testUserId = 2L;

    userResponseDto = new UserResponseDto(user.getUserId(), user.getUsername(), user.getEmail());
  }

  /** Verifies a user with the "USER" role can access the "/users" endpoint successfully. */
  @Test
  @WithMockUser // WithMockUser has a roles parameter and its default value is "USER"
  void givenUserWithRoleUserWhenGetUsersThenReturnsSuccessStatus() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  /** Verifies a user with the "ADMIN" role can access the "/users" endpoint successfully. */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  void givenUserWithRoleAdminWhenGetUsersThenReturnsSuccessStatus() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  /** Verifies a user without valid roles cannot access the "/users" endpoint. */
  @Test
  @WithMockUser(roles = {"GUEST"})
  void givenNoRoleWhenGetUsersThenRespondsForbiddenStatus() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isForbidden());
  }

  /** Verifies a user without a role cannot access the "/users" endpoint. */
  @Test
  @WithMockUser(roles = {})
  void givenEmptyRoleWhenGetUsersThenRespondsForbiddenStatus() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isForbidden());
  }

  /** Ensures an unauthenticated request to "/users" responds with "unauthorized". */
  @Test
  @WithAnonymousUser
  void givenUnauthenticatedWhenGetUsersThenRespondsUnauthorizedStatus() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isUnauthorized());
  }

  /** Verifies "/users" and "/users/" routes map to the correct method in the controller. */
  @Test
  @WithMockUser
  void givenUsersUrisWhenGetThenRoutesToGetUsersMethod() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(handler().methodName("getUsers"));

    mockMvc.perform(get("/users/"))
        .andExpect(status().isOk())
        .andExpect(handler().methodName("getUsers"));
  }

  /** Ensures default pagination parameters are used when no parameters are provided. */
  @Test
  @WithMockUser
  void givenNoParamsWhenGetUsersThenReturnsSuccessWithDefaults() throws Exception {
    int defaultPageNumber = 0;
    int defaultPageSize = 10;
    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));

    // Verify default values were used
    verify(userService, times(1))
        .getUsers(defaultPageNumber, defaultPageSize);
  }

  /** Verifies custom pagination parameters are passed to the service. */
  @Test
  @WithMockUser
  void givenCustomParamsWhenGetUsersThenCallsServiceWithParams() throws Exception {
    Page<UserResponseDto> customPagedUsers = createPagedUserResponseDtos(1, 5);
    when(userService.getUsers(1, 5)).thenReturn(customPagedUsers);

    mockMvc.perform(get("/users")
            .param("page", "1")
            .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));

    // Verify getUsers method was called once with the custom values
    verify(userService, times(1)).getUsers(1, 5);
  }

  /** Verifies invalid pagination parameters result in a bad request (400). */
  @Test
  @WithMockUser
  void givenInvalidParametersWhenGetUsersThenRespondsWithBadRequestAndErrorMessage()
      throws Exception {
    mockMvc.perform(get("/users")
            .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.page").value("Page number must not be negative"));

    mockMvc.perform(get("/users")
            .param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.size").value("Size must be a positive number"));
  }

  /** Verifies a valid request retrieves the correct page of non-admin users. */
  @Test
  @WithMockUser
  void givenValidRequestWhenGetUsersThenReturnsSuccessAndExpectedData() throws Exception {
    try (LogCaptor logCaptor = LogCaptor.forClass(UserController.class)) {
      mockMvc.perform(get("/users"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("success"))
          .andExpect(jsonPath("$.message")
              .value("Users retrieved successfully"))
          .andExpect(jsonPath("$.data.data[0].userId").value(2L))
          .andExpect(jsonPath("$.data.data[0].username").value("testUser"))
          .andExpect(jsonPath("$.data.data[0].email").value("test@example.com"))
          .andExpect(jsonPath("$.data.data[1].userId").value(3L))
          .andExpect(jsonPath("$.data.data[1].username").value("testUser1"))
          .andExpect(jsonPath("$.data.data[1].email").value("test1@example.com"))
          .andExpect(jsonPath("$.data.data[0].username", not("admin")))
          .andExpect(jsonPath("$.data.data[1].username", not("admin")))
          .andExpect(jsonPath("$.data.currentPage").value(0))
          .andExpect(jsonPath("$.data.totalPages").value(1))
          .andExpect(jsonPath("$.data.totalItems").value(2));

      verify(userService, times(1)).getUsers(anyInt(), anyInt());

      assertFalse(logCaptor.getInfoLogs().isEmpty(), "Expected info logs to not be empty.");
      assertEquals(2, logCaptor.getInfoLogs().size(),
          "Expected exactly 2 info log messages.");
      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Expected log to contain fetching users log but it was "
              + "missing.")
          .contains("Fetching users with page: 0, size: 10");
      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Expected log to contain user retrieval summary but it "
              + "was missing.")
          .contains("Retrieved users for page 0: 2 users (1 total pages, 2 total users)");
    }
  }

  /** Verifies the correct handling of empty data in the response. */
  @Test
  @WithMockUser
  void givenEmptyUserListWhenGetUsersThenReturnsSuccessWithEmptyData() throws Exception {
    Page<UserResponseDto> emptyPage =
        new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
    when(userService.getUsers(0, 10)).thenReturn(emptyPage);

    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.data.data").isEmpty())
        .andExpect(jsonPath("$.data.currentPage").value(0))
        .andExpect(jsonPath("$.data.totalPages").value(0))
        .andExpect(jsonPath("$.data.totalItems").value(0));

    verify(userService, times(1)).getUsers(anyInt(), anyInt());
  }

  /**
   * Creates a paginated response of {@link UserResponseDto} objects for testing purposes.
   *
   * @param page the page number to simulate (0-based index)
   * @param size the number of users per page
   * @return a {@link Page} containing the {@link UserResponseDto} objects for the specified page
   *     and size
   */
  private Page<UserResponseDto> createPagedUserResponseDtos(int page, int size) {
    List<UserResponseDto> users = Arrays.asList(
        new UserResponseDto(2L, "testUser", "test@example.com"),
        new UserResponseDto(3L, "testUser1", "test1@example.com")
    );
    return new PageImpl<>(users, PageRequest.of(page, size), users.size());
  }

  /** Verifies a valid user ID responds ok (200) with the user data and logged properly. */
  @Test
  @WithMockUser()
  void givenValidIdWhenGetUserByIdThenRespondsSuccessAndExpectedData() throws Exception {
    when(userService.getUserById(testUserId)).thenReturn(userResponseDto);

    try (LogCaptor logCaptor = LogCaptor.forClass(UserController.class)) {
      mockMvc.perform(get("/users/2"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("success"))
          .andExpect(jsonPath("$.message").value("User retrieved successfully"))
          .andExpect(jsonPath("$.data.userId").value(testUserId))
          .andExpect(jsonPath("$.data.username").value(user.getUsername()))
          .andExpect(jsonPath("$.data.email").value(user.getEmail()));

      verify(userService, times(1)).getUserById(testUserId);

      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Expected log to contain GET request log for user ID 2 "
              + "but it was missing.")
          .contains("Endpoint '/user/{id}' received GET request with id = 2");
    }
  }

  /**
   * Verifies an invalid parameter type responds with a bad request (400).
   * This test case verifies that validation of path variable's parameter types are integrated.
   */
  @Test
  @WithMockUser()
  void givenInvalidParamsTypeWhenGetUserByIdThenRespondsBadRequest() throws Exception {
    mockMvc.perform(get("/users/string"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Invalid argument data type"));
  }

  /**
   *  Verifies an invalid user id responds with a bad request (400).
   *  This test case verifies that validation annotations are integrated.
   */
  @Test
  @WithMockUser()
  void givenInvalidUserIdWhenGetUserByIdThenRespondsBadRequest() throws Exception {
    mockMvc.perform(get("/users/0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"));
  }
}
