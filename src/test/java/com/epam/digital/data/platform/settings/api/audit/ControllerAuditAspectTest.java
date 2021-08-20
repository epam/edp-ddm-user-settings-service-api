package com.epam.digital.data.platform.settings.api.audit;

import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.settings.api.controller.SettingsController;
import com.epam.digital.data.platform.settings.api.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsUpdateService;
import com.epam.digital.data.platform.settings.api.service.TraceService;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateOutputDto;
import com.epam.digital.data.platform.starter.security.exception.JwtParsingException;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.PutMapping;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import(AopAutoConfiguration.class)
@SpringBootTest(classes = {
    SettingsController.class,
    ControllerAuditAspectTest.MockNonControllerClient.class,
    ControllerAuditAspect.class,
    ApplicationExceptionHandler.class,
    TokenParser.class
})
class ControllerAuditAspectTest {

  @Autowired
  private SettingsController controller;
  @Autowired
  private ApplicationExceptionHandler applicationExceptionHandler;
  @Autowired
  private MockNonControllerClient nonControllerClient;
  @Autowired
  private TokenParser tokenParser;

  @MockBean
  private ObjectMapper objectMapper;
  @MockBean
  private SettingsReadService readService;
  @MockBean
  private SettingsUpdateService updateService;
  @MockBean
  private RestAuditEventsFacade restAuditEventsFacade;
  @MockBean
  private TraceService traceService;

  @Mock
  private RequestContext mockRequestContext;
  @Mock
  private SecurityContext mockSecurityContext;

  @BeforeEach
  void beforeEach() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void expectAuditAspectBeforeAndAfterGetMethodWhenNoException() {
    when(readService.request(any())).thenReturn(mockSuccessResponse());

    controller.findUserSettings(mockSecurityContext);

    verify(restAuditEventsFacade, times(2))
        .sendRestAudit(any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnGetMethod() {
    when(readService.request(any())).thenThrow(new RuntimeException());

    assertThrows(
        RuntimeException.class,
        () -> controller.findUserSettings(mockSecurityContext));

    verify(restAuditEventsFacade)
        .sendRestAudit(any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectBeforeAndAfterPutMethodWhenNoException() {
    when(updateService.request(any())).thenReturn(mockSuccessResponse());

    controller.updateUserSettings(new SettingsUpdateInputDto(), mockSecurityContext);

    verify(restAuditEventsFacade, times(2))
        .sendRestAudit(any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnPutMethod() {
    when(updateService.request(any())).thenReturn(mockErrorResponse());

    assertThrows(
        RuntimeException.class,
        () ->
            controller.updateUserSettings(new SettingsUpdateInputDto(),
                mockSecurityContext));

    verify(restAuditEventsFacade)
        .sendRestAudit(any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectNonCalledIfNonRestControllerCall() {
    nonControllerClient.updateNonController();

    verifyNoInteractions(restAuditEventsFacade);
  }

  @Test
  void expectAuditAspectBeforeGetAndAfterExceptionHandler(){
    applicationExceptionHandler.handleException(new RuntimeException());

    verify(restAuditEventsFacade).sendExceptionAudit(any(), any());
  }

  @Test
  void expectAuditAspectWhenExceptionWhileTokenParsing() {
    assertThrows(
        JwtParsingException.class,
        () -> tokenParser.parseClaims("incorrectToken"));

    verify(restAuditEventsFacade).auditInvalidAccessToken();
  }

  private <T> Response<T> mockSuccessResponse() {
    Response<T> response = new Response<>();
    response.setStatus(Status.SUCCESS);
    return response;
  }

  private <T> Response<T> mockErrorResponse() {
    Response<T> response = new Response<>();
    response.setStatus(Status.JWT_INVALID);
    return response;
  }

  @TestComponent
  public static class MockNonControllerClient {

    @PutMapping
    public SettingsUpdateOutputDto updateNonController() {
      return new SettingsUpdateOutputDto();
    }
  }
}
