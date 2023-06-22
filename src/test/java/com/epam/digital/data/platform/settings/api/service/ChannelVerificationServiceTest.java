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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.notification.dto.Recipient;
import com.epam.digital.data.platform.settings.api.entity.OtpEntity;
import com.epam.digital.data.platform.settings.api.model.OtpData;
import com.epam.digital.data.platform.settings.api.repository.OtpRepository;
import com.epam.digital.data.platform.settings.api.service.impl.ChannelVerificationServiceImpl;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.VerificationInputDto;
import com.epam.digital.data.platform.starter.security.SystemRole;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ChannelVerificationServiceTest {

  private static final int OTP_TTL = 60;
  private static final String VALID_ACCESS_TOKEN = "valid_access_token";
  private static final String INVALID_ACCESS_TOKEN = "invalid_access_token";
  private static final String USER_ID = "user_id";
  private static final String USER_NAME = "user_name";
  private static final Channel EMAIL_CHANNEL = Channel.EMAIL;
  private static final String VALID_RECORD_KEY = String.format("%s/%s", USER_ID,
      EMAIL_CHANNEL.getValue());
  private static final String VALID_DIIA_RECORD_KEY = String.format("%s/%s", USER_ID,
      Channel.DIIA.getValue());
  private static final String VALID_EMAIL_ADDRESS = "test@email.addr";
  private static final String VALID_OTP_CODE = "654321";

  @Mock
  private OtpRepository repository;
  @Mock
  private JwtInfoProvider jwtInfoProvider;
  @Mock
  private VerificationCodeGenerator generator;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UserRoleVerifierService userRoleVerifierService;

  private ChannelVerificationService channelVerificationService;

  @BeforeEach
  public void beforeEach() {
    channelVerificationService = new ChannelVerificationServiceImpl(repository,
        jwtInfoProvider, generator, notificationService, userRoleVerifierService, OTP_TTL);

  }

  @Test
  void shouldSendVerificationCode() {
    Mockito.reset(repository);
    when(generator.generate()).thenReturn(VALID_OTP_CODE);
    when(jwtInfoProvider.getUsername(VALID_ACCESS_TOKEN)).thenReturn(USER_NAME);
    var inputDto = new VerificationInputDto();
    inputDto.setAddress(VALID_EMAIL_ADDRESS);
    when(userRoleVerifierService.verify(EMAIL_CHANNEL, VALID_ACCESS_TOKEN)).thenReturn(true);
    when(jwtInfoProvider.getUserRoles(VALID_ACCESS_TOKEN)).thenReturn(List.of(SystemRole.CITIZEN.getName()));
    when(jwtInfoProvider.getUserId(VALID_ACCESS_TOKEN)).thenReturn(USER_ID);

    var response = channelVerificationService.sendVerificationCode(EMAIL_CHANNEL,
        inputDto, VALID_ACCESS_TOKEN);

    assertThat(response.getVerificationCodeExpirationSec()).isEqualTo(OTP_TTL);

    var otpEntityCaptor = ArgumentCaptor.forClass(OtpEntity.class);
    verify(repository).save(otpEntityCaptor.capture());

    var otpEntity = otpEntityCaptor.getValue();
    assertThat(otpEntity.getId()).isEqualTo(VALID_RECORD_KEY);
    assertThat(otpEntity.getOtpData().getVerificationCode()).isEqualTo(VALID_OTP_CODE);
    assertThat(otpEntity.getOtpData().getAddress()).isEqualTo(VALID_EMAIL_ADDRESS);

    var channelCaptor = ArgumentCaptor.forClass(Channel.class);
    var addressCaptor = ArgumentCaptor.forClass(String.class);
    var usernameCaptor = ArgumentCaptor.forClass(String.class);
    var otpCodeCaptor = ArgumentCaptor.forClass(String.class);
    var realmCaptor = ArgumentCaptor.forClass(Recipient.RecipientRealm.class);
    verify(notificationService).sendNotification(channelCaptor.capture(), addressCaptor.capture(),
        usernameCaptor.capture(), otpCodeCaptor.capture(), realmCaptor.capture());

    assertThat(channelCaptor.getValue()).isEqualTo(EMAIL_CHANNEL);
    assertThat(addressCaptor.getValue()).isEqualTo(VALID_EMAIL_ADDRESS);
    assertThat(usernameCaptor.getValue()).isEqualTo(USER_NAME);
    assertThat(otpCodeCaptor.getValue()).isEqualTo(VALID_OTP_CODE);
    assertThat(realmCaptor.getValue()).isEqualTo(Recipient.RecipientRealm.CITIZEN);
  }

  @Test
  void shouldNotPassUserRoleVerification() {
    when(userRoleVerifierService.verify(EMAIL_CHANNEL, VALID_ACCESS_TOKEN)).thenReturn(false);

    Assertions.assertThatThrownBy(
            () ->
                channelVerificationService.sendVerificationCode(
                    EMAIL_CHANNEL, null, VALID_ACCESS_TOKEN))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Invalid user role for verify operation");
  }

  @Test
  void shouldVerifyCorrectCodeAndEmail() {
    var otpEntity = OtpEntity.builder()
        .id(VALID_RECORD_KEY)
        .otpData(new OtpData(VALID_EMAIL_ADDRESS, VALID_OTP_CODE))
        .build();
    when(repository.findById(VALID_RECORD_KEY)).thenReturn(Optional.of(otpEntity));
    when(jwtInfoProvider.getUserId(VALID_ACCESS_TOKEN)).thenReturn(USER_ID);

    var isValid = channelVerificationService.verify(EMAIL_CHANNEL, VALID_ACCESS_TOKEN,
        VALID_OTP_CODE, VALID_EMAIL_ADDRESS);

    assertThat(isValid).isTrue();
  }

  @Test
  void shouldNotVerifyIncorrectCodeAndCorrectEmail() {
    var otpEntity = OtpEntity.builder()
        .id(VALID_RECORD_KEY)
        .otpData(new OtpData(VALID_EMAIL_ADDRESS, VALID_OTP_CODE))
        .build();
    when(repository.findById(VALID_RECORD_KEY)).thenReturn(Optional.of(otpEntity));
    when(jwtInfoProvider.getUserId(VALID_ACCESS_TOKEN)).thenReturn(USER_ID);

    var isValid = channelVerificationService.verify(EMAIL_CHANNEL, VALID_ACCESS_TOKEN,
        "111111", VALID_EMAIL_ADDRESS);

    assertThat(isValid).isFalse();
  }

  @Test
  void shouldNotVerifyCorrectCodeAndIncorrectEmail() {
    var otpEntity = OtpEntity.builder()
        .id(VALID_RECORD_KEY)
        .otpData(new OtpData(VALID_EMAIL_ADDRESS, VALID_OTP_CODE))
        .build();
    when(repository.findById(VALID_RECORD_KEY)).thenReturn(Optional.of(otpEntity));
    when(jwtInfoProvider.getUserId(VALID_ACCESS_TOKEN)).thenReturn(USER_ID);

    var isValid = channelVerificationService.verify(EMAIL_CHANNEL, VALID_ACCESS_TOKEN,
        VALID_OTP_CODE, "invalid@email.addr");

    assertThat(isValid).isFalse();
  }

  @Test
  void shouldNotVerifyForIncorrectUser() {
    Mockito.reset(jwtInfoProvider);
    when(jwtInfoProvider.getUserId(INVALID_ACCESS_TOKEN)).thenReturn(EMPTY);
    when(repository.findById(anyString())).thenReturn(Optional.empty());

    var isValid = channelVerificationService.verify(EMAIL_CHANNEL, INVALID_ACCESS_TOKEN,
        VALID_OTP_CODE, VALID_EMAIL_ADDRESS);

    assertThat(isValid).isFalse();
  }

  @Test
  void shouldNotPassVerificationForDiiaChannelDifferentDrfos() {
    Mockito.reset(jwtInfoProvider);
    var drfo = "1234567891";
    when(jwtInfoProvider.getUserId(VALID_ACCESS_TOKEN)).thenReturn(USER_ID);
    when(jwtInfoProvider.getDrfo(VALID_ACCESS_TOKEN)).thenReturn("1111111111");
    var otpEntity = OtpEntity.builder()
        .id(VALID_DIIA_RECORD_KEY)
        .otpData(new OtpData(drfo, VALID_OTP_CODE))
        .build();
    when(repository.findById(VALID_DIIA_RECORD_KEY)).thenReturn(Optional.of(otpEntity));
    when(jwtInfoProvider.getUserId(VALID_ACCESS_TOKEN)).thenReturn(USER_ID);

    var isValid = channelVerificationService.verify(Channel.DIIA, VALID_ACCESS_TOKEN,
        VALID_OTP_CODE, drfo);

    assertThat(isValid).isFalse();
  }

  @Test
  void shouldPassVerificationForDiiaChannel() {
    Mockito.reset(jwtInfoProvider);
    var drfo = "1234567891";
    when(jwtInfoProvider.getUserId(VALID_ACCESS_TOKEN)).thenReturn(USER_ID);
    when(jwtInfoProvider.getDrfo(VALID_ACCESS_TOKEN)).thenReturn(drfo);
    var otpEntity = OtpEntity.builder()
        .id(VALID_DIIA_RECORD_KEY)
        .otpData(new OtpData(drfo, VALID_OTP_CODE))
        .build();
    when(repository.findById(VALID_DIIA_RECORD_KEY)).thenReturn(Optional.of(otpEntity));
    when(jwtInfoProvider.getUserId(VALID_ACCESS_TOKEN)).thenReturn(USER_ID);

    var isValid = channelVerificationService.verify(Channel.DIIA, VALID_ACCESS_TOKEN,
        VALID_OTP_CODE, drfo);

    assertThat(isValid).isTrue();
  }
}
