package com.epam.digital.data.platform.settings.api.service;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateOutputDto;
import com.epam.digital.data.platform.starter.restapi.config.properties.KafkaProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SettingsUpdateService
    extends GenericService<SettingsUpdateInputDto, SettingsUpdateOutputDto> {

  private static final String REQUEST_TYPE = "update-settings";

  protected SettingsUpdateService(
      ReplyingKafkaTemplate<
              String, Request<SettingsUpdateInputDto>, Response<SettingsUpdateOutputDto>>
          replyingKafkaTemplate,
      KafkaProperties kafkaProperties) {
    super(replyingKafkaTemplate, kafkaProperties.getTopics().get(REQUEST_TYPE));
  }
}
