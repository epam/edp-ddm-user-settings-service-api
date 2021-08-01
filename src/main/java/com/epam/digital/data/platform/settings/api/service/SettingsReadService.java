package com.epam.digital.data.platform.settings.api.service;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import com.epam.digital.data.platform.starter.restapi.config.properties.KafkaProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SettingsReadService extends GenericService<Void, SettingsReadDto> {

  private static final String REQUEST_TYPE = "read-settings";

  protected SettingsReadService(
      ReplyingKafkaTemplate<String, Request<Void>, Response<SettingsReadDto>> replyingKafkaTemplate,
      KafkaProperties kafkaProperties) {
    super(replyingKafkaTemplate, kafkaProperties.getTopics().get(REQUEST_TYPE));
  }
}
