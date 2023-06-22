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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.starter.security.SystemRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRoleVerifierServiceTest {

  private UserRoleVerifierService userRoleVerifierService;
  @Mock
  private JwtInfoProvider jwtInfoProvider;

  private static final String VALID_ACCESS_TOKEN = "valid_access_token";

  @BeforeEach
  public void beforeAll() {
    userRoleVerifierService = new UserRoleVerifierService(jwtInfoProvider);
  }

  @Test
  void shouldPassUserRoleVerification() {
    when(jwtInfoProvider.getUserRoles(VALID_ACCESS_TOKEN))
        .thenReturn(List.of(SystemRole.CITIZEN.getName()));

    boolean result = userRoleVerifierService.verify(Channel.DIIA, VALID_ACCESS_TOKEN);

    assertThat(result).isTrue();
    Mockito.verify(jwtInfoProvider).getUserRoles(VALID_ACCESS_TOKEN);
  }

  @Test
  void shouldPassUserRoleVerificationWithChannelEmail() {
    when(jwtInfoProvider.getUserRoles(VALID_ACCESS_TOKEN))
        .thenReturn(List.of(SystemRole.OFFICER.getName()));

    boolean result = userRoleVerifierService.verify(Channel.EMAIL, VALID_ACCESS_TOKEN);

    assertThat(result).isTrue();
    Mockito.verify(jwtInfoProvider).getUserRoles(VALID_ACCESS_TOKEN);
  }

  @Test
  void shouldNotPassUserRoleVerification() {
    when(jwtInfoProvider.getUserRoles(VALID_ACCESS_TOKEN))
        .thenReturn(List.of(SystemRole.OFFICER.getName()));

    boolean result = userRoleVerifierService.verify(Channel.DIIA, VALID_ACCESS_TOKEN);
    assertThat(result).isFalse();
    Mockito.verify(jwtInfoProvider).getUserRoles(VALID_ACCESS_TOKEN);
  }

  @Test
  void shouldNotPassUserRoleVerificationWithNoRoles() {
    when(jwtInfoProvider.getUserRoles(VALID_ACCESS_TOKEN)).thenReturn(null);

    boolean result = userRoleVerifierService.verify(Channel.DIIA, VALID_ACCESS_TOKEN);

    assertThat(result).isFalse();
    Mockito.verify(jwtInfoProvider).getUserRoles(VALID_ACCESS_TOKEN);
  }
}
