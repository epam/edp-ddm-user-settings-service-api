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

package com.epam.digital.data.platform.settings.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.settings.api.config.TestBeansConfig;
import com.epam.digital.data.platform.settings.api.exception.NoKafkaResponseException;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsUpdateService;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateOutputDto;
import com.epam.digital.data.platform.starter.restapi.config.properties.KafkaProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;

@SpringBootTest(classes = SettingsUpdateService.class)
@Import(TestBeansConfig.class)
@EnableConfigurationProperties({KafkaProperties.class})
class GenericServiceTest {

  private static final UUID SETTINGS_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  @MockBean
  private ReplyingKafkaTemplate<String, Request<SettingsUpdateInputDto>, String>
      replyingKafkaTemplate;

  @Autowired
  private SettingsUpdateService settingsUpdateService;

  @Captor
  private ArgumentCaptor<ProducerRecord<String, Request<SettingsUpdateInputDto>>> producerRecord;

  @Captor
  private ArgumentCaptor<RequestReplyFuture<String, Request<SettingsUpdateInputDto>, String>>
      reply;

  @Test
  void shouldWriteToKafkaCorrectMessageBody() {
    SettingsUpdateInputDto mock = new SettingsUpdateInputDto();
    mock.setEmail("");
    Request<SettingsUpdateInputDto> request = getRequest(mock);

    SettingsUpdateOutputDto responsePayload = new SettingsUpdateOutputDto(SETTINGS_ID);

    RequestReplyFuture<String, Request<SettingsUpdateInputDto>, String>
        replyFuture =
            wrapResponseObjectAsKafkaReplay(request, responsePayload);
    when(replyingKafkaTemplate.sendAndReceive(any())).thenReturn(replyFuture);

    Response<SettingsUpdateOutputDto> actual = settingsUpdateService.request(request);

    assertThat(actual.getPayload()).isEqualTo(responsePayload);

    verify(replyingKafkaTemplate).sendAndReceive(producerRecord.capture());
    var requestValue = producerRecord.getValue().value();
    assertThat(requestValue.getPayload()).isEqualTo(mock);
    assertThat(requestValue.getSecurityContext()).isNull();
    assertThat(requestValue.getRequestContext()).isNull();
  }

  @Test
  void shouldThrowExceptionWhenTimeout() throws ExecutionException, InterruptedException {
    RequestReplyFuture mockReplyFuture = Mockito.mock(RequestReplyFuture.class);
    when(replyingKafkaTemplate.sendAndReceive(any())).thenReturn(mockReplyFuture);

    Mockito.doThrow(new InterruptedException()).when(mockReplyFuture).get();

    Exception exception = assertThrows(NoKafkaResponseException.class, () -> {
      settingsUpdateService.request(getRequest(new SettingsUpdateInputDto()));
    });

    assertThat(exception.getCause()).isInstanceOf(InterruptedException.class);
  }

  @Test
  void shouldThrowExceptionWhenInvalidJson() throws ExecutionException, InterruptedException {
    Request<SettingsUpdateInputDto> request = getRequest(new SettingsUpdateInputDto());

    RequestReplyFuture<String, Request<SettingsUpdateInputDto>, String>
        replyFuture =
        wrapResponseObjectAsKafkaReplay(request, "invalid json");
    when(replyingKafkaTemplate.sendAndReceive(any())).thenReturn(replyFuture);

    Exception exception = assertThrows(RuntimeJsonMappingException.class, () -> {
      settingsUpdateService.request(request);
    });
  }

  private Request<SettingsUpdateInputDto> getRequest(
      SettingsUpdateInputDto settingsUpdateInputDto) {
    var request = new Request<SettingsUpdateInputDto>();
    request.setPayload(settingsUpdateInputDto);
    return request;
  }

  private <I, O> RequestReplyFuture<String, I, String> wrapResponseObjectAsKafkaReplay(
      I input, O output) {
    return wrapResponseObjectAsKafkaReplayWithStatus(input, output, Status.SUCCESS);
  }

  private <I, O>
      RequestReplyFuture<String, I, String> wrapResponseObjectAsKafkaReplayWithStatus(
          I input, O output, Status status) {
    Response<O> responseWrapper = new Response<>();
    responseWrapper.setPayload(output);
    responseWrapper.setStatus(status);

    ConsumerRecord<String, String> responseRecord =
        new ConsumerRecord<>("out", 0, 0, null, toJsonStr(responseWrapper));

    RequestReplyFuture<String, I, String> replyFuture = new RequestReplyFuture<>();
    replyFuture.set(responseRecord);
    return replyFuture;
  }

  private <O> String toJsonStr(O obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}

