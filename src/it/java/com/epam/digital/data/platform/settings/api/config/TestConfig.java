/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.settings.api.config;

import com.epam.digital.data.platform.settings.api.UserSettingsServiceApiApplication;
import com.epam.digital.data.platform.settings.api.service.ChannelVerificationService;
import com.epam.digital.data.platform.settings.api.service.SettingsActivationService;
import com.epam.digital.data.platform.settings.api.service.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.SettingsValidationService;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(UserSettingsServiceApiApplication.class)
public class TestConfig {

  @Bean
  public SettingsReadService testSettingsReadService() {
    return Mockito.mock(SettingsReadService.class);
  }

  @Bean
  public SettingsActivationService testSettingsActivationService() {
    return Mockito.mock(SettingsActivationService.class);
  }

  @Bean
  public SettingsValidationService testSettingsValidationService() {
    return Mockito.mock(SettingsValidationService.class);
  }

  @Bean
  public ChannelVerificationService testChannelVerificationService() {
    return Mockito.mock(ChannelVerificationService.class);
  }

}
