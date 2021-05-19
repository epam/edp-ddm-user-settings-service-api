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

package com.epam.digital.data.platform.settings.api.service.impl;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.settings.api.service.GenericService;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadByKeycloakIdInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import com.epam.digital.data.platform.starter.kafka.config.properties.KafkaProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SettingsReadByKeycloakIdService
    extends GenericService<SettingsReadByKeycloakIdInputDto, SettingsReadDto> {

  private static final String REQUEST_TYPE = "read-settings-by-keycloak-id";

  protected SettingsReadByKeycloakIdService(
      ReplyingKafkaTemplate<String, Request<SettingsReadByKeycloakIdInputDto>, String>
          replyingKafkaTemplate,
      KafkaProperties kafkaProperties) {
    super(replyingKafkaTemplate, kafkaProperties.getRequestReply().getTopics().get(REQUEST_TYPE));
  }

  @Override
  protected TypeReference<Response<SettingsReadDto>> type() {
    return new TypeReference<>() {};
  }
}
