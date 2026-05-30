package io.github.ajmiller611.usermanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ajmiller611.usermanagement.dto.InvitationRequestDto;
import io.github.ajmiller611.usermanagement.dto.InvitationValidationResponseDto;
import io.github.ajmiller611.usermanagement.model.Invitation;
import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.repository.InvitationRepository;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link InvitationService} class.
 *
 * <p>This test class verifies the functionality in the {@link InvitationService},
 * focusing on database interactions.</p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Invitation Creation:</strong> Ensures {@code createInvitation()}
 *     creates the invitation and saves it to the database properly.
 *   </li>
 *   <li>
 *     <strong>Invitation Validation:</strong> Validates a valid token is found in the database
 *     and correct details are in the response dto.
 *   </li>
 *   <li>
 *     <strong>Mark Invitation as Used:</strong> Ensures an invitation is updated successfully
 *     when marked as used.
 *   </li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class InvitationServiceIntegrationTests {

  @Autowired private InvitationService invitationService;
  @Autowired private InvitationRepository invitationRepository;
  @Autowired private RoleRepository roleRepository;

  Role role;

  @BeforeEach
  void setUp() {
    // Roles already exist in the database due to bootstrap service running on startup
    role = roleRepository.findByAuthority("USER").orElseThrow();
  }

  /** Verifies {@code createInvitation()} creates the invitation and saves it. */
  @Test
  void givenValidRequestWhenCreateInvitationThenReturnInvitationResponseDto() {
    InvitationRequestDto invitationRequestDto = new InvitationRequestDto(
        "test@email.com",
        "USER"
    );

    invitationService.createInvitation(invitationRequestDto);

    Invitation invitation =
        invitationRepository.findByEmail(invitationRequestDto.getEmail()).orElseThrow();

    assertNotNull(invitation, "The invitation should not be null");
    assertNotNull(invitation.getId(), "The invitation id should not be null");
    assertNotNull(invitation.getToken(), "The invitation token should not be null");
    assertEquals(
        invitationRequestDto.getEmail(),
        invitation.getEmail(),
        "The invitation email should be the same"
    );
    assertEquals(
        invitationRequestDto.getRole(),
        invitation.getRole().getAuthority(),
        "The invitation role should be the same"
    );
    assertTrue(
        invitation.getCreatedAt().isBefore(LocalDateTime.now()),
        "The invitation createdAt timestamp should be before this assertion");


    assertTrue(
        invitation.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6).plusHours(23)),
        "The expiresAt timestamp should be 7 days after the createdAt timestamp"
    );
  }

  /** Verifies {@code validateInvitation()} returns the details when a valid token is given. */
  @Test
  void givenValidTokenWhenValidateInvitationThenReturnInvitationValidationResponseDto() {
    Invitation invitation = new Invitation(
        null,
        "test@email.com",
        "token",
        role,
        LocalDateTime.now(),
        LocalDateTime.now().plusDays(7),
        false
    );
    invitationRepository.save(invitation);

    InvitationValidationResponseDto responseDto =
        invitationService.validateInvitation(invitation.getToken());

    assertNotNull(responseDto, "The response should not be null");
    assertEquals(invitation.getEmail(), responseDto.getEmail(), "The email should be the same");
    assertEquals(
        invitation.getRole().getAuthority(),
        responseDto.getRole(),
        "The role should be the same"
    );
    assertTrue(
        responseDto.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6).plusHours(23)),
        "The expiresAt timestamp should be 7 days after the createdAt timestamp"
    );
  }

  /** Verifies {@code markInvitationAsUsed} updates the invitation and the change is saved. */
  @Test
  void givenInvitationWhenMarkInvitationAsUsedThenConfirmInvitationAsUsed() {
    Invitation invitation = new Invitation(
        null,
        "test@email.com",
        "token",
        role,
        LocalDateTime.now(),
        LocalDateTime.now().plusDays(7),
        false
    );
    Invitation saved = invitationRepository.save(invitation);

    invitationService.markInvitationAsUsed(saved);

    Invitation updated = invitationRepository.findById(saved.getId()).orElseThrow();

    assertTrue(updated.isUsed());
  }
}
