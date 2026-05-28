package io.github.ajmiller611.usermanagement.repository;

import io.github.ajmiller611.usermanagement.model.Invitation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Invitation} entities.
 *
 * <p>This interface extends {@link JpaRepository} to provide standard CRUD operations and allows
 * additional query methods for Invitation data access. It is annotated with {@link Repository} to
 * indicate its role in Spring Data.</p>
 */
@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

  /**
   * Retrieves an {@link Invitation} entity by its token.
   *
   * @param token the token of the {@link Invitation} to retrieve
   * @return an {@link Optional} containing the {@link Invitation} if found, or empty if not
   */
  Optional<Invitation> findByToken(String token);

  /**
   * Retrieves an {@link Invitation} entity by its email.
   *
   * @param email the email of the {@link Invitation} to retrieve
   * @return an {@link Optional} containing the {@link Invitation} if found, or empty if not
   */
  Optional<Invitation> findByEmail(String email);
}
