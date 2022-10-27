/*
 *  Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.settings.api.interceptor;

import static com.epam.digital.data.platform.settings.api.utils.Header.X_ACCESS_TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.epam.digital.data.platform.settings.api.config.WebConfig;
import com.epam.digital.data.platform.settings.api.controller.SettingsController;
import com.epam.digital.data.platform.settings.api.service.SettingsActivationService;
import com.epam.digital.data.platform.settings.api.service.ChannelVerificationService;
import com.epam.digital.data.platform.settings.api.service.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.SettingsValidationService;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import com.epam.digital.data.platform.starter.actuator.livenessprobe.LivenessStateHandler;
import com.epam.digital.data.platform.starter.security.PermitAllWebSecurityConfig;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.epam.digital.data.platform.starter.security.jwt.TokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

@WebMvcTest
@ContextConfiguration(
    classes = {SettingsController.class, LivenessProbeStateInterceptor.class, WebConfig.class,
    TokenProvider.class, TokenParser.class})
@Import({PermitAllWebSecurityConfig.class})
class LivenessProbeStateInterceptorTest {

  private static final String BASE_URL = "/api/settings/me";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SettingsReadService settingsReadService;
  @MockBean
  private LivenessStateHandler livenessStateHandler;
  @MockBean
  private SettingsActivationService settingsActivationService;
  @MockBean
  private SettingsValidationService settingsValidationService;
  @MockBean
  private ChannelVerificationService channelVerificationFacade;

  @Test
  void expectStateHandlerIsCalledInInterceptor() throws Exception {
    when(settingsReadService.findSettingsFromUserToken(any()))
        .thenReturn(new SettingsReadDto(UUID.randomUUID()));

    mockMvc.perform(get(BASE_URL).header(X_ACCESS_TOKEN.getHeaderName(), "token"));

    verify(livenessStateHandler).handleResponse(eq(HttpStatus.OK), any());
  }
}
