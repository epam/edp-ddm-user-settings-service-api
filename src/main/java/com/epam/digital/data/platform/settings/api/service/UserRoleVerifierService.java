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

package com.epam.digital.data.platform.settings.api.service;

import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.starter.security.SystemRole;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRoleVerifierService {

  private final JwtInfoProvider jwtInfoProvider;

  public boolean verify(Channel channel, String accessToken) {
    List<String> userRoles = jwtInfoProvider.getUserRoles(accessToken);
    if (Objects.isNull(userRoles)) {
      return false;
    }
    if (Channel.DIIA.equals(channel)) {
      if (CollectionUtils.containsAny(
          userRoles, SystemRole.OFFICER.getName(), SystemRole.UNREGISTERED_OFFICER.getName())) {
        return false;
      }
    }
    return true;
  }
}
