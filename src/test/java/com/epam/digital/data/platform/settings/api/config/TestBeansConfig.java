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

package com.epam.digital.data.platform.settings.api.config;

import static org.mockito.Mockito.mock;

import com.epam.digital.data.platform.settings.api.audit.SettingsAuditFacade;
import com.epam.digital.data.platform.settings.api.service.TraceService;
import com.epam.digital.data.platform.starter.actuator.livenessprobe.LivenessStateHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestBeansConfig {

  @Bean
  public LivenessStateHandler livenessStateHandler() {
    return mock(LivenessStateHandler.class);
  }

  @Bean
  public TraceService logService() {
    return mock(TraceService.class);
  }

  @Bean
  public SettingsAuditFacade settingsAuditFacade() {
    return mock(SettingsAuditFacade.class);
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
