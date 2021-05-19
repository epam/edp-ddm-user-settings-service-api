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

import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.settings.api.controller.SettingsController;
import com.epam.digital.data.platform.settings.api.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsReadByKeycloakIdService;
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
    AuditAspectTest.MockNonControllerClient.class,
    AuditEventProcessor.class,
    AuditAspect.class,
    ApplicationExceptionHandler.class,
    TokenParser.class
})
@MockBean(ObjectMapper.class)
@MockBean(TraceService.class)
class AuditAspectTest {

  @Autowired
  private SettingsController controller;
  @Autowired
  private ApplicationExceptionHandler applicationExceptionHandler;
  @Autowired
  private MockNonControllerClient nonControllerClient;
  @Autowired
  private TokenParser tokenParser;
  
  @MockBean
  private SettingsReadService readService;
  @MockBean
  private SettingsUpdateService updateService;
  @MockBean
  private SettingsReadByKeycloakIdService readByKeycloakIdService;
  @MockBean
  private RestAuditEventsFacade restAuditEventsFacade;

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
