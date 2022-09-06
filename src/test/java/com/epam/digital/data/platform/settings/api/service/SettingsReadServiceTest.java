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

import com.epam.digital.data.platform.settings.api.model.NotificationChannel;
import com.epam.digital.data.platform.settings.api.model.Settings;
import com.epam.digital.data.platform.settings.api.repository.NotificationChannelRepository;
import com.epam.digital.data.platform.settings.api.repository.SettingsRepository;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsReadServiceTest {

  private static final UUID SETTINGS_ID = UUID.fromString("321e7654-e89b-12d3-a456-426655441111");
  private static final UUID NOTIFICATION_CHANNEL_ID = UUID.fromString("da8ee615-5de7-474d-a673-f83831c7f547");
  private static final UUID TOKEN_SUBJECT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
  private static final String EMAIL = "email@email.com";
  private static final String UPDATED_EMAIL = "email@email.com";

  private static final String DEACTIVATION_REASON = "User deactivated";

  private SettingsReadService settingsReadService;

  @Mock
  private JwtInfoProvider jwtInfoProvider;
  @Mock
  private SettingsRepository settingsRepository;
  @Mock
  private NotificationChannelRepository notificationChannelRepository;

  @Captor
  private ArgumentCaptor<Settings> updatedSettingsCaptor;

  @BeforeEach
  void beforeEach() {
    settingsReadService = new SettingsReadService(settingsRepository, notificationChannelRepository, jwtInfoProvider);

    when(jwtInfoProvider.getUserId(any())).thenReturn(TOKEN_SUBJECT_ID.toString());
  }

  @Test
  void expectUserSettingsFromDbReturnedIfExist() {
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

    when(settingsRepository.getByKeycloakId(TOKEN_SUBJECT_ID)).thenReturn(settingsFromDb);
    when(notificationChannelRepository.findBySettingsId(SETTINGS_ID))
        .thenReturn(Collections.singletonList(channelFromDb));

    var actual = settingsReadService.findSettingsFromUserToken("token");

    assertThat(actual.getSettingsId()).isEqualTo(settingsFromDb.getId());
    assertThat(actual.getChannels()).hasSize(1);
    assertThat(actual.getChannels().get(0).getChannel()).isEqualTo(Channel.EMAIL);
    assertThat(actual.getChannels().get(0).isActivated()).isFalse();
    assertThat(actual.getChannels().get(0).getAddress()).isEqualTo(EMAIL);
    assertThat(actual.getChannels().get(0).getDeactivationReason()).isEqualTo(DEACTIVATION_REASON);
  }
}

