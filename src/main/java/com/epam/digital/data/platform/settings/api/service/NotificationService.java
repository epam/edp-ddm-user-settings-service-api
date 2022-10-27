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

import com.epam.digital.data.platform.notification.dto.ChannelObject;
import com.epam.digital.data.platform.notification.dto.NotificationContextDto;
import com.epam.digital.data.platform.notification.dto.Recipient;
import com.epam.digital.data.platform.notification.dto.UserNotificationDto;
import com.epam.digital.data.platform.notification.dto.UserNotificationMessageDto;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.starter.notifications.facade.UserNotificationFacade;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

  private final Map<Channel, String> subjects =
      Map.of(Channel.EMAIL, "Підтвердження електронної пошти",
          Channel.DIIA, "Підтвердження каналу зв'язку реєстру");
  private final UserNotificationFacade notificationFacade;
  private final String applicationName;

  public NotificationService(UserNotificationFacade notificationFacade,
      @Value("${spring.application.name}") String applicationName) {
    this.notificationFacade = notificationFacade;
    this.applicationName = applicationName;
  }

  public void sendNotification(Channel channel, String address, String username, String otpCode) {
    List<Recipient> recipients = List.of(Recipient.builder()
        .id(username)
        .channels(List.of(getChannelObject(channel, address)))
        .parameters(Map.of("verificationCode", otpCode))
        .build());

    var notificationMessageDto = UserNotificationMessageDto
        .builder()
        .context(NotificationContextDto.builder()
            .system("Low-code Platform")
            .application(applicationName)
            .build())
        .notification(UserNotificationDto.builder()
            .title(subjects.get(channel))
            .templateName("channel-confirmation")
            .ignoreChannelPreferences(true)
            .build())
        .recipients(recipients)
        .build();

    notificationFacade.sendNotification(notificationMessageDto);
  }

  private ChannelObject getChannelObject(Channel channel, String address) {
    var builder = ChannelObject.builder().channel(channel.getValue());
    if (channel.equals(Channel.EMAIL)) {
      builder.email(address);
    } else if (channel.equals(Channel.DIIA)) {
      builder.rnokpp(address);
    }
    return builder.build();
  }
}
