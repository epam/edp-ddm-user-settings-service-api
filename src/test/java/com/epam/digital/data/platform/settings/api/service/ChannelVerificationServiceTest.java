package com.epam.digital.data.platform.settings.api.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.settings.api.entity.OtpEntity;
import com.epam.digital.data.platform.settings.api.model.OtpData;
import com.epam.digital.data.platform.settings.api.repository.OtpRepository;
import com.epam.digital.data.platform.settings.api.service.impl.ChannelVerificationServiceImpl;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.VerificationInputDto;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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

  private ChannelVerificationService channelVerificationService;

  @BeforeEach
  public void beforeEach() {
    channelVerificationService = new ChannelVerificationServiceImpl(repository,
        jwtInfoProvider, generator, notificationService, OTP_TTL);
    when(jwtInfoProvider.getUserId(VALID_ACCESS_TOKEN)).thenReturn(USER_ID);
  }

  @Test
  void shouldSendVerificationCode() {
    Mockito.reset(repository);
    when(generator.generate()).thenReturn(VALID_OTP_CODE);
    when(jwtInfoProvider.getUsername(VALID_ACCESS_TOKEN)).thenReturn(USER_NAME);
    var inputDto = new VerificationInputDto();
    inputDto.setAddress(VALID_EMAIL_ADDRESS);

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
    verify(notificationService).sendNotification(channelCaptor.capture(), addressCaptor.capture(),
        usernameCaptor.capture(), otpCodeCaptor.capture());

    assertThat(channelCaptor.getValue()).isEqualTo(EMAIL_CHANNEL);
    assertThat(addressCaptor.getValue()).isEqualTo(VALID_EMAIL_ADDRESS);
    assertThat(usernameCaptor.getValue()).isEqualTo(USER_NAME);
    assertThat(otpCodeCaptor.getValue()).isEqualTo(VALID_OTP_CODE);
  }

  @Test
  void shouldVerifyCorrectCodeAndEmail() {
    var otpEntity = OtpEntity.builder()
        .id(VALID_RECORD_KEY)
        .otpData(new OtpData(VALID_EMAIL_ADDRESS, VALID_OTP_CODE))
        .build();
    when(repository.findById(VALID_RECORD_KEY)).thenReturn(Optional.of(otpEntity));

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
}
