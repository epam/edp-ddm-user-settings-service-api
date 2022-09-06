/*
 * Copyright 2022 EPAM Systems.
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
import com.epam.digital.data.platform.settings.api.model.Settings;
import com.epam.digital.data.platform.settings.api.repository.NotificationChannelRepository;
import com.epam.digital.data.platform.settings.api.repository.SettingsRepository;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.SettingsDeactivateChannelInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsEmailInputDto;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SettingsActivationService {

  private final Logger log = LoggerFactory.getLogger(SettingsActivationService.class);

  private final NotificationChannelRepository channelRepository;
  private final SettingsRepository settingsRepository;
  private final JwtInfoProvider jwtInfoProvider;
  private final SettingsAuditFacade auditFacade;

  public SettingsActivationService(
      NotificationChannelRepository channelRepository,
      SettingsRepository settingsRepository,
      JwtInfoProvider jwtInfoProvider,
      SettingsAuditFacade auditFacade) {
    this.channelRepository = channelRepository;
    this.settingsRepository = settingsRepository;
    this.jwtInfoProvider = jwtInfoProvider;
    this.auditFacade = auditFacade;
  }

  public void activateEmail(SettingsEmailInputDto input, String accessToken) {
    var settings = getSettingsFromToken(accessToken);
    var notificationChannel =
        channelRepository.findBySettingsIdAndChannel(settings.getId(), Channel.EMAIL);

    try {
      if (notificationChannel.isPresent()) {
        log.info("Activation of existing email channel");
        channelRepository.activateChannel(
            notificationChannel.get().getId(), input.getAddress(), LocalDateTime.now());
      } else {
        log.info("Creation of activated email channel");
        channelRepository.create(settings.getId(), Channel.EMAIL, input.getAddress(), true, null);
      }
      auditFacade.sendActivationAuditOnSuccess(Channel.EMAIL, input);
    } catch (RuntimeException exception) {
      auditFacade.sendActivationAuditOnFailure(Channel.EMAIL, input, exception.getMessage());
      throw exception;
    }
  }

  public void activateDiia(String accessToken) {
    var settings = getSettingsFromToken(accessToken);
    var notificationChannel =
        channelRepository.findBySettingsIdAndChannel(settings.getId(), Channel.DIIA);

    try {
      if (notificationChannel.isPresent()) {
        log.info("Activation of existing diia channel");
        channelRepository.activateChannel(
            notificationChannel.get().getId(), null, LocalDateTime.now());
      } else {
        log.info("Creation of activated diia channel");
        channelRepository.create(settings.getId(), Channel.DIIA, null, true, null);
      }
      auditFacade.sendActivationAuditOnSuccess(Channel.DIIA, null);
    } catch (RuntimeException exception) {
      auditFacade.sendActivationAuditOnFailure(Channel.DIIA, null, exception.getMessage());
      throw exception;
    }
  }

  public void deactivateChannel(
      Channel channel, SettingsDeactivateChannelInputDto input, String accessToken) {
    var settings = getSettingsFromToken(accessToken);
    var notificationChannel =
        channelRepository.findBySettingsIdAndChannel(settings.getId(), channel);
    if (notificationChannel.isPresent()) {
      log.info("Deactivation of existing channel {}", channel);
      try {
        channelRepository.deactivateChannel(
            notificationChannel.get().getId(), input.getDeactivationReason(), LocalDateTime.now());
        auditFacade.sendDeactivationAuditOnSuccess(channel, notificationChannel.get().getAddress(),
            input);
      } catch (RuntimeException exception) {
        auditFacade.sendDeactivationAuditOnFailure(channel, notificationChannel.get().getAddress(),
            input, exception.getMessage());
        throw exception;
      }
    }
  }

  private Settings getSettingsFromToken(String accessToken) {
    var userKeycloakId = jwtInfoProvider.getUserId(accessToken);
    return settingsRepository.getByKeycloakId(UUID.fromString(userKeycloakId));
  }
}
