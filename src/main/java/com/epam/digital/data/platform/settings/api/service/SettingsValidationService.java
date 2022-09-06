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

import com.epam.digital.data.platform.settings.api.exception.EmailAddressValidationException;
import com.epam.digital.data.platform.settings.model.dto.SettingsEmailInputDto;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SettingsValidationService {

  private static final String EMAIL_REGEX = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+(?:\\.[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)+$";
  private static final String EMAIL_ADDRESS_NOT_VALID_KEY = "ERROR_EMAIL_ADDRESS_NOT_VALID";
  private static final String EMAIL_ADDRESS_EMPTY_KEY = "ERROR_EMAIL_ADDRESS_EMPTY";

  public boolean validateEmailAddress(SettingsEmailInputDto input) {
    var address = input.getAddress();
    if (Objects.isNull(address) || address.isEmpty()) {
      throw new EmailAddressValidationException("Email address is empty", EMAIL_ADDRESS_EMPTY_KEY);
    }
    if (!address.matches(EMAIL_REGEX)) {
      throw new EmailAddressValidationException("Email address is not valid", EMAIL_ADDRESS_NOT_VALID_KEY);
    }
    return true;
  }
}
