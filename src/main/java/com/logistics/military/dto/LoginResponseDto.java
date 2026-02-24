package com.logistics.military.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing the response returned after a successful login.
 *
 * <p>This DTO contains basic user information along with role names needed by the frontend
 * for UI authorization. Sensitive data such as passwords, tokens, or internal role structures
 * are intentionally excluded.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
  private Long userId;
  private String username;
  private String email;
  private List<String> roles;
}

