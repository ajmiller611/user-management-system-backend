package io.github.ajmiller611.usermanagement.repository;

import io.github.ajmiller611.usermanagement.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Role} entities.
 *
 * <p>This interface extends {@link JpaRepository} to provide standard CRUD operations and allows
 * the definition of additional query methods for Role data access. It is annotated with
 * {@link Repository} to indicate its role in Spring Data.</p>
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

  /**
   * Retrieves a Role entity by its authority.
   *
   * <p>This method returns an {@link Optional} to indicate whether a Role was found with the
   * specified authority.</p>
   *
   * @param authority the authority of the Role to retrieve.
   * @return an {@link Optional} containing the Role if found, or empty if not.
   */
  Optional<Role> findByAuthority(String authority);
}

