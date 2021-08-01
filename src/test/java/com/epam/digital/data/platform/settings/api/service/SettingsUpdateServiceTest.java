package com.epam.digital.data.platform.settings.api.service;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateOutputDto;
import com.epam.digital.data.platform.starter.restapi.config.properties.KafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SettingsUpdateService.class)
@EnableConfigurationProperties({KafkaProperties.class})
class SettingsUpdateServiceTest {

  private static final UUID SETTINGS_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  @MockBean
  private ReplyingKafkaTemplate<
          String, Request<SettingsUpdateInputDto>, Response<SettingsUpdateOutputDto>>
      replyingKafkaTemplate;

  @Autowired
  private SettingsUpdateService settingsUpdateService;

  @Captor
  private ArgumentCaptor<ProducerRecord<String, Request<SettingsUpdateInputDto>>> producerRecord;

  @Captor
  private ArgumentCaptor<
          RequestReplyFuture<
              String, Request<SettingsUpdateInputDto>, Response<SettingsUpdateOutputDto>>>
      reply;

  @Test
  void shouldWriteToKafkaCorrectMessageBody() {
    SettingsUpdateInputDto mock = new SettingsUpdateInputDto();
    mock.setEmail("");
    Request<SettingsUpdateInputDto> request = getRequest(mock);

    SettingsUpdateOutputDto responsePayload = new SettingsUpdateOutputDto(SETTINGS_ID);

    RequestReplyFuture<String, Request<SettingsUpdateInputDto>, Response<SettingsUpdateOutputDto>>
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

  private Request<SettingsUpdateInputDto> getRequest(
      SettingsUpdateInputDto settingsUpdateInputDto) {
    var request = new Request<SettingsUpdateInputDto>();
    request.setPayload(settingsUpdateInputDto);
    return request;
  }

  private <I, O> RequestReplyFuture<String, I, Response<O>> wrapResponseObjectAsKafkaReplay(
      I input, O output) {
    return wrapResponseObjectAsKafkaReplayWithStatus(input, output, Status.SUCCESS);
  }

  private <I, O>
      RequestReplyFuture<String, I, Response<O>> wrapResponseObjectAsKafkaReplayWithStatus(
          I input, O output, Status status) {
    RequestReplyFuture<String, I, Response<O>> replyFuture = new RequestReplyFuture<>();
    Response<O> responseWrapper = new Response<>();
    responseWrapper.setPayload(output);
    responseWrapper.setStatus(status);
    ConsumerRecord<String, Response<O>> responseRecord =
        new ConsumerRecord<>("out", 0, 0, null, responseWrapper);
    replyFuture.set(responseRecord);
    return replyFuture;
  }
}
