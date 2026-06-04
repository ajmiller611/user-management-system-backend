package io.github.ajmiller611.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.dto.InvitationRequestDto;
import io.github.ajmiller611.usermanagement.dto.InvitationResponseDto;
import io.github.ajmiller611.usermanagement.dto.InvitationValidationResponseDto;
import io.github.ajmiller611.usermanagement.exception.InvitationAlreadyUsedException;
import io.github.ajmiller611.usermanagement.exception.InvitationExpiredException;
import io.github.ajmiller611.usermanagement.exception.InvitationNotFoundException;
import io.github.ajmiller611.usermanagement.exception.RoleNotFoundException;
import io.github.ajmiller611.usermanagement.model.Invitation;
import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.repository.InvitationRepository;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link InvitationService} class.
 *
 * <p>This test class focuses on verifying the core functionality of the {@link InvitationService},
 * including invitation creation, token validation, and marking an invitation as used.</p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Invitation Creation:</strong> Ensures that a valid {@link InvitationRequestDto}
 *     results in an {@link Invitation} entity being correctly constructed,
 *     passed to the repository, and mapped to an {@link InvitationResponseDto}.
 *   </li>
 *   <li>
 *     <strong>Invitation Validation:</strong> Ensures that an invitation token is validated
 *     against existence, expiration, and usage status, and that appropriate exceptions are thrown
 *     for invalid cases.
 *   </li>
 *   <li>
 *     <strong>Mark Invitation as Used:</strong> Ensures an {@link Invitation}'s used flag
 *     is updated and persisted via the repository.
 *   </li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class InvitationServiceTests {

  @InjectMocks private InvitationService invitationService;
  @Mock private InvitationRepository invitationRepository;
  @Mock private InvitationTokenService invitationTokenService;
  @Mock private RoleRepository roleRepository;
  @Mock private Clock clock;

  LocalDateTime fixedTimestamp = LocalDateTime.of(2026, 5, 29, 0, 0, 0, 0);
  Clock fixedClock =
      Clock.fixed(
          fixedTimestamp.atZone(ZoneId.systemDefault()).toInstant(),
          ZoneId.systemDefault());

  InvitationRequestDto invitationRequestDto;
  Invitation invitation;
  Role role;

  String expectedEmail;
  String expectedToken;
  Role expectedRole;
  LocalDateTime expectedCreatedAt;
  LocalDateTime expectedExpiredAt;

  @BeforeEach
  void setUp() {
    role = new Role("USER");

    invitationRequestDto = new InvitationRequestDto(
        "test@email.com",
        "USER"
    );

    expectedEmail = invitationRequestDto.getEmail();
    expectedToken = "token";
    expectedRole = role;
    expectedCreatedAt = fixedTimestamp;
    expectedExpiredAt = expectedCreatedAt.plusDays(7);

    invitation = new Invitation(
        1L,
        invitationRequestDto.getEmail(),
        "token",
        role,
        fixedTimestamp,
        fixedTimestamp.plusDays(7),
        false
    );
  }

  /**
   * Verifies a valid request creates the invitation entity and saves it to the database.
   * Also, checks that the response includes the correct information.
   */
  @Test
  void givenValidRequestWhenCreateInvitationThenReturnInvitationResponseDto() {
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(roleRepository.findByAuthority(invitationRequestDto.getRole()))
        .thenReturn(Optional.of(role));
    when(invitationTokenService.generateInvitationToken()).thenReturn("token");
    when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);

    ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);

    try (LogCaptor logCaptor = LogCaptor.forClass(InvitationService.class)) {
      final InvitationResponseDto responseDto =
          invitationService.createInvitation(invitationRequestDto);

      verify(roleRepository).findByAuthority("USER");
      verify(invitationTokenService).generateInvitationToken();
      verify(invitationRepository, times(1)).save(captor.capture());

      Invitation saved = captor.getValue();

      assertNotNull(responseDto, "Response should not be null");
      assertEquals(expectedEmail, saved.getEmail(), "Email should be set");
      assertEquals(expectedToken, saved.getToken(), "Token should be set");
      assertEquals(expectedRole, saved.getRole(), "Role should be set");
      assertEquals(expectedCreatedAt, saved.getCreatedAt(), "Created at should be set");
      assertEquals(expectedExpiredAt, saved.getExpiresAt(), "Expires at should be set");
      assertFalse(saved.isUsed(), "Marked should be false");

      assertEquals(expectedToken, responseDto.getToken(), "Response should have the token");
      assertEquals(expectedEmail, responseDto.getEmail(), "Response should have the email");
      assertEquals(
          expectedExpiredAt,
          responseDto.getExpiresAt(),
          "Response should have the expires at"
      );

      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Expected log message for create invitation request with DTO details")
          .anyMatch(log ->
              log.contains("Create invitation request with DTO: " + invitationRequestDto.toString())
          );

      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Expected log message for after invitation creation with ID")
          .anyMatch(
              log -> log.contains("Invitation created with ID: ")
          );
    }
  }

  /** Verifies a {@link RoleNotFoundException} is thrown when the {@link Role} does not exist. */
  @Test
  void givenInvalidRoleWhenCreateInvitationThenThrowRoleNotFoundException() {
    when(roleRepository.findByAuthority(invitationRequestDto.getRole()))
        .thenReturn(Optional.empty());

    assertThrows(RoleNotFoundException.class,
        () -> invitationService.createInvitation(invitationRequestDto),
        "Expected createInvitation to throw an exception when role is not found");
  }

  /** Verifies a valid token responds with the needed details in the response dto. */
  @Test
  void givenValidTokenWhenValidateInvitationThenReturnValidationResponseDto() {
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(invitationRepository.findByToken(expectedToken)).thenReturn(Optional.of(invitation));

    InvitationValidationResponseDto responseDto =
        invitationService.validateInvitation(expectedToken);

    assertNotNull(responseDto, "Response should not be null");
    assertEquals(expectedEmail, responseDto.getEmail(), "Email should be set");
    assertEquals(expectedRole.getAuthority(), responseDto.getRole(), "Role should be set");
    assertEquals(expectedExpiredAt, responseDto.getExpiresAt(), "Expired at should be set");
  }

  /** Verifies a valid token returns the associated invitation. */
  @Test
  void givenValidTokenWhenGetValidInvitationThenReturnInvitation() {
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(invitationRepository.findByToken(expectedToken)).thenReturn(Optional.of(invitation));

    Invitation result =
        invitationService.getValidInvitation(expectedToken);

    verify(invitationRepository, times(1)).findByToken(expectedToken);
    assertNotNull(result, "Response should not be null");
    assertEquals(expectedEmail, result.getEmail(), "Email should be set");
    assertEquals(expectedToken, result.getToken(), "Token should be set");
    assertEquals(expectedRole, result.getRole(), "Role should be set");
    assertEquals(expectedCreatedAt, result.getCreatedAt(), "Created at should be set");
    assertEquals(expectedExpiredAt, result.getExpiresAt(), "Expired at should be set");
    assertFalse(result.isUsed(), "Marked should be false");
  }

  /**
   * Verifies a {@link InvitationNotFoundException} is thrown when validating a token
   * that is not associated with an existing {@link Invitation}.
   */
  @Test
  void givenNonExistentInvitationWhenGetValidInvitationThenThrowInvitationNotFoundException() {
    when(invitationRepository.findByToken(expectedToken)).thenReturn(Optional.empty());

    assertThrows(InvitationNotFoundException.class,
        () -> invitationService.getValidInvitation(expectedToken),
        "Expected getValidInvitation to throw an exception when invitation is not found");
  }

  /**
   * Verifies a {@link InvitationExpiredException} is thrown when validating
   * an expired {@link Invitation}.
   */
  @Test
  void givenExpiredInvitationWhenGetValidInvitationThenThrowInvitationExpiredException() {
    invitation = new Invitation(
        1L,
        invitationRequestDto.getEmail(),
        "token",
        role,
        fixedTimestamp,
        fixedTimestamp.minusDays(1),
        false
    );
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(invitationRepository.findByToken(expectedToken)).thenReturn(Optional.of(invitation));

    assertThrows(InvitationExpiredException.class,
        () -> invitationService.getValidInvitation(expectedToken),
        "Expected getValidInvitation to throw an exception when invitation is expired");
  }

  /**
   * Verifies a {@link InvitationAlreadyUsedException} is thrown when validating
   * an {@link Invitation} that has been used already.
   */
  @Test
  void givenAlreadyUsedInvitationWhenGetValidInvitationThenThrowInvitationAlreadyUsedException() {
    invitation = new Invitation(
        1L,
        invitationRequestDto.getEmail(),
        "token",
        role,
        fixedTimestamp,
        fixedTimestamp.plusDays(7),
        true
    );
    when(invitationRepository.findByToken(expectedToken)).thenReturn(Optional.of(invitation));

    assertThrows(InvitationAlreadyUsedException.class,
        () -> invitationService.getValidInvitation(expectedToken),
        "Expected getValidInvitation to throw an exception when invitation is already used");
  }

  /** Verify an {@link Invitation} is updated and marked as used. */
  @Test
  void givenInvitationWhenMarkInvitationAsUsedThenUpdateInvitationAsUsed() {
    ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
    invitationService.markInvitationAsUsed(invitation);

    verify(invitationRepository, times(1)).save(captor.capture());

    Invitation saved = captor.getValue();

    assertTrue(saved.isUsed());
  }
}
