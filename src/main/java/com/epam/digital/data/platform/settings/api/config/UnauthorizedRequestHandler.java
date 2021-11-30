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

package com.epam.digital.data.platform.settings.api.config;

import com.epam.digital.data.platform.settings.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.settings.api.audit.RestAuditEventsFacade;
import com.epam.digital.data.platform.settings.api.service.TraceService;
import com.epam.digital.data.platform.settings.api.utils.ResponseCode;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.security.jwt.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class UnauthorizedRequestHandler extends RestAuthenticationEntryPoint {

  private final TraceService traceService;
  private final ObjectMapper objectMapper;
  private final RestAuditEventsFacade restAuditEventsFacade;

  public UnauthorizedRequestHandler(
      ObjectMapper objectMapper,
      TraceService traceService,
      RestAuditEventsFacade restAuditEventsFacade) {
    super(objectMapper);
    this.traceService = traceService;
    this.objectMapper = objectMapper;
    this.restAuditEventsFacade = restAuditEventsFacade;
  }

  @Override
  @SuppressWarnings("findsecbugs:XSS_SERVLET")
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException e) throws IOException {
    restAuditEventsFacade.sendExceptionAudit(EventType.SECURITY_EVENT, ResponseCode.AUTHENTICATION_FAILED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write(objectMapper.writeValueAsString(
        newDetailedResponse(ResponseCode.AUTHENTICATION_FAILED)));
  }

  private <T> DetailedErrorResponse<T> newDetailedResponse(String code) {
    var response = new DetailedErrorResponse<T>();
    response.setTraceId(traceService.getRequestId());
    response.setCode(code);
    return response;
  }
}
