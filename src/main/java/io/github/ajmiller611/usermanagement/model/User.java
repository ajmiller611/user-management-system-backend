package io.github.ajmiller611.usermanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


/**
 * Represents a user in the system. This entity is mapped to the 'users' table in the database.
 * It contains fields for user ID, username, password, email, and creation timestamp.
 *
 * <p>Implements the {@link UserDetails} interface for Spring Security integration,
 * providing authentication and authorization information for the user.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "user_id")
  private Long userId;

  @NotNull
  @Column(unique = true)
  private String username;

  @NotNull
  // The stored password is encoded so the column length is increased to accommodate.
  @Column(length = 512)
  private String password;

  @NotNull
  private String email;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  /**
   * The roles assigned to the user. The user can have multiple roles.
   * This mapping is stored in the 'user_role_junction' table to create a many-to-many relationship.
   */
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_role_junction",
      joinColumns = {@JoinColumn(name = "user_id")},
      inverseJoinColumns = {@JoinColumn(name = "role_id")}
  )
  private Set<Role> authorities;

  /**
   * Retrieves the authorities (roles) granted to the user.
   * This method is part of the {@link UserDetails} interface and is used by Spring Security.
   *
   * @return a collection of granted authorities
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return this.authorities;
  }

  /**
   * Retrieves the username of the user. This method is part of the {@link UserDetails} interface
   * and is used by Spring Security for authentication.
   *
   * @return the username of the user
   */
  @Override
  public String getUsername() {
    return this.username;
  }

  /**
   * Retrieves the password of the user. This method is part of the {@link UserDetails} interface
   * and is used by Spring Security for authentication.
   *
   * @return the password of the user
   */
  @Override
  public String getPassword() {
    return this.password;
  }

  /**
   * Overrides the {@link Object#toString()} method to avoid logging sensitive information,
   * such as the user's password. It returns the user's basic information, excluding sensitive data.
   *
   * @return a string representation of the user
   */
  @Override
  public String toString() {
    return "User("
        + "userId=" + userId
        + ", username=" + username
        + ", email=" + email
        + ", createdAt=" + createdAt
        + ", authorities=" + authorities
        + ")";
  }

  /**
   * Checks if the user has the specified role.
   *
   * @param role the role to check for
   * @return true if the user has the specified role, false otherwise
   */
  public boolean hasRole(String role) {
    return getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals(role));
  }
}