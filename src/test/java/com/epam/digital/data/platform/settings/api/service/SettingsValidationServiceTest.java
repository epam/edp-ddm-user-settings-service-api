/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.settings.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epam.digital.data.platform.settings.api.exception.EmailAddressValidationException;
import com.epam.digital.data.platform.settings.model.dto.SettingsEmailInputDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingsValidationServiceTest {

  private SettingsValidationService settingsValidationService;

  @BeforeEach
  void beforeEach() {
   settingsValidationService = new SettingsValidationService();
  }

  @Test
  void expectValidationException() {
    var input = new SettingsEmailInputDto();
    input.setAddress(".settings2@yahoo.com");

    var ex = assertThrows(EmailAddressValidationException.class,
        () -> settingsValidationService.validateEmailAddress(input));

    assertEquals("Email address is not valid", ex.getMessage());
  }

  @Test
  void expectEmptyAddressException() {
    var input = new SettingsEmailInputDto();
    input.setAddress("");

    var ex = assertThrows(EmailAddressValidationException.class,
        () -> settingsValidationService.validateEmailAddress(input));

    assertEquals("Email address is empty", ex.getMessage());
  }

  @Test
  void expectPassValidation() {
    var input = new SettingsEmailInputDto();
    input.setAddress("email@gmail.com");
    var result = settingsValidationService.validateEmailAddress(input);
    assertTrue(result);
  }

  @Test
  void expectValidationExceptionWhenNoDotBeforeTopLevelDomain() {
    var input = new SettingsEmailInputDto();
    input.setAddress("settings2@yahoo");

    var ex = assertThrows(EmailAddressValidationException.class,
        () -> settingsValidationService.validateEmailAddress(input));

    assertEquals("Email address is not valid", ex.getMessage());
  }
}
