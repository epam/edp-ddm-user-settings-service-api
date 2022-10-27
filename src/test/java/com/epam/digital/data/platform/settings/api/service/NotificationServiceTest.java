package com.epam.digital.data.platform.settings.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.epam.digital.data.platform.notification.dto.UserNotificationMessageDto;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.starter.notifications.facade.UserNotificationFacade;
import java.util.AbstractMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  private static final String APPLICATION_NAME = "application_name";
  private static final Channel EMAIL_CHANNEL = Channel.EMAIL;
  private static final Channel DIIA_CHANNEL = Channel.DIIA;
  private static final String EMAIL_ADDRESS = "test@email.addr";
  private static final String DIIA_ADDRESS = "test_diia_address";
  private static final String USER_ID = "";
  private static final String OTP_CODE = "654321";

  @Mock
  private UserNotificationFacade userNotificationFacade;
  private NotificationService notificationService;

  @BeforeEach
  public void beforeEach() {
    notificationService = new NotificationService(userNotificationFacade, APPLICATION_NAME);
  }

  @Test
  void shouldSendCorrectEmailNotificationDto() {

    notificationService.sendNotification(EMAIL_CHANNEL, EMAIL_ADDRESS, USER_ID, OTP_CODE);

    var captor = ArgumentCaptor.forClass(UserNotificationMessageDto.class);
    verify(userNotificationFacade).sendNotification(captor.capture());

    var notificationDto = captor.getValue();
    assertThat(notificationDto.getContext().getSystem()).isEqualTo("Low-code Platform");
    assertThat(notificationDto.getContext().getApplication()).isEqualTo(APPLICATION_NAME);

    assertThat(notificationDto.getNotification().getTemplateName()).isEqualTo(
        "channel-confirmation");
    assertThat(notificationDto.getNotification().isIgnoreChannelPreferences()).isTrue();

    assertThat(notificationDto.getRecipients()).hasSize(1);

    var recipient = notificationDto.getRecipients().get(0);
    assertThat((recipient.getId())).isEqualTo(USER_ID);
    assertThat(recipient.getChannels()).hasSize(1);
    assertThat(recipient.getParameters()).hasSize(1);

    var channel = recipient.getChannels().get(0);
    assertThat(channel.getChannel()).isEqualTo(EMAIL_CHANNEL.getValue());
    assertThat(channel.getEmail()).isEqualTo(EMAIL_ADDRESS);
    assertThat(channel.getRnokpp()).isNull();

    assertThat(recipient.getParameters()).containsOnly(
        new AbstractMap.SimpleEntry<>("verificationCode", OTP_CODE));
  }

  @Test
  void shouldSendCorrectDiiaNotificationDto() {

    notificationService.sendNotification(DIIA_CHANNEL, DIIA_ADDRESS, USER_ID, OTP_CODE);

    var captor = ArgumentCaptor.forClass(UserNotificationMessageDto.class);
    verify(userNotificationFacade).sendNotification(captor.capture());

    var notificationDto = captor.getValue();
    assertThat(notificationDto.getContext().getSystem()).isEqualTo("Low-code Platform");
    assertThat(notificationDto.getContext().getApplication()).isEqualTo(APPLICATION_NAME);

    assertThat(notificationDto.getNotification().getTemplateName()).isEqualTo(
        "channel-confirmation");
    assertThat(notificationDto.getNotification().isIgnoreChannelPreferences()).isTrue();

    assertThat(notificationDto.getRecipients()).hasSize(1);

    var recipient = notificationDto.getRecipients().get(0);
    assertThat((recipient.getId())).isEqualTo(USER_ID);
    assertThat(recipient.getChannels()).hasSize(1);
    assertThat(recipient.getParameters()).hasSize(1);

    var channel = recipient.getChannels().get(0);
    assertThat(channel.getChannel()).isEqualTo(DIIA_CHANNEL.getValue());
    assertThat(channel.getRnokpp()).isEqualTo(DIIA_ADDRESS);
    assertThat(channel.getEmail()).isNull();

    assertThat(recipient.getParameters()).containsOnly(
        new AbstractMap.SimpleEntry<>("verificationCode", OTP_CODE));
  }
}
