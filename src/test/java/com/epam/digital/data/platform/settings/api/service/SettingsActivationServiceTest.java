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
import com.epam.digital.data.platform.settings.api.model.NotificationChannel;
import com.epam.digital.data.platform.settings.api.model.Settings;
import com.epam.digital.data.platform.settings.api.repository.NotificationChannelRepository;
import com.epam.digital.data.platform.settings.api.repository.SettingsRepository;
import com.epam.digital.data.platform.settings.model.dto.ActivateEmailInputDto;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.SettingsEmailInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsDeactivateChannelInputDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsActivationServiceTest {

  private static final UUID TOKEN_SUBJECT_ID = UUID.fromString(
      "123e4567-e89b-12d3-a456-426655440000");
  private static final UUID SETTINGS_ID = UUID.fromString("321e7654-e89b-12d3-a456-426655441111");
  private static final UUID NOTIFICATION_CHANNEL_ID = UUID.fromString(
      "da8ee615-5de7-474d-a673-f83831c7f547");
  private static final String EMAIL = "email@email.com";
  private static final String DEACTIVATION_REASON = "User deactivated";

  private SettingsActivationService settingsActivationService;

  @Mock
  private SettingsRepository settingsRepository;
  @Mock
  private NotificationChannelRepository notificationChannelRepository;
  @Mock
  private JwtInfoProvider jwtInfoProvider;
  @Mock
  private SettingsAuditFacade auditFacade;
  @Mock
  private ChannelVerificationService channelVerificationService;

  @BeforeEach
  void beforeEach() {
    settingsActivationService = new SettingsActivationService(notificationChannelRepository,
        settingsRepository, jwtInfoProvider, auditFacade, channelVerificationService);

    when(jwtInfoProvider.getUserId(any())).thenReturn(TOKEN_SUBJECT_ID.toString());
  }

  @Test
  void expectUpdateDeactivatedEmailChannel() {
    var inputDto = new ActivateEmailInputDto();
    inputDto.setAddress("new@email.com");
    inputDto.setVerificationCode("123456");

    var settingsFromDb = new Settings();
    settingsFromDb.setId(SETTINGS_ID);
    settingsFromDb.setKeycloakId(TOKEN_SUBJECT_ID);

    var channelFromDb = new NotificationChannel();
    channelFromDb.setId(NOTIFICATION_CHANNEL_ID);
    channelFromDb.setChannel(Channel.EMAIL);
    channelFromDb.setActivated(false);
    channelFromDb.setAddress(EMAIL);
    channelFromDb.setDeactivationReason(DEACTIVATION_REASON);
    channelFromDb.setCreatedAt(LocalDateTime.MIN);
    channelFromDb.setUpdatedAt(LocalDateTime.MIN);

    when(settingsRepository.getByKeycloakId(TOKEN_SUBJECT_ID))
        .thenReturn(settingsFromDb);
    when(notificationChannelRepository.findBySettingsIdAndChannel(SETTINGS_ID,
        Channel.EMAIL)).thenReturn(Optional.of(channelFromDb));
    when(channelVerificationService.verify(Channel.EMAIL, "token", "123456", "new@email.com"))
        .thenReturn(true);

    settingsActivationService.activateEmail(inputDto, "token");

    verify(notificationChannelRepository)
        .activateChannel(eq(NOTIFICATION_CHANNEL_ID), eq("new@email.com"), any());
  }

  @Test
  void expectCreateActivatedEmailChannel() {
    var inputDto = new ActivateEmailInputDto();
    inputDto.setAddress("new@email.com");
    inputDto.setVerificationCode("123456");

    var settingsFromDb = new Settings();
    settingsFromDb.setId(SETTINGS_ID);
    settingsFromDb.setKeycloakId(TOKEN_SUBJECT_ID);

    when(settingsRepository.getByKeycloakId(TOKEN_SUBJECT_ID)).thenReturn(settingsFromDb);
    when(notificationChannelRepository.findBySettingsIdAndChannel(SETTINGS_ID, Channel.EMAIL))
        .thenReturn(Optional.empty());
    when(channelVerificationService.verify(Channel.EMAIL, "token", "123456", "new@email.com"))
        .thenReturn(true);

    settingsActivationService.activateEmail(inputDto, "token");

    verify(notificationChannelRepository)
        .create(SETTINGS_ID, Channel.EMAIL, "new@email.com", true, null);
  }

  @Test
  void expectUpdateDeactivatedDiiaChannel() {

    var settingsFromDb = new Settings();
    settingsFromDb.setId(SETTINGS_ID);
    settingsFromDb.setKeycloakId(TOKEN_SUBJECT_ID);

    var channelFromDb = new NotificationChannel();
    channelFromDb.setId(NOTIFICATION_CHANNEL_ID);
    channelFromDb.setChannel(Channel.DIIA);
    channelFromDb.setActivated(false);
    channelFromDb.setDeactivationReason(DEACTIVATION_REASON);
    channelFromDb.setCreatedAt(LocalDateTime.MIN);
    channelFromDb.setUpdatedAt(LocalDateTime.MIN);

    when(settingsRepository.getByKeycloakId(TOKEN_SUBJECT_ID))
            .thenReturn(settingsFromDb);
    when(notificationChannelRepository.findBySettingsIdAndChannel(SETTINGS_ID,
            Channel.DIIA)).thenReturn(Optional.of(channelFromDb));

    settingsActivationService.activateDiia("token");

    verify(notificationChannelRepository)
            .activateChannel(eq(NOTIFICATION_CHANNEL_ID), eq(null), any());
  }

  @Test
  void expectCreateActivatedDiiaChannel() {
    var settingsFromDb = new Settings();
    settingsFromDb.setId(SETTINGS_ID);
    settingsFromDb.setKeycloakId(TOKEN_SUBJECT_ID);

    when(settingsRepository.getByKeycloakId(TOKEN_SUBJECT_ID))
            .thenReturn(settingsFromDb);
    when(notificationChannelRepository.findBySettingsIdAndChannel(SETTINGS_ID, Channel.DIIA))
            .thenReturn(Optional.empty());

    settingsActivationService.activateDiia("token");

    verify(notificationChannelRepository)
            .create(SETTINGS_ID, Channel.DIIA, null, true, null);
  }

  @Test
  void expectDeactivatedExistingChannel() {
    var settingsFromDb = new Settings();
    settingsFromDb.setId(SETTINGS_ID);
    settingsFromDb.setKeycloakId(TOKEN_SUBJECT_ID);

    var channelFromDb = new NotificationChannel();
    channelFromDb.setId(NOTIFICATION_CHANNEL_ID);
    channelFromDb.setChannel(Channel.DIIA);
    channelFromDb.setActivated(true);
    channelFromDb.setCreatedAt(LocalDateTime.MIN);
    channelFromDb.setUpdatedAt(LocalDateTime.MIN);

    when(settingsRepository.getByKeycloakId(TOKEN_SUBJECT_ID)).thenReturn(settingsFromDb);
    when(notificationChannelRepository.findBySettingsIdAndChannel(SETTINGS_ID, Channel.DIIA))
        .thenReturn(Optional.of(channelFromDb));

    var input = new SettingsDeactivateChannelInputDto();
    input.setDeactivationReason(DEACTIVATION_REASON);

    settingsActivationService.deactivateChannel(Channel.DIIA, input, "token");

    verify(notificationChannelRepository).deactivateChannel(any(), eq("User deactivated"), any());
  }
}
