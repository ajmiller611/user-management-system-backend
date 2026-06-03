package io.github.ajmiller611.usermanagement.controller;

import io.github.ajmiller611.usermanagement.dto.InvitationRequestDto;
import io.github.ajmiller611.usermanagement.dto.InvitationResponseDto;
import io.github.ajmiller611.usermanagement.dto.InvitationValidationResponseDto;
import io.github.ajmiller611.usermanagement.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling invitation-related HTTP requests.
 *
 * <p>This controller exposes endpoints for creating an invitation (ADMIN access only) and
 * validating an invitation via a token. It also facilitates interaction with the
 * {@link InvitationService} for invitation-related operations.</p>
 */
@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@Validated
public class InvitationController {

  private final Logger logger = LoggerFactory.getLogger(InvitationController.class);
  private final InvitationService invitationService;

  /**
   * Creates a new invitation in the system.
   *
   * <p>This endpoint receives a {@link InvitationRequestDto} object containing the email and role
   * for the new user. It delegates invitation creation to the {@link InvitationService} and,
   * upon successful creation, returns a response with the token and metadata.
   * </p>
   *
   * @param invitationRequestDto the {@link InvitationRequestDto} containing the email and role
   * @return a {@link ResponseEntity} containing a {@link InvitationResponseDto} containing the
   *         token and metadata
   */
  @PostMapping({"/", ""})
  public ResponseEntity<InvitationResponseDto> createInvitation(
      @Valid @RequestBody InvitationRequestDto invitationRequestDto
  ) {
    logger.info("Endpoint /invitations received POST request: {}", invitationRequestDto);
    InvitationResponseDto invitationResponseDto =
        invitationService.createInvitation(invitationRequestDto);

    ResponseEntity<InvitationResponseDto> response =
        ResponseEntity.status(HttpStatus.CREATED).body(invitationResponseDto);
    logger.info("Invitation created successfully for email: {}", invitationResponseDto.getEmail());
    return response;
  }

  /**
   * Validates an invitation.
   *
   * <p>This endpoint receives a token associated with an invitation to check its validation.
   * It delegates the validation to the {@link InvitationService} and, if valid, returns a response
   * with email and role of the invitation.</p>
   *
   * @param token a token associated with an invitation
   * @return a {@link ResponseEntity} containing a {@link InvitationValidationResponseDto} with
   *         the email and role
   */
  @GetMapping("/validate")
  public ResponseEntity<InvitationValidationResponseDto> validateInvitation(
      @RequestParam String token
  ) {
    logger.info("Endpoint /invitations/validate received GET request");

    InvitationValidationResponseDto response =
        invitationService.validateInvitation(token);

    logger.info("Invitation validated successfully for email: {}", response.getEmail());
    return ResponseEntity.ok(response);
  }
}
