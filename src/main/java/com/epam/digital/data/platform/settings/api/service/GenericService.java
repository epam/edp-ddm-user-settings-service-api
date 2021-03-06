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

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.settings.api.exception.NoKafkaResponseException;
import com.epam.digital.data.platform.starter.kafka.config.properties.KafkaProperties.RequestReplyHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;

public abstract class GenericService<I, O> {

  private final Logger log = LoggerFactory.getLogger(GenericService.class);

  protected final ReplyingKafkaTemplate<String, Request<I>, String> replyingKafkaTemplate;
  protected final RequestReplyHandler topics;

  @Autowired
  private ObjectMapper objectMapper;

  protected GenericService(
      ReplyingKafkaTemplate<String, Request<I>, String> replyingKafkaTemplate,
      RequestReplyHandler topics) {
    this.replyingKafkaTemplate = replyingKafkaTemplate;
    this.topics = topics;
  }

  protected abstract TypeReference<Response<O>> type();

  public Response<O> request(Request<I> input) {
    ProducerRecord<String, Request<I>> request = new ProducerRecord<>(topics.getRequest(), input);
    RecordHeader header = new RecordHeader(KafkaHeaders.REPLY_TOPIC, topics.getReply().getBytes());
    request.headers().add(header);

    log.info("Sending event to Kafka");
    var replyFuture = replyingKafkaTemplate.sendAndReceive(request);

    ConsumerRecord<String, String> response;
    try {
      response = replyFuture.get();
      log.info("Successfully got response from Kafka");
    } catch (Exception e) {
      throw new NoKafkaResponseException("No response for request: " + input, e);
    }

    return fromString(response.value());
  }

  private Response<O> fromString(String content) {
    try {
      return objectMapper.readValue(content, type());
    } catch (JsonProcessingException e) {
      throw new RuntimeJsonMappingException(e.getMessage());
    }
  }
}
