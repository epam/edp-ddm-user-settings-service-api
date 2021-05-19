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

package com.epam.digital.data.platform.settings.api.controller;

import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.settings.api.config.TestBeansConfig;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsReadByKeycloakIdService;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsUpdateService;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateOutputDto;
import com.epam.digital.data.platform.starter.security.PermitAllWebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@TestPropertySource(properties = {"platform.security.enabled=false"})
@Import({TestBeansConfig.class, PermitAllWebSecurityConfig.class})
@ContextConfiguration
class SettingsControllerTest {

  private static final String BASE_URL = "/settings";

  private static final UUID SETTINGS_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
  private static final UUID KEYCLOAK_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
  private static final String EMAIL = "email@email.com";
  private static final String PHONE = "0000000000";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SettingsReadService readService;
  @MockBean
  private SettingsUpdateService updateService;
  @MockBean
  private SettingsReadByKeycloakIdService readByKeycloakIdService;

  @Test
  void expectUserSettingsReturnedFromKafka() throws Exception {
    var response = new Response<SettingsReadDto>();
    response.setStatus(Status.SUCCESS);
    var payload = new SettingsReadDto();
    payload.setSettingsId(SETTINGS_ID);
    payload.setEmail(EMAIL);
    payload.setPhone(PHONE);
    response.setPayload(payload);

    when(readService.request(any())).thenReturn(response);

    mockMvc
        .perform(get(BASE_URL))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                jsonPath("$.settings_id", is(SETTINGS_ID.toString())),
                jsonPath("$.e-mail", is(EMAIL)),
                jsonPath("$.phone", is(PHONE))));
  }

  @Test
  void expectReadSettingsByIdIsReturnedFromKafka() throws Exception {
    var response = new Response<SettingsReadDto>();
    response.setStatus(Status.SUCCESS);
    var payload = new SettingsReadDto();
    payload.setSettingsId(SETTINGS_ID);
    payload.setEmail(EMAIL);
    payload.setPhone(PHONE);
    response.setPayload(payload);

    when(readByKeycloakIdService.request(any())).thenReturn(response);

    mockMvc
        .perform(get(BASE_URL + "/" + KEYCLOAK_ID))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                jsonPath("$.settings_id", is(SETTINGS_ID.toString())),
                jsonPath("$.e-mail", is(EMAIL)),
                jsonPath("$.phone", is(PHONE))));
  }

  @Test
  void expectUserSettingsIdIsReturnedOnSuccessfulUpdate() throws Exception {
    var input = new SettingsUpdateInputDto();
    input.setEmail(EMAIL);
    input.setPhone(PHONE);

    var response = new Response<SettingsUpdateOutputDto>();
    response.setStatus(Status.SUCCESS);
    response.setPayload(new SettingsUpdateOutputDto(SETTINGS_ID));

    when(updateService.request(any())).thenReturn(response);

    String requestBodyJson = String.format("{\"e-mail\": \"%s\", \"phone\": \"%s\"}", EMAIL, PHONE);

    mockMvc
        .perform(put(BASE_URL).content(requestBodyJson).contentType(MediaType.APPLICATION_JSON))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                jsonPath("$.settings_id", is(SETTINGS_ID.toString()))));
  }
}
