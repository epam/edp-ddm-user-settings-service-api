/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.settings.api.service;

import com.epam.digital.data.platform.settings.api.audit.SettingsAuditFacade;
import com.epam.digital.data.platform.settings.api.exception.ChannelVerificationException;
import com.epam.digital.data.platform.settings.api.model.Settings;
import com.epam.digital.data.platform.settings.api.repository.NotificationChannelRepository;
import com.epam.digital.data.platform.settings.api.repository.SettingsRepository;
import com.epam.digital.data.platform.settings.model.dto.ActivateChannelInputDto;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.SettingsDeactivateChannelInputDto;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class SettingsActivationService {

  private final Logger log = LoggerFactory.getLogger(SettingsActivationService.class);

  private final NotificationChannelRepository channelRepository;
  private final SettingsRepository settingsRepository;
  private final JwtInfoProvider jwtInfoProvider;
  private final SettingsAuditFacade auditFacade;
  private final ChannelVerificationService channelVerificationService;
  private final UserRoleVerifierService userRoleVerifierService;

  public SettingsActivationService(
      NotificationChannelRepository channelRepository,
      SettingsRepository settingsRepository,
      JwtInfoProvider jwtInfoProvider,
      SettingsAuditFacade auditFacade,
      ChannelVerificationService channelVerificationService,
      UserRoleVerifierService userRoleVerifierService) {
    this.channelRepository = channelRepository;
    this.settingsRepository = settingsRepository;
    this.jwtInfoProvider = jwtInfoProvider;
    this.auditFacade = auditFacade;
    this.channelVerificationService = channelVerificationService;
    this.userRoleVerifierService = userRoleVerifierService;
  }

  public void activateChannel(ActivateChannelInputDto input, Channel channel, String accessToken) {
    if (!userRoleVerifierService.verify(channel, accessToken)) {
      auditFacade.sendActivationAuditOnFailure(channel, input, "User role verification failed");
      throw new AccessDeniedException("Invalid user role for activate operation");
    }

    boolean successfullyVerified =
        channelVerificationService.verify(
            channel, accessToken, input.getVerificationCode(), input.getAddress());
    if (!successfullyVerified) {
      auditFacade.sendActivationAuditOnFailure(
          channel, input, "Communication channel verification failed");
      throw new ChannelVerificationException("Communication channel verification failed");
    }
    var settings = getSettingsFromToken(accessToken);
    var notificationChannel =
        channelRepository.findBySettingsIdAndChannel(settings.getId(), channel);

    try {
      if (notificationChannel.isPresent()) {
        log.info("Activation of existing {} channel", channel.getValue());
        channelRepository.activateChannel(
            notificationChannel.get().getId(), input.getAddress(), LocalDateTime.now());
      } else {
        log.info("Creation of activated {} channel", channel.getValue());
        channelRepository.create(settings.getId(), channel, input.getAddress(), true, null);
      }
      auditFacade.sendActivationAuditOnSuccess(channel, input);
    } catch (RuntimeException exception) {
      auditFacade.sendActivationAuditOnFailure(channel, input, exception.getMessage());
      throw exception;
    }
  }

  public void deactivateChannel(
      Channel channel, SettingsDeactivateChannelInputDto input, String accessToken) {

    if (!userRoleVerifierService.verify(channel, accessToken)) {
      auditFacade.sendDeactivationAuditOnFailure(channel, input, "User role verification failed");
      throw new AccessDeniedException("Invalid user role for deactivate operation");
    }

    var settings = getSettingsFromToken(accessToken);
    var notificationChannel =
        channelRepository.findBySettingsIdAndChannel(settings.getId(), channel);
    try {
      if (notificationChannel.isPresent()) {
        log.info("Deactivation of existing channel {}", channel);
        var address =
            input.getAddress() == null
                ? notificationChannel.get().getAddress()
                : input.getAddress();
        channelRepository.deactivateChannel(
            notificationChannel.get().getId(),
            address,
            input.getDeactivationReason(),
            LocalDateTime.now());
      } else {
        log.info("Creation of deactivated {} channel", channel);
        channelRepository.create(
            settings.getId(), channel, input.getAddress(), false, input.getDeactivationReason());
      }
      auditFacade.sendDeactivationAuditOnSuccess(channel, input);
    } catch (RuntimeException exception) {
      auditFacade.sendDeactivationAuditOnFailure(channel, input, exception.getMessage());
      throw exception;
    }
  }

  private Settings getSettingsFromToken(String accessToken) {
    var userKeycloakId = jwtInfoProvider.getUserId(accessToken);
    return settingsRepository.getByKeycloakId(UUID.fromString(userKeycloakId));
  }
}
