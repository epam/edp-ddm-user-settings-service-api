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

package com.epam.digital.data.platform.settings.api.audit;

import com.epam.digital.data.platform.settings.api.service.TraceService;
import com.epam.digital.data.platform.starter.audit.model.AuditEvent;
import com.epam.digital.data.platform.starter.audit.model.AuditUserInfo;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static com.epam.digital.data.platform.settings.api.audit.RestAuditEventsFacade.INVALID_ACCESS_TOKEN_EVENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ObjectMapper.class})
class RestAuditEventsFacadeTest {

  private static final String APP_NAME = "application";
  private static final String REQUEST_ID = "1";
  private static final String METHOD_NAME = "method";
  private static final String ACTION = "CREATE";
  private static final String STEP = "BEFORE";
  private static final String USER_DRFO = "1010101014";
  private static final String USER_KEYCLOAK_ID = "496fd2fd-3497-4391-9ead-41410522d06f";
  private static final String USER_NAME = "Сидоренко Василь Леонідович";
  private static final String RESULT = "RESULT";

  private static final LocalDateTime CURR_TIME = LocalDateTime.of(2021, 4, 1, 11, 50);
  private static final String ACCESS_TOKEN = "token";

  private RestAuditEventsFacade restAuditEventsFacade;
  @Mock
  private AuditService auditService;
  @Mock
  private TraceService traceService;
  @Mock
  private TokenParser tokenParser;

  private final Clock clock =
      Clock.fixed(CURR_TIME.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

  @Captor
  private ArgumentCaptor<AuditEvent> auditEventCaptor;

  @BeforeEach
  void beforeEach() {
    restAuditEventsFacade =
        new RestAuditEventsFacade(
            auditService, APP_NAME, clock, traceService, tokenParser);

    when(traceService.getRequestId()).thenReturn(REQUEST_ID);
  }

  @Test
  void expectCorrectAuditEventWhenNoAccessToken() {
    String CODE_JWT_INVALID = "JWT_INVALID";
    restAuditEventsFacade.auditInvalidAccessToken();

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    var expectedEvent = AuditEvent.AuditEventBuilder.anAuditEvent()
            .application(APP_NAME)
            .name(INVALID_ACCESS_TOKEN_EVENT_NAME)
            .requestId(REQUEST_ID)
            .sourceInfo(null)
            .userInfo(null)
            .currentTime(clock.millis())
            .eventType(EventType.SECURITY_EVENT)
            .context(Map.of("action", CODE_JWT_INVALID))
            .build();
    assertThat(actualEvent).usingRecursiveComparison().isEqualTo(expectedEvent);
  }

  @Test
  void expectCorrectAuditEventWithJwt() {
    Map<String, Object> context = Map.of("action", ACTION, "step", STEP,  "result", RESULT);
    when(auditService.createContext(ACTION, STEP, null, null, null, RESULT)).thenReturn(context);
    JwtClaimsDto userClaims = new JwtClaimsDto();
    userClaims.setDrfo(USER_DRFO);
    userClaims.setSubject(USER_KEYCLOAK_ID);
    userClaims.setFullName(USER_NAME);
    when(tokenParser.parseClaims(any()))
            .thenReturn(userClaims);

    restAuditEventsFacade.sendRestAudit(EventType.USER_ACTION, METHOD_NAME, ACTION, ACCESS_TOKEN, STEP, RESULT);

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    var expectedEvent = AuditEvent.AuditEventBuilder.anAuditEvent()
            .application(APP_NAME)
            .name("HTTP request. Method: method")
            .requestId(REQUEST_ID)
            .sourceInfo(null)
            .userInfo(new AuditUserInfo(USER_NAME, USER_KEYCLOAK_ID, USER_DRFO))
            .currentTime(clock.millis())
            .eventType(EventType.USER_ACTION)
            .context(context)
            .build();
    assertThat(actualEvent).usingRecursiveComparison().isEqualTo(expectedEvent);
  }

  @Test
  void expectCorrectAuditEventWithoutJwt() {
    Map<String, Object> context = Map.of("action", ACTION, "step", STEP);
    when(auditService.createContext(ACTION, STEP, null, null, null, null))
        .thenReturn(context);

    restAuditEventsFacade.sendRestAudit(EventType.USER_ACTION, METHOD_NAME, ACTION, null, STEP, null);

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    var expectedEvent = AuditEvent.AuditEventBuilder.anAuditEvent()
            .application(APP_NAME)
            .name("HTTP request. Method: method")
            .requestId(REQUEST_ID)
            .sourceInfo(null)
            .userInfo(null)
            .currentTime(clock.millis())
            .eventType(EventType.USER_ACTION)
            .context(context)
            .build();
    assertThat(actualEvent).usingRecursiveComparison().isEqualTo(expectedEvent);
  }

  @Test
  void expectCorrectExceptionAudit() {
    Map<String, Object> context = Map.of("action", ACTION);
    when(auditService.createContext(ACTION, null, null, null, null, null))
        .thenReturn(context);

    restAuditEventsFacade.sendExceptionAudit(EventType.USER_ACTION, ACTION);

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    var expectedEvent = AuditEvent.AuditEventBuilder.anAuditEvent()
            .application(APP_NAME)
            .name("EXCEPTION")
            .requestId(REQUEST_ID)
            .sourceInfo(null)
            .userInfo(null)
            .currentTime(clock.millis())
            .eventType(EventType.USER_ACTION)
            .context(context)
            .build();
    assertThat(actualEvent).usingRecursiveComparison().isEqualTo(expectedEvent);
  }
}