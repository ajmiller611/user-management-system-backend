package io.github.ajmiller611.usermanagement.repository;

import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link User} entities.
 *
 * <p>This interface extends {@link JpaRepository} to provide standard CRUD operations and allows
 * the definition of additional query methods for User data access. It is annotated with
 * {@link Repository} to indicate its role in Spring Data.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Retrieves a {@link User} entity by its username.
   *
   * <p>This method returns an {@link Optional} to indicate whether a User was found with the
   * specified username.
   *
   * @param username the username of the {@link User} to retrieve.
   * @return an {@link Optional} containing the {@link User} if found, or empty if not.
   */
  Optional<User> findByUsername(String username);

  /**
   * Retrieves a paginated list of {@link User} entities where the users do not have the
   * specified role. This is used to exclude users with a specific role, such as admin users,
   * from the result set.
   *
   * @param pageable The pagination information, including the page number and size,
   *                 to control the result set's paging.
   * @param role The role to exclude from the list of users.
   * @return A {@link Page} of {@link User} entities where users do not have the specified
   *         role, wrapped according to the pagination criteria.
   */
  @Query("SELECT u FROM User u WHERE NOT :role MEMBER OF u.authorities")
  Page<User> findAllWithoutRole(Pageable pageable, Role role);
}
