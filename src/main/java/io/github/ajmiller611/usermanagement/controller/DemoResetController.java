package io.github.ajmiller611.usermanagement.controller;

import io.github.ajmiller611.usermanagement.service.DemoResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for managing demo data operations.
 *
 * <p>This controller provides administrative endpoints for resetting
 * demo data within the system. It is intended for use in development
 * and demonstration environments only.</p>
 *
 * <p>Access to all endpoints in this controller is restricted to users
 * with the ADMIN role.</p>
 */
@RestController
@RequestMapping("/admin/demo")
@RequiredArgsConstructor
public class DemoResetController {

  private final DemoResetService demoResetService;

  /**
   * Resets the application's demo data to its initial state.
   *
   * <p>This operation clears and reinitializes demo-related data
   * by delegating to the {@link DemoResetService}.</p>
   *
   * @return a {@link ResponseEntity} containing a success message
   *         confirming the demo data has been reset
   */
  @PostMapping("/reset")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<String> resetDemo() {
    demoResetService.resetDemoData();
    return ResponseEntity.ok("Demo data reset successfully.");
  }
}
