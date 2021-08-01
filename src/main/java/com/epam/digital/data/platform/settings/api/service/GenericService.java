package com.epam.digital.data.platform.settings.api.service;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.settings.api.exception.NoKafkaResponseException;
import com.epam.digital.data.platform.starter.restapi.config.properties.KafkaProperties.Handler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;

public abstract class GenericService<I, O> {

  protected final ReplyingKafkaTemplate<String, Request<I>, Response<O>> replyingKafkaTemplate;
  protected final Handler topics;

  protected GenericService(
      ReplyingKafkaTemplate<String, Request<I>, Response<O>> replyingKafkaTemplate,
      Handler topics) {
    this.replyingKafkaTemplate = replyingKafkaTemplate;
    this.topics = topics;
  }

  public Response<O> request(Request<I> input) {
    ProducerRecord<String, Request<I>> request = new ProducerRecord<>(topics.getRequest(), input);
    RecordHeader header = new RecordHeader(KafkaHeaders.REPLY_TOPIC, topics.getReplay().getBytes());
    request.headers().add(header);
    var replyFuture = replyingKafkaTemplate.sendAndReceive(request);
    ConsumerRecord<String, Response<O>> response;
    try {
      response = replyFuture.get();
    } catch (Exception e) {
      throw new NoKafkaResponseException("No response for request: " + input, e);
    }
    return response.value();
  }
}
