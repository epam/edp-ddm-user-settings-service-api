/*
 * Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.settings.api.audit;

import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.settings.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.settings.api.utils.ResponseCode;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class AuditEventProcessor {
  
  // step
  private static final String BEFORE = "BEFORE";
  private static final String AFTER = "AFTER";
  
  private static final Set<Integer> httpStatusOfSecurityAudit = Set.of(401, 403, 412);
  private static final Set<String> responseCodeOfSecurityAudit = Set.of(
      ResponseCode.AUTHENTICATION_FAILED,
      ResponseCode.JWT_INVALID,
      ResponseCode.JWT_EXPIRED);

  private final RestAuditEventsFacade restAuditEventsFacade;

  public AuditEventProcessor(RestAuditEventsFacade restAuditEventsFacade) {
    this.restAuditEventsFacade = restAuditEventsFacade;
  }

  void prepareAndSendExceptionAudit(ResponseEntity<?> response) {
    var eventType = EventType.USER_ACTION;

    String action = response.getStatusCode().getReasonPhrase();
    if (response.getBody() instanceof DetailedErrorResponse) {
      action = ((DetailedErrorResponse) response.getBody()).getCode();
    }

    if (httpStatusOfSecurityAudit.contains(response.getStatusCodeValue()) ||
        responseCodeOfSecurityAudit.contains(action)) {
      eventType = EventType.SECURITY_EVENT;
    }
    restAuditEventsFacade.sendExceptionAudit(eventType, action);
  }

  void sendInvalidAccessTokenAudit() {
    restAuditEventsFacade.auditInvalidAccessToken();
  }

  Object prepareAndSendRestAudit(ProceedingJoinPoint joinPoint, String action,
      SecurityContext securityContext) throws Throwable {

    String methodName = joinPoint.getSignature().getName();
    String jwt = securityContext == null ? null : securityContext.getAccessToken();

    restAuditEventsFacade
        .sendRestAudit(EventType.USER_ACTION, methodName, action, jwt, BEFORE, null);

    Object result = joinPoint.proceed();
    var resultStatus = ((ResponseEntity<?>) result).getStatusCode().getReasonPhrase();

    restAuditEventsFacade.sendRestAudit(EventType.USER_ACTION, methodName,
        action, jwt, AFTER, resultStatus);

    return result;
  }
}
