package io.github.ajmiller611.usermanagement;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for verifying the database connection and schema setup.
 *
 * <p>This test class focuses on validating the connection to the database and ensuring that
 * the required tables are created during the test execution. These tests help ensure that
 * the basic database infrastructure is correctly set up for integration testing.</p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Database Connection:</strong> Ensures that the database is accessible by executing
 *     a simple query that verifies the connection is active and returns a valid result.
 *   </li>
 *   <li>
 *     <strong>Table Existence:</strong> Confirms that the required tables—{@code users},
 *     {@code roles}, and {@code user_role_junction}—exist in the database by querying
 *     the {@code information_schema.tables}.
 *   </li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("integration")
class IntegrationTests {

  @Autowired JdbcTemplate jdbcTemplate;

  /** Verify the database is connected. */
  @Test
  void givenDatabaseSetupWhenTestConnectionThenShouldReturnValidResult() {
    Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    assertTrue(result != null && result == 1, "Database connection test failed!");
  }

  /** Verify the tables are created. */
  @Test
  void givenDatabaseSchemaWhenCheckTablesExistenceThenShouldFindRequiredTables() {
    Integer result = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM information_schema.tables WHERE table_name = 'users'",
        Integer.class
    );
    assertTrue(result != null && result > 0,
        "User table does not exist.");

    result = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM information_schema.tables WHERE table_name = 'roles'",
        Integer.class
    );
    assertTrue(result != null && result > 0, "Role table does not exist.");

    result = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM information_schema.tables WHERE table_name = 'user_role_junction'",
        Integer.class
    );
    assertTrue(result != null && result > 0,
        "User_Role_Junction table does not exist.");
  }
}
