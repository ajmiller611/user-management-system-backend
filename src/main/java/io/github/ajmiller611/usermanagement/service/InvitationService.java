package io.github.ajmiller611.usermanagement.service;

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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing invitation-related operations,
 * including invitation creation, validation, and lifecycle management.
 */
@Service
@RequiredArgsConstructor
public class InvitationService {

  private final Logger logger = LoggerFactory.getLogger(InvitationService.class);
  private final InvitationRepository invitationRepository;
  private final InvitationTokenService invitationTokenService;
  private final RoleRepository roleRepository;
  private final Clock clock;

  /**
   * Creates a new invitation for a user and persists it to the database.
   *
   * <p>A secure token is generated and associated with the invitation.
   * The invitation is assigned a role and expires after a fixed duration.</p>
   *
   * @param invitationRequestDto the {@link InvitationRequestDto} containing email and role
   * @return a {@link InvitationResponseDto} containing the invitation token, email,
   *         and expiration date
   */
  public InvitationResponseDto createInvitation(InvitationRequestDto invitationRequestDto) {
    logger.info("Create invitation request with DTO: {}", invitationRequestDto);

    Role role = roleRepository.findByAuthority(invitationRequestDto.getRole())
        .orElseThrow(
            () -> new RoleNotFoundException(invitationRequestDto.getRole() + " not found")
        );

    String token = invitationTokenService.generateInvitationToken();

    LocalDateTime createdAt = LocalDateTime.now(clock);
    LocalDateTime expiresAt = createdAt.plusDays(7);

    Invitation invitation = new Invitation();
    invitation.setEmail(invitationRequestDto.getEmail());
    invitation.setToken(token);
    invitation.setRole(role);
    invitation.setCreatedAt(createdAt);
    invitation.setExpiresAt(expiresAt);
    invitation.setUsed(false);
    invitation = invitationRepository.save(invitation);

    logger.info("Invitation created with ID: {}", invitation.getId());
    return new InvitationResponseDto(
        invitation.getToken(),
        invitation.getEmail(),
        invitation.getExpiresAt()
    );
  }

  /**
   * Validates that a token is associated with a valid invitation.
   *
   * <p>If the invitation does not exist, is expired, or has already been used,
   * an exception is thrown.</p>
   *
   * @param token the token associated with the {@link Invitation}
   * @return a {@link InvitationValidationResponseDto} containing validated invitation details
   */
  public InvitationValidationResponseDto validateInvitation(String token) {
    Invitation invitation = getValidInvitation(token);

    return new InvitationValidationResponseDto(
        invitation.getEmail(),
        invitation.getRole().getAuthority(),
        invitation.getExpiresAt()
    );
  }

  /**
   * Marks an invitation as used and persists the updated invitation state.
   *
   * @param invitation the {@link Invitation} to mark as used
   */
  public void markInvitationAsUsed(Invitation invitation) {
    invitation.setUsed(true);
    invitationRepository.save(invitation);
  }

  /**
   * Retrieves a valid invitation or throws an exception if the invitation does not exist,
   * is used, or is expired.
   *
   * @param token the token associated with the {@link Invitation}
   * @return a valid {@link Invitation}
   */
  public Invitation getValidInvitation(String token) {
    Invitation invitation = invitationRepository.findByToken(token)
        .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));

    if (invitation.isUsed()) {
      throw new InvitationAlreadyUsedException("Invitation is already used");
    }

    if (invitation.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
      throw new InvitationExpiredException("Invitation is expired");
    }

    return invitation;
  }
}
