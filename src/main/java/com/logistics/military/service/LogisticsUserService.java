package com.logistics.military.service;

import com.logistics.military.annotation.CheckUserExistence;
import com.logistics.military.dto.LogisticsUserDto;
import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.dto.UserResponseDto;
import com.logistics.military.dto.UserUpdateRequestDto;
import com.logistics.military.exception.RoleNotFoundException;
import com.logistics.military.exception.UnauthorizedOperationException;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing and processing user-related operations,
 * including user registration, password encoding, role assignment, and user retrieval.
 *
 * <p>This service encapsulates the core business logic for user management,
 * ensuring secure and efficient handling of user data. It interacts with the
 * persistence layer through {@link LogisticsUserRepository} and integrates with
 * Spring Security for authentication and role-based access control.</p>
 */
@Service
@RequiredArgsConstructor
public class LogisticsUserService implements UserDetailsService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final LogisticsUserRepository logisticsUserRepository;
  private final Clock clock;

  private static final String ROLE_NAME_ADMIN = "ADMIN";

  /**
   * Registers a new user by encoding the password and assigning a default role before
   * saving the user information to the database.
   *
   * <p>This method ensures secure handling of user data by performing the following actions:
   * - Validating the user does not already exist (via {@link CheckUserExistence}).
   * - Encoding the user's password before saving.
   * - Assigning the default "USER" role to the new user.
   * - Persisting the user in the database and returning the user details.
   * </p>
   *
   * @param userRequestDto the {@link UserRequestDto} containing the user's registration data.
   * @return a {@link LogisticsUserDto} object containing the registered user's details.
   * @throws RoleNotFoundException if the "USER" role is missing in the database.
   */
  @CheckUserExistence(checkBy = "username")
  public LogisticsUserDto createAndSaveUser(UserRequestDto userRequestDto) {
    logger.info("Create user request with DTO: {}",  userRequestDto);

    // Prepare user for registration by setting required fields and encoding the password.
    LogisticsUser user = new LogisticsUser();
    user.setUsername(userRequestDto.getUsername());
    user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
    user.setEmail(userRequestDto.getEmail());
    user.setCreatedAt(LocalDateTime.now(clock)); // Timestamp for auditing user creation.

    // Attempt to retrieve and assign the default role. Throws an error if the role is unavailable.
    Role userRole = roleRepository.findByAuthority("USER")
        .orElseThrow(() -> new RoleNotFoundException("Role 'USER' not found"));

    Set<Role> authorities = new HashSet<>();
    authorities.add(userRole);
    user.setAuthorities(authorities);

    // Save the user in the database to generate userId.
    user = logisticsUserRepository.save(user);

    logger.info("LogisticsUser created: {}", user);
    return mapToUserDto(user);
  }

  /**
   * Retrieves a paginated list of users, excluding any users with the "ADMIN" role.
   *
   * <p>This method queries the database for all users without the "ADMIN" role before mapping
   * the remaining users to {@link UserResponseDto} objects.
   * </p>
   *
   * @param page the page number to retrieve.
   * @param size the number of users per page.
   * @return a {@link Page} containing {@link UserResponseDto} objects for the requested page,
   *         excluding users with the "ADMIN" role.
   * @throws RoleNotFoundException if the "ADMIN" role is missing in the database.
   */
  public Page<UserResponseDto> getUsers(int page, int size) {
    // Set the paging restrictions for the pageable object
    Pageable pageable = PageRequest.of(page, size);
    Role adminRole = roleRepository.findByAuthority(ROLE_NAME_ADMIN)
        .orElseThrow(() -> new RoleNotFoundException("Role 'ADMIN' not found"));
    Page<LogisticsUser> usersPage =
        logisticsUserRepository.findAllWithoutRole(pageable, adminRole);

    // Convert the users to response DTOs
    List<UserResponseDto> userResponseDtos = usersPage.stream()
        .map(this::mapToUserResponseDto)
        .toList();

    return new PageImpl<>(userResponseDtos, pageable, userResponseDtos.size());
  }

  /**
   * Retrieves a user with the specified ID, excluding users with the "ADMIN" role.
   *
   * <p>This method queries the database for a user with the given ID. If the user exists and
   * does not have the "ADMIN" role, the method returns a {@link UserResponseDto} containing the
   * user's details. If the user has the "ADMIN" role, it throws an
   * {@link UnauthorizedOperationException}.
   * </p>
   *
   * @param id the ID of the user to retrieve
   * @return a {@link UserResponseDto} containing the user's details if the user exists and does not
   *         have the "ADMIN" role.
   * @throws UnauthorizedOperationException if the user has the "ADMIN" role.
   */
  @CheckUserExistence // Check by ID is default behavior
  public UserResponseDto getUserById(Long id) {
    /*
     * The @CheckUserExistence aspect handles when the ID provided is to a nonexistent user. This
     * interception by the aspect makes sure that this method wouldn't be able to execute when an ID
     * is nonexistent. Since findById() returns an Optional, best practice is to only access the
     * value after calling isPresent(). In this case, a value will always be present due to the
     * aspect validation check. To prevent making redundant calls to the database by first calling
     * isPresent() and then findById(), I am using .orElse() with an empty LogisticsUser object to
     * satisfy the concern of accessing a value on an empty Optional. In practice,
     * this empty LogisticsUser object will never be assigned to the user variable due to the
     * aspects' validation.
     */
    LogisticsUser user = logisticsUserRepository.findById(id).orElse(new LogisticsUser());

    if (user.hasRole(ROLE_NAME_ADMIN)) {
      throw new UnauthorizedOperationException(
          String.format("Unauthorized user cannot update admin user with id %d", id), id);
    }

    return new UserResponseDto(user.getUserId(), user.getUsername(), user.getEmail());
  }

  /**
   * Updates the details of an existing user, excluding users with the "ADMIN" role.
   *
   * @param id the ID of the user to be updated
   * @param requestDto the {@link UserUpdateRequestDto} containing the updated data for the user
   * @return a {@link UserResponseDto} containing the updated user's details
   * @throws UnauthorizedOperationException if the user has the "ADMIN" role.
   */
  @CheckUserExistence
  public UserResponseDto updateUser(Long id, UserUpdateRequestDto requestDto) {
    // Refer to the explanation in getUserById() for why this approach is used.
    LogisticsUser user = logisticsUserRepository.findById(id).orElse(new LogisticsUser());

    if (user.hasRole(ROLE_NAME_ADMIN)) {
      throw new UnauthorizedOperationException(
          String.format("Unauthorized user cannot update admin user with id %d", id), id);
    }

    user.setUsername(requestDto.getUsername());
    user.setEmail(requestDto.getEmail());
    user = logisticsUserRepository.save(user);

    return new UserResponseDto(user.getUserId(), user.getUsername(), user.getEmail());
  }

  /**
   * Deletes an existing user based on the provided id.
   *
   * @param id the id of the user to be deleted
   * @throws UnauthorizedOperationException if the user is an admin
   */
  @CheckUserExistence
  public void deleteUser(Long id) {
    // Refer to the explanation in getUserById() for why this approach is used.
    LogisticsUser user = logisticsUserRepository.findById(id).orElse(new LogisticsUser());

    if (user.hasRole(ROLE_NAME_ADMIN)) {
      throw new UnauthorizedOperationException(
          String.format("Unauthorized user cannot delete admin user with id %d", id), id);
    }

    logisticsUserRepository.deleteById(id);
  }

  /**
   * Loads a user by their username.
   *
   * <p>This method is used by controllers to return a user in DTO from.
   * It retrieves a user based on their username and returns their details,
   * which include credentials and authorities.
   * </p>
   *
   * @param username the username of the user to load
   * @return the {@link UserDetails} containing user information for authentication
   */
  @CheckUserExistence
  public LogisticsUserDto getUserByUsername(String username) {
    LogisticsUser user = logisticsUserRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return mapToUserDto(user);
  }

  /**
   * Loads a user by their username.
   *
   * <p>This method is required by {@link UserDetailsService} and is used by Spring Security
   * to authenticate a user. It retrieves a user based on their username and returns their
   * details, which include credentials and authorities.
   * </p>
   *
   * @param username the username of the user to load
   * @return the {@link UserDetails} containing user information for authentication
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return logisticsUserRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User is not valid"));
  }

  /**
   * Convert a {@link LogisticsUser} object to a {@link LogisticsUserDto} object.
   * This method also converts {@link org.springframework.security.core.GrantedAuthority} objects
   * into {@link Role} objects to reflect the user's roles.
   *
   * @param user a {@link LogisticsUser} object containing the user's data to be converted
   *             to a {@link LogisticsUserDto} object.
   * @return a {@link LogisticsUserDto} object containing the transformed user data.
   */
  public LogisticsUserDto mapToUserDto(LogisticsUser user) {
    Set<Role> roles = user.getAuthorities().stream()
        .map(authority -> {
          Role role = new Role();
          role.setAuthority(authority.getAuthority());
          return role;
        })
        .collect(Collectors.toSet());

    return new LogisticsUserDto(
        user.getUserId(),
        user.getUsername(),
        user.getEmail(),
        user.getCreatedAt(),
        roles
    );
  }

  /**
   * Convert a {@link LogisticsUser} object to a {@link UserResponseDto} object.
   *
   * @param user a {@link LogisticsUser} object containing the user's data to be converted to a
   *             {@link LogisticsUserDto} object.
   * @return a {@link UserResponseDto} object containing the transformed user data.
   */
  public UserResponseDto mapToUserResponseDto(LogisticsUser user) {
    return new UserResponseDto(user.getUserId(), user.getUsername(), user.getEmail());
  }
}