package com.epam.digital.data.platform.settings.api.audit;

import com.epam.digital.data.platform.settings.api.service.TraceService;
import com.epam.digital.data.platform.starter.audit.model.AuditUserInfo;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.audit.service.AbstractAuditFacade;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import java.time.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestAuditEventsFacade extends AbstractAuditFacade {

  static final String INVALID_ACCESS_TOKEN_EVENT_NAME = "Access Token is not valid";
  static final String CODE_JWT_INVALID = "JWT_INVALID";

  private final TraceService traceService;
  private final TokenParser tokenParser;

  static final String HTTP_REQUEST = "HTTP request. Method: ";
  static final String EXCEPTION = "EXCEPTION";

  static final String ACTION = "action";

  public RestAuditEventsFacade(
      AuditService auditService,
      @Value("${spring.application.name:user-settings-service-api}") String appName,
      Clock clock,
      TraceService traceService,
      TokenParser tokenParser) {
    super(auditService, appName, clock);
    this.traceService = traceService;
    this.tokenParser = tokenParser;
  }

  public void auditInvalidAccessToken() {
    var event =
        createBaseAuditEvent(
            EventType.SECURITY_EVENT, INVALID_ACCESS_TOKEN_EVENT_NAME, traceService.getRequestId());
    event.setContext(Map.of(ACTION, CODE_JWT_INVALID));
    auditService.sendAudit(event.build());
  }

  public void sendExceptionAudit(EventType eventType, String action) {
    var event = createBaseAuditEvent(eventType, EXCEPTION, traceService.getRequestId());

    var context = auditService.createContext(action, null, null, null, null, null);
    event.setContext(context);

    auditService.sendAudit(event.build());
  }

  public void sendRestAudit(EventType eventType, String methodName, String action, String jwt,
      String step, String result) {
    var event = createBaseAuditEvent(
        eventType, HTTP_REQUEST + methodName, traceService.getRequestId());

    var context = auditService.createContext(action, step, null, null, null, result);
    event.setContext(context);
    setUserInfoToEvent(event, jwt);

    auditService.sendAudit(event.build());
  }

  private void setUserInfoToEvent(GroupedAuditEventBuilder event, String jwt) {
    if (jwt == null) {
      return;
    }

    var jwtClaimsDto = tokenParser.parseClaims(jwt);
    var userInfo = AuditUserInfo.AuditUserInfoBuilder.anAuditUserInfo()
            .userName(jwtClaimsDto.getFullName())
            .userKeycloakId(jwtClaimsDto.getSubject())
            .userDrfo(jwtClaimsDto.getDrfo())
            .build();
    event.setUserInfo(userInfo);
  }
}
