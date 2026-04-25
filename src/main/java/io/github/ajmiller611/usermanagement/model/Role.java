package io.github.ajmiller611.usermanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * Represents a role in the system with an associated authority.
 *
 * <p>This class implements {@link GrantedAuthority} and is used to define roles that users can have
 * in the system, such as "ADMIN", "USER", etc. It is mapped to the "roles" table in the database.
 * </p>
 *
 * <p>Each role has a unique identifier and an authority string, which represents the name
 * of the role. The authority is used by Spring Security for role-based access control.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role implements GrantedAuthority {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "role_id")
  @JsonIgnore
  private Integer roleId;

  private String authority;

  /**
   * Constructs a new Role with the specified authority.
   *
   * @param authority the authority name for the role.
   */
  public Role(String authority) {
    this.authority = authority;
  }

  /**
   * Returns the authority name of the role, which is used for security authorization.
   *
   * @return the authority name of the role.
   */
  @Override
  public String getAuthority() {
    return this.authority;
  }
}
