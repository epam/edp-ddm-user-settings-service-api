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

import com.epam.digital.data.platform.settings.api.repository.NotificationChannelRepository;
import com.epam.digital.data.platform.settings.api.repository.SettingsRepository;
import com.epam.digital.data.platform.settings.model.dto.ChannelReadDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SettingsReadService {

  private final SettingsRepository settingsRepository;
  private final NotificationChannelRepository notificationChannelRepository;
  private final JwtInfoProvider jwtInfoProvider;

  public SettingsReadService(
      SettingsRepository settingsRepository,
      NotificationChannelRepository notificationChannelRepository,
      JwtInfoProvider jwtInfoProvider) {
    this.settingsRepository = settingsRepository;
    this.notificationChannelRepository = notificationChannelRepository;
    this.jwtInfoProvider = jwtInfoProvider;
  }

  public SettingsReadDto findSettingsFromUserToken(String accessToken) {
    var userKeycloakId = jwtInfoProvider.getUserId(accessToken);
    return findSettingsByUserId(UUID.fromString(userKeycloakId));
  }

  public SettingsReadDto findSettingsByUserId(UUID userId) {
    var settings = settingsRepository.getByKeycloakId(userId);
    var readDtoChannels =
        notificationChannelRepository.findBySettingsId(settings.getId()).stream()
            .map(
                notificationChannel -> {
                  var readDtoChannel = new ChannelReadDto();
                  readDtoChannel.setChannel(notificationChannel.getChannel());
                  readDtoChannel.setActivated(notificationChannel.isActivated());
                  readDtoChannel.setAddress(notificationChannel.getAddress());
                  readDtoChannel.setDeactivationReason(notificationChannel.getDeactivationReason());
                  return readDtoChannel;
                })
            .collect(Collectors.toList());
    return new SettingsReadDto(settings.getId(), readDtoChannels);
  }
}
