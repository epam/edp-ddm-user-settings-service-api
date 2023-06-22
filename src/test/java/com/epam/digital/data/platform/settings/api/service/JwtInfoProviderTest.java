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

import com.epam.digital.data.platform.starter.security.SystemRole;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class JwtInfoProviderTest {

  @Mock
  private TokenParser tokenParser;

  private JwtInfoProvider jwtInfoProvider;

  @BeforeEach
  void beforeEach() {
    jwtInfoProvider = new JwtInfoProvider(tokenParser);
  }

  @Test
  void expectSubjectIsRetrievedFromToken() {
    JwtClaimsDto jwtClaimsDto = new JwtClaimsDto();
    jwtClaimsDto.setSubject("subject");

    when(tokenParser.parseClaims(any())).thenReturn(jwtClaimsDto);

    var actual = jwtInfoProvider.getUserId("token");

    assertThat(actual).isEqualTo("subject");
  }

  @Test
  void containsUserRole() {
    JwtClaimsDto jwtClaimsDto = new JwtClaimsDto();
    List<String> roles = List.of("officer", "auditor", "task-dispatcher");
    jwtClaimsDto.setRoles(roles);

    when(tokenParser.parseClaims(any())).thenReturn(jwtClaimsDto);

    var actual = jwtInfoProvider.getUserRoles("token");

    assertThat(actual).isEqualTo(roles);
  }
}
