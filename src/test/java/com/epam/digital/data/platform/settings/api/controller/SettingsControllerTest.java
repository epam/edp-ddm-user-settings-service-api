/*
 *  Copyright 2023 EPAM Systems.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.digital.data.platform.settings.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.settings.api.config.TestBeansConfig;
import com.epam.digital.data.platform.settings.api.service.ChannelVerificationService;
import com.epam.digital.data.platform.settings.api.service.SettingsActivationService;
import com.epam.digital.data.platform.settings.api.service.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.SettingsValidationService;
import com.epam.digital.data.platform.settings.api.utils.Header;
import com.epam.digital.data.platform.settings.model.dto.ActivateChannelInputDto;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.ChannelReadDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsDeactivateChannelInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsEmailInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import com.epam.digital.data.platform.settings.model.dto.VerificationCodeExpirationDto;
import com.epam.digital.data.platform.settings.model.dto.VerificationInputDto;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
import com.epam.digital.data.platform.starter.security.PermitAllWebSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@TestPropertySource(properties = {"platform.security.enabled=false"})
@Import({TestBeansConfig.class, PermitAllWebSecurityConfig.class})
@ContextConfiguration
class SettingsControllerTest {

  private static final String BASE_URL = "/api/settings";

  private static final UUID SETTINGS_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
  private static final UUID KEYCLOAK_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
  private static final String EMAIL = "email@email.com";

  private static final String TOKEN = "token";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private SettingsReadService settingsReadService;
  @MockBean
  private SettingsActivationService settingsActivationService;
  @MockBean
  private SettingsValidationService settingsValidationService;
  @MockBean
  private MessageResolver messageResolver;
  @MockBean
  private ChannelVerificationService channelVerificationService;

  @Test
  void expectControllerReturnSettingsFromToken() throws Exception {
    var payload = new SettingsReadDto(SETTINGS_ID);
    when(settingsReadService.findSettingsFromUserToken(any())).thenReturn(payload);

    mockMvc
        .perform(get(BASE_URL + "/me").header(Header.X_ACCESS_TOKEN.getHeaderName(), TOKEN))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.settingsId", is(SETTINGS_ID.toString())),
            jsonPath("$.channels", is(Collections.emptyList())));
  }

  @Test
  void expectControllerReturnSettingsByUserId() throws Exception {
    var channelDto = new ChannelReadDto();
    channelDto.setChannel(Channel.EMAIL);
    channelDto.setActivated(true);
    channelDto.setAddress(EMAIL);
    var payload = new SettingsReadDto(SETTINGS_ID, Collections.singletonList(channelDto));

    when(settingsReadService.findSettingsByUserId(KEYCLOAK_ID)).thenReturn(payload);

    mockMvc
        .perform(get(BASE_URL + "/" + KEYCLOAK_ID))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.settingsId", is(SETTINGS_ID.toString())),
            jsonPath("$.channels[0].channel", is(Channel.EMAIL.getValue())),
            jsonPath("$.channels[0].activated", is(true)),
            jsonPath("$.channels[0].address", is(EMAIL)),
            jsonPath("$.channels[0].deactivationReason").doesNotExist());
  }

  @Test
  void expectControllerActivateEmailChannel() throws Exception {
    var payload = new ActivateChannelInputDto();
    payload.setAddress(EMAIL);
    payload.setVerificationCode("123456");

    mockMvc
        .perform(post(BASE_URL + "/me/channels/email/activate").header(
                Header.X_ACCESS_TOKEN.getHeaderName(), TOKEN)
            .content(objectMapper.writeValueAsString(payload))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isOk());

    var captor = ArgumentCaptor.forClass(ActivateChannelInputDto.class);
    verify(settingsActivationService).activateChannel(captor.capture(), eq(Channel.EMAIL), eq(TOKEN));
    assertThat(captor.getValue().getAddress()).isEqualTo(EMAIL);
  }

  @Test
  void expectControllerActivateDiiaChannel() throws Exception {
    var payload = new ActivateChannelInputDto();
    payload.setAddress("2222222222");
    payload.setVerificationCode("123456");
    mockMvc
        .perform(
            post(BASE_URL + "/me/channels/diia/activate")
                .header(Header.X_ACCESS_TOKEN.getHeaderName(), TOKEN)
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(status().isOk());

    var captor = ArgumentCaptor.forClass(ActivateChannelInputDto.class);
    verify(settingsActivationService).activateChannel(captor.capture(), eq(Channel.DIIA), eq(TOKEN));
    var capturedPayload = captor.getValue();
    assertThat(capturedPayload.getAddress()).isEqualTo(payload.getAddress());
    assertThat(capturedPayload.getVerificationCode()).isEqualTo(payload.getVerificationCode());
  }

  @Test
  void expectControllerDeactivateChannel() throws Exception {
    var payload = new SettingsDeactivateChannelInputDto();
    payload.setDeactivationReason("Reason");

    mockMvc
        .perform(
            post(BASE_URL + "/me/channels/diia/deactivate")
                .header(Header.X_ACCESS_TOKEN.getHeaderName(), TOKEN)
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(status().isOk());

    var captor = ArgumentCaptor.forClass(SettingsDeactivateChannelInputDto.class);
    verify(settingsActivationService)
        .deactivateChannel(eq(Channel.DIIA), captor.capture(), eq(TOKEN));
    assertThat(captor.getValue().getDeactivationReason()).isEqualTo("Reason");
  }

  @Test
  void expectControllerVerifyEmailChannel() throws Exception {
    var request = new SettingsEmailInputDto();
    request.setAddress(EMAIL);
    when(channelVerificationService.sendVerificationCode(any(Channel.class), any(
        VerificationInputDto.class), anyString()))
        .thenReturn(new VerificationCodeExpirationDto(60));

    mockMvc
        .perform(
            post(BASE_URL + "/me/channels/email/verify")
                .header(Header.X_ACCESS_TOKEN.getHeaderName(), TOKEN)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(status().isAccepted(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.verificationCodeExpirationSec", is(60)));

    verify(channelVerificationService).sendVerificationCode(eq(Channel.EMAIL),
        argThat(dto -> EMAIL.equals(dto.getAddress())), eq(TOKEN));
  }
}
