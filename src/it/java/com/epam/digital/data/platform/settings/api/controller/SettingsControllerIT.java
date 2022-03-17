/*
 * Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.settings.api.controller;

import static com.epam.digital.data.platform.settings.api.TestUtils.readClassPathResource;
import static com.epam.digital.data.platform.settings.api.utils.Header.X_ACCESS_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateOutputDto;
import com.epam.digital.data.platform.starter.kafka.config.properties.KafkaProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092",
    "port=9092"})
class SettingsControllerIT {

  private static final String BASE_URL = "/settings";
  private static final UUID SETTINGS_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
  private static final String EMAIL = "name@email.com";
  private static final String PHONE = "0000000000";

  private static String OFFICER_TOKEN;

  @Autowired
  MockMvc mockMvc;
  @Autowired
  KafkaProperties kafkaProperties;
  @Autowired
  ObjectMapper objectMapper;

  @MockBean
  ReplyingKafkaTemplate replyingKafkaTemplate;

  @Captor
  ArgumentCaptor<ProducerRecord> captor;

  @BeforeAll
  static void init() throws IOException {
    OFFICER_TOKEN = readClassPathResource("/officerToken.txt");
  }

  @Test
  void shouldReadDataThroughKafkaUsingRequestReplyPattern() throws Exception {

    // given
    var response = wrapToResponse(mockSettingsReadDto(), Status.SUCCESS);

    RequestReplyFuture<String, Request<SettingsReadDto>, String> replyFuture = new RequestReplyFuture<>();
    replyFuture.set(wrapToConsumerRecord(response));

    when(replyingKafkaTemplate.sendAndReceive((ProducerRecord) any())).thenReturn(replyFuture);

    // when
    mockMvc
        .perform(get(BASE_URL)
            .header(X_ACCESS_TOKEN.getHeaderName(), OFFICER_TOKEN))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.settings_id", is(SETTINGS_ID.toString())),
            jsonPath("$.e-mail", is(EMAIL)),
            jsonPath("$.phone", is(PHONE)));

    // then
    verify(replyingKafkaTemplate).sendAndReceive(captor.capture());
    var procedureRecord = captor.getValue();

    assertThat(procedureRecord.topic())
        .isEqualTo(kafkaProperties.getRequestReply().getTopics().get("read-settings").getRequest());

    assertThat(((Request) procedureRecord.value()).getSecurityContext().getAccessToken())
        .isEqualTo(OFFICER_TOKEN);
    assertThat(((Request) procedureRecord.value()).getPayload()).isNull();

    var headers = procedureRecord.headers().toArray();
    assertThat(headers).hasSize(1);
    assertThat(headers[0].key()).isEqualTo("kafka_replyTopic");
    assertThat(new String(headers[0].value(), StandardCharsets.UTF_8))
        .isEqualTo(kafkaProperties.getRequestReply().getTopics().get("read-settings").getReply());
  }

  @Test
  void unauthorizedIfAccessTokenAbsent() throws Exception {
    mockMvc
        .perform(get(BASE_URL))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldSendDataThroughKafkaUsingRequestReplyPattern() throws Exception {

    // given
    var response = wrapToResponse(mockSettingsUpdateOutputDto(), Status.SUCCESS);

    RequestReplyFuture<String, Request<SettingsUpdateInputDto>, String> replyFuture = new RequestReplyFuture<>();
    replyFuture.set(wrapToConsumerRecord(response));

    when(replyingKafkaTemplate.sendAndReceive((ProducerRecord) any())).thenReturn(replyFuture);

    // when
    mockMvc
        .perform(put(BASE_URL)
            .header(X_ACCESS_TOKEN.getHeaderName(), OFFICER_TOKEN)
            .content(objectMapper.writeValueAsString(mockSettingsUpdateInputDto()))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.settings_id", is(SETTINGS_ID.toString())));

    // then
    verify(replyingKafkaTemplate).sendAndReceive(captor.capture());
    var procedureRecord = captor.getValue();

    assertThat(procedureRecord.topic())
        .isEqualTo(kafkaProperties.getRequestReply().getTopics().get("update-settings").getRequest());

    assertThat(((Request) procedureRecord.value()).getSecurityContext().getAccessToken())
        .isEqualTo(OFFICER_TOKEN);
    assertThat(((Request) procedureRecord.value()).getPayload()).isEqualTo(mockSettingsUpdateInputDto());

    var headers = procedureRecord.headers().toArray();
    assertThat(headers).hasSize(1);
    assertThat(headers[0].key()).isEqualTo("kafka_replyTopic");
    assertThat(new String(headers[0].value(), StandardCharsets.UTF_8))
        .isEqualTo(kafkaProperties.getRequestReply().getTopics().get("update-settings").getReply());
  }

  @Test
  void returnTimeoutErrorWhenFutureDoesNotReturnAnyValue() throws Exception {
    
    // given
    var replyFuture = mock(RequestReplyFuture.class);
    when(replyFuture.get()).thenThrow(new RuntimeException());
    when(replyingKafkaTemplate.sendAndReceive((ProducerRecord) any())).thenReturn(replyFuture);
    
    // when - then
    mockMvc
        .perform(put(BASE_URL)
            .header(X_ACCESS_TOKEN.getHeaderName(), OFFICER_TOKEN)
            .content(objectMapper.writeValueAsString(mockSettingsUpdateInputDto()))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isInternalServerError(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.code", is("TIMEOUT_ERROR")));
  }

  private SettingsUpdateInputDto mockSettingsUpdateInputDto() {
    SettingsUpdateInputDto dto = new SettingsUpdateInputDto();
    dto.setEmail(EMAIL);
    dto.setPhone(PHONE);
    dto.setCommunicationAllowed(false);
    return dto;
  }

  private SettingsUpdateOutputDto mockSettingsUpdateOutputDto() {
    SettingsUpdateOutputDto dto = new SettingsUpdateOutputDto();
    dto.setSettingsId(SETTINGS_ID);
    return dto;
  }

  private SettingsReadDto mockSettingsReadDto() {
    SettingsReadDto dto = new SettingsReadDto();
    dto.setSettingsId(SETTINGS_ID);
    dto.setEmail(EMAIL);
    dto.setPhone(PHONE);
    dto.setCommunicationAllowed(false);
    return dto;
  }

  private <T> Response<T> wrapToResponse(T dto, Status status) {
    var response = new Response<T>();
    response.setPayload(dto);
    response.setStatus(status);
    return response;
  }

  private <T> ConsumerRecord<String, String> wrapToConsumerRecord(Response<T> response)
      throws JsonProcessingException {
    String responseStr = objectMapper.writeValueAsString(response);
    return new ConsumerRecord<>("topic", 1, 0, "key", responseStr);
  }
}
