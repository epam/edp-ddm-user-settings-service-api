package com.epam.digital.data.platform.settings.api.audit;

import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.settings.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.settings.api.utils.ResponseCode;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateInputDto;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Set;

@Aspect
@Component
public class ControllerAuditAspect {

  Set<Integer> httpStatusOfSecurityAudit = Set.of(401, 403, 412);
  private final Set<String> responseCodeOfSecurityAudit = Set.of(
      ResponseCode.AUTHENTICATION_FAILED,
      ResponseCode.JWT_INVALID,
      ResponseCode.JWT_EXPIRED);
  // action
  static final String READ = "READ ENTITY";
  static final String UPDATE = "UPDATE ENTITY";

  // step
  static final String BEFORE = "BEFORE";
  static final String AFTER = "AFTER";

  private final RestAuditEventsFacade restAuditEventsFacade;

  public ControllerAuditAspect(RestAuditEventsFacade restAuditEventsFacade) {
    this.restAuditEventsFacade = restAuditEventsFacade;
  }

  @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
  public void withinRestControllerPointcut() {
  }

  @Pointcut("execution(* com.epam.digital.data.platform.settings.api.exception.ApplicationExceptionHandler.*(..))")
  public void exceptionHandlerPointcut() {
  }

  @Pointcut("execution(public * com.epam.digital.data.platform.starter.security.jwt.TokenParser.parseClaims(..))")
  public void jwtParsingPointcut() {
  }

  @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping) && withinRestControllerPointcut()")
  public void getPointcut() {
  }

  @Pointcut("@annotation(org.springframework.web.bind.annotation.PutMapping) && withinRestControllerPointcut()")
  public void putPointcut() {
  }

  @AfterReturning(pointcut = "exceptionHandlerPointcut()", returning = "response")
  void exceptionAudit(ResponseEntity<?> response) {
    prepareAndSendExceptionAudit(response);
  }

  @Around("putPointcut() && args(input, securityContext)")
  Object auditUpdate(
      ProceedingJoinPoint joinPoint, SettingsUpdateInputDto input, SecurityContext securityContext)
      throws Throwable {
    return prepareAndSendRestAudit(joinPoint, UPDATE, securityContext);
  }

  @AfterThrowing("jwtParsingPointcut()")
  void auditInvalidJwt() {
    restAuditEventsFacade.auditInvalidAccessToken();
  }

  @Around("getPointcut() && args(securityContext)")
  Object auditGetSearch(ProceedingJoinPoint joinPoint, SecurityContext securityContext) throws Throwable {
    return prepareAndSendRestAudit(joinPoint, READ, securityContext);
  }

  private void prepareAndSendExceptionAudit(ResponseEntity<?> response) {
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

  private Object prepareAndSendRestAudit(ProceedingJoinPoint joinPoint, String action,
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
