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

package com.epam.digital.data.platform.settings.api.exception;

import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.settings.api.controller.SettingsController;
import com.epam.digital.data.platform.settings.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.settings.api.model.FieldsValidationErrorDetails;
import com.epam.digital.data.platform.settings.api.audit.RestAuditEventsFacade;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsUpdateService;
import com.epam.digital.data.platform.settings.api.service.TraceService;
import com.epam.digital.data.platform.settings.api.utils.ResponseCode;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateInputDto;
import com.epam.digital.data.platform.starter.security.PermitAllWebSecurityConfig;
import com.epam.digital.data.platform.starter.security.exception.JwtParsingException;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.epam.digital.data.platform.starter.security.jwt.TokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(
    classes = {SettingsController.class, ApplicationExceptionHandler.class, TokenParser.class})
@Import({TokenProvider.class, PermitAllWebSecurityConfig.class})
class ApplicationExceptionHandlerTest extends ResponseEntityExceptionHandler {

  private static final String BASE_URL = "/settings";

  private static final String TRACE_ID = "1";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private SettingsReadService readService;
  @MockBean
  private SettingsUpdateService updateService;
  @MockBean
  private RestAuditEventsFacade restAuditEventsFacade;
  @MockBean
  private TraceService traceService;

  @BeforeEach
  void beforeEach() {
    when(traceService.getRequestId()).thenReturn(TRACE_ID);
  }

  @Test
  void shouldReturnTimeoutErrorOnNoKafkaResponse() throws Exception {
    when(readService.request(any())).thenThrow(NoKafkaResponseException.class);

    mockMvc
        .perform(get(BASE_URL))
        .andExpect(status().isInternalServerError())
        .andExpect(
            response ->
                assertTrue(response.getResolvedException() instanceof NoKafkaResponseException))
        .andExpect(
            matchAll(
                jsonPath("$.traceId").value(is(TRACE_ID)),
                jsonPath("$.code").value(is(ResponseCode.TIMEOUT_ERROR)),
                jsonPath("$.details").doesNotExist()));
  }

  @Test
  void shouldReturnRuntimeErrorOnGenericException() throws Exception {
    when(readService.request(any())).thenThrow(RuntimeException.class);

    mockMvc
        .perform(get(BASE_URL))
        .andExpect(status().isInternalServerError())
        .andExpect(
            matchAll(
                jsonPath("$.traceId").value(is(TRACE_ID)),
                jsonPath("$.code").value(is(ResponseCode.RUNTIME_ERROR)),
                jsonPath("$.details").doesNotExist()));
  }

  @Test
  void shouldReturnBadRequestOnHttpNotReadable() throws Exception {
    when(readService.request(any())).thenThrow(HttpMessageNotReadableException.class);

    mockMvc
        .perform(get(BASE_URL))
        .andExpect(status().isBadRequest())
        .andExpect(
            response ->
                assertTrue(
                    response.getResolvedException() instanceof HttpMessageNotReadableException));
  }

  @Test
  void shouldReturn415WithBodyWhenMediaTypeIsNotSupported() throws Exception {
    var unsupportedMediaType = MediaType.APPLICATION_PDF;

    mockMvc
        .perform(put(BASE_URL).contentType(unsupportedMediaType))
        .andExpect(
            matchAll(
                status().isUnsupportedMediaType(),
                jsonPath("$.traceId").value(is(TRACE_ID)),
                jsonPath("$.code").value(is(ResponseCode.UNSUPPORTED_MEDIA_TYPE)),
                jsonPath("$.details").doesNotExist()));
  }

  @Test
  void shouldReturn500WithCorrectCodeWhenKafkaInternalException() throws Exception {
    when(readService.request(any()))
        .thenReturn(mockResponse(Status.THIRD_PARTY_SERVICE_UNAVAILABLE));

    mockMvc
        .perform(get(BASE_URL))
        .andExpect(status().isInternalServerError())
        .andExpect(
            matchAll(
                jsonPath("$.traceId").value(is(TRACE_ID)),
                jsonPath("$.code").value(is("THIRD_PARTY_SERVICE_UNAVAILABLE")),
                jsonPath("$.details").doesNotExist()));
  }

  @Test
  void shouldReturn422WithBodyWhenMethodArgumentNotValid() throws Exception {
    var inputBody = new SettingsUpdateInputDto();
    String inputStringBody = objectMapper.writeValueAsString(inputBody);

    var expectedResponseObject = new DetailedErrorResponse<FieldsValidationErrorDetails>();
    expectedResponseObject.setTraceId(TRACE_ID);
    expectedResponseObject.setCode(ResponseCode.VALIDATION_ERROR);
    expectedResponseObject.setDetails(
        validationDetailsFrom(
            new FieldsValidationErrorDetails.FieldError(null, "email", "must not be null")));
    String expectedOutputBody = objectMapper.writeValueAsString(expectedResponseObject);

    mockMvc
        .perform(put(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(inputStringBody))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            response ->
                assertTrue(
                    response.getResolvedException() instanceof MethodArgumentNotValidException))
        .andExpect(content().json(expectedOutputBody));
  }

  @Test
  void shouldReturn401WhenJwtParsingException() throws Exception {
    when(readService.request(any())).thenThrow(JwtParsingException.class);

    mockMvc
        .perform(get(BASE_URL))
        .andExpect(
            matchAll(
                status().isUnauthorized(),
                jsonPath("$.traceId").value(is(TRACE_ID)),
                jsonPath("$.code").value(is(ResponseCode.JWT_INVALID))));

    verify(restAuditEventsFacade).auditInvalidAccessToken();
  }

  @Test
  void shouldReturn403WhenKafkaReturnJwtError() throws Exception {
    when(readService.request(any())).thenReturn(mockResponse(Status.JWT_INVALID));

    mockMvc
        .perform(get(BASE_URL))
        .andExpect(
            matchAll(
                status().isForbidden(),
                jsonPath("$.traceId").value(is(TRACE_ID)),
                jsonPath("$.code").value(is(ResponseCode.JWT_INVALID))));
  }

  @Test
  void shouldReturn404WhenNotFoundException() throws Exception {
    when(readService.request(any())).thenThrow(new NotFoundException("some resource is not found"));

    mockMvc
        .perform(get(BASE_URL))
        .andExpect(
            matchAll(
                status().isNotFound(),
                jsonPath("$.traceId").value(is(TRACE_ID)),
                jsonPath("$.code").value(is(ResponseCode.NOT_FOUND))));
  }

  @Test
  void shouldReturn404WhenNoHandlerFoundException() throws Exception {
    MockHttpServletRequestBuilder requestBuilder = get("/someBadUrl");
    
    ResultActions perform = mockMvc.perform(requestBuilder);
    
    perform
            .andExpect(
                    matchAll(
                            status().isNotFound(),
                            jsonPath("$.traceId").value(is(TRACE_ID)),
                            jsonPath("$.code").value(is(ResponseCode.NOT_FOUND))));
  }

  private <T> Response<T> mockResponse(Status status) {
    Response<T> response = new Response<>();
    response.setStatus(status);
    return response;
  }

  public FieldsValidationErrorDetails validationDetailsFrom(
      FieldsValidationErrorDetails.FieldError... details) {
    return new FieldsValidationErrorDetails(Arrays.asList(details));
  }
}
