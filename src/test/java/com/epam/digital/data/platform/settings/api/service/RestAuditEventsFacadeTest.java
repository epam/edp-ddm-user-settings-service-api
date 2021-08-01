package com.epam.digital.data.platform.settings.api.service;

import com.epam.digital.data.platform.starter.audit.model.AuditEvent;
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

import static com.epam.digital.data.platform.settings.api.service.RestAuditEventsFacade.INVALID_ACCESS_TOKEN_EVENT_NAME;
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
  private static final String USER_ID = "1010101014";
  private static final String USER_NAME = "Сидоренко Василь Леонідович";
  private static final String RESULT = "RESULT";

  private static final LocalDateTime CURR_TIME = LocalDateTime.of(2021, 4, 1, 11, 50);
  private static String ACCESS_TOKEN = "token";

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
    restAuditEventsFacade = new RestAuditEventsFacade(APP_NAME, auditService, traceService, clock,
        tokenParser);

    when(traceService.getRequestId()).thenReturn(REQUEST_ID);
  }

  @Test
  void expectCorrectAuditEventWhenNoAccessToken() {
    String CODE_JWT_INVALID = "JWT_INVALID";
    restAuditEventsFacade.auditInvalidAccessToken();

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();
    assertThat(actualEvent.getRequestId()).isEqualTo(REQUEST_ID);
    assertThat(actualEvent.getApplication()).isEqualTo(APP_NAME);
    assertThat(actualEvent.getEventType()).isEqualTo(EventType.SECURITY_EVENT);
    assertThat(actualEvent.getCurrentTime()).isEqualTo(clock.millis());
    assertThat(actualEvent.getUserId()).isBlank();
    assertThat(actualEvent.getUserName()).isBlank();
    assertThat(actualEvent.getName()).isEqualTo(INVALID_ACCESS_TOKEN_EVENT_NAME);
    assertThat(actualEvent.getContext()).containsEntry("action", CODE_JWT_INVALID);
  }

  @Test
  void expectCorrectAuditEventWithJwt() {
    Map<String, Object> context = Map.of("action", ACTION, "step", STEP,  "result", RESULT);
    when(auditService.createContext(ACTION, STEP, null, null, null, RESULT)).thenReturn(context);
    JwtClaimsDto userClaims = new JwtClaimsDto();
    userClaims.setDrfo(USER_ID);
    userClaims.setFullName(USER_NAME);
    when(tokenParser.parseClaims(any()))
            .thenReturn(userClaims);

    restAuditEventsFacade.sendRestAudit(EventType.USER_ACTION, METHOD_NAME, ACTION, ACCESS_TOKEN, STEP, RESULT);

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    assertThat(actualEvent.getRequestId()).isEqualTo(REQUEST_ID);
    assertThat(actualEvent.getApplication()).isEqualTo(APP_NAME);
    assertThat(actualEvent.getEventType()).isEqualTo(EventType.USER_ACTION);
    assertThat(actualEvent.getCurrentTime()).isEqualTo(clock.millis());
    assertThat(actualEvent.getUserId()).isEqualTo(USER_ID);
    assertThat(actualEvent.getUserName()).isEqualTo(USER_NAME);
    assertThat(actualEvent.getName()).isEqualTo("HTTP request. Method: method");
    assertThat(actualEvent.getContext()).isEqualTo(context);
  }

  @Test
  void expectCorrectAuditEventWithoutJwt() {
    Map<String, Object> context = Map.of("action", ACTION, "step", STEP);
    when(auditService.createContext(ACTION, STEP, null, null, null, null))
        .thenReturn(context);

    restAuditEventsFacade.sendRestAudit(EventType.USER_ACTION, METHOD_NAME, ACTION, null, STEP, null);

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    assertThat(actualEvent.getRequestId()).isEqualTo(REQUEST_ID);
    assertThat(actualEvent.getApplication()).isEqualTo(APP_NAME);
    assertThat(actualEvent.getEventType()).isEqualTo(EventType.USER_ACTION);
    assertThat(actualEvent.getCurrentTime()).isEqualTo(clock.millis());
    assertThat(actualEvent.getUserId()).isNull();
    assertThat(actualEvent.getUserName()).isNull();
    assertThat(actualEvent.getName()).isEqualTo("HTTP request. Method: method");
    assertThat(actualEvent.getContext()).isEqualTo(context);
  }

  @Test
  void expectCorrectExceptionAudit() {
    Map<String, Object> context = Map.of("action", ACTION);
    when(auditService.createContext(ACTION, null, null, null, null, null))
        .thenReturn(context);

    restAuditEventsFacade.sendExceptionAudit(EventType.USER_ACTION, ACTION);

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    assertThat(actualEvent.getRequestId()).isEqualTo(REQUEST_ID);
    assertThat(actualEvent.getApplication()).isEqualTo(APP_NAME);
    assertThat(actualEvent.getEventType()).isEqualTo(EventType.USER_ACTION);
    assertThat(actualEvent.getCurrentTime()).isEqualTo(clock.millis());
    assertThat(actualEvent.getUserId()).isNull();
    assertThat(actualEvent.getUserName()).isNull();
    assertThat(actualEvent.getName()).isEqualTo("EXCEPTION");
    assertThat(actualEvent.getContext()).isEqualTo(context);
  }
}