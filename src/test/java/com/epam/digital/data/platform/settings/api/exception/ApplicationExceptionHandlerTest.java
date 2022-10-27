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

import static com.epam.digital.data.platform.settings.api.utils.Header.X_ACCESS_TOKEN;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.settings.api.controller.SettingsController;
import com.epam.digital.data.platform.settings.api.converter.StringToChannelConverter;
import com.epam.digital.data.platform.settings.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.settings.api.model.FieldsValidationErrorDetails;
import com.epam.digital.data.platform.settings.api.service.ChannelVerificationService;
import com.epam.digital.data.platform.settings.api.service.SettingsActivationService;
import com.epam.digital.data.platform.settings.api.service.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.SettingsValidationService;
import com.epam.digital.data.platform.settings.api.service.TraceService;
import com.epam.digital.data.platform.settings.api.utils.ResponseCode;
import com.epam.digital.data.platform.settings.model.dto.ActivateEmailInputDto;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.SettingsEmailInputDto;
import com.epam.digital.data.platform.settings.model.dto.VerificationInputDto;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
import com.epam.digital.data.platform.starter.security.PermitAllWebSecurityConfig;
import com.epam.digital.data.platform.starter.security.exception.JwtParsingException;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.epam.digital.data.platform.starter.security.jwt.TokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
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

@WebMvcTest
@ContextConfiguration(
    classes = {SettingsController.class, ApplicationExceptionHandler.class, TokenParser.class,
        StringToChannelConverter.class})
@Import({TokenProvider.class, PermitAllWebSecurityConfig.class})
class ApplicationExceptionHandlerTest extends ResponseEntityExceptionHandler {

  private static final String BASE_URL = "/api/settings";

  private static final String TRACE_ID = "1";

  private static final String TOKEN = "token";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private SettingsReadService settingsReadService;
  @MockBean
  private TraceService traceService;
  @MockBean
  private SettingsActivationService settingsActivationService;
  @MockBean
  private SettingsValidationService settingsValidationService;
  @MockBean
  private MessageResolver messageResolver;
  @MockBean
  private ChannelVerificationService channelVerificationFacade;

  @BeforeEach
  void beforeEach() {
    when(traceService.getRequestId()).thenReturn(TRACE_ID);
  }

  @Test
  void shouldReturnRuntimeErrorOnGenericException() throws Exception {
    when(settingsReadService.findSettingsFromUserToken(any())).thenThrow(RuntimeException.class);

    mockMvc
        .perform(get(BASE_URL + "/me").header(X_ACCESS_TOKEN.getHeaderName(), TOKEN))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.RUNTIME_ERROR)),
            jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturnBadRequestOnHttpNotReadable() throws Exception {
    when(settingsReadService.findSettingsFromUserToken(any())).thenThrow(
        HttpMessageNotReadableException.class);

    mockMvc
        .perform(get(BASE_URL + "/me").header(X_ACCESS_TOKEN.getHeaderName(), TOKEN))
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
        .perform(
            post(BASE_URL + "/me/channels/email/activate")
                .header(X_ACCESS_TOKEN.getHeaderName(), TOKEN)
                .contentType(unsupportedMediaType))
        .andExpectAll(
            status().isUnsupportedMediaType(),
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.UNSUPPORTED_MEDIA_TYPE)),
            jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturn422WithBodyWhenMethodArgumentNotValid() throws Exception {
    var inputBody = new ActivateEmailInputDto();
    inputBody.setVerificationCode("123456");
    String inputStringBody = objectMapper.writeValueAsString(inputBody);

    var expectedResponseObject = new DetailedErrorResponse<FieldsValidationErrorDetails>();
    expectedResponseObject.setTraceId(TRACE_ID);
    expectedResponseObject.setCode(ResponseCode.VALIDATION_ERROR);
    expectedResponseObject.setDetails(
        validationDetailsFrom(
            new FieldsValidationErrorDetails.FieldError(null, "address", "must not be null")));
    String expectedOutputBody = objectMapper.writeValueAsString(expectedResponseObject);

    mockMvc
        .perform(
            post(BASE_URL + "/me/channels/email/activate")
                .header(X_ACCESS_TOKEN.getHeaderName(), TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputStringBody))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            response ->
                assertTrue(
                    response.getResolvedException() instanceof MethodArgumentNotValidException))
        .andExpect(content().json(expectedOutputBody));
  }

  @Test
  void shouldReturn401WhenJwtParsingException() throws Exception {
    when(settingsReadService.findSettingsFromUserToken(any())).thenThrow(JwtParsingException.class);

    mockMvc
        .perform(get(BASE_URL + "/me").header(X_ACCESS_TOKEN.getHeaderName(), TOKEN))
        .andExpectAll(
            status().isUnauthorized(),
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.JWT_INVALID)));
  }

  @Test
  void shouldReturn404WhenNoHandlerFoundException() throws Exception {
    MockHttpServletRequestBuilder requestBuilder = get("/someBadUrl");

    ResultActions perform = mockMvc.perform(requestBuilder);

    perform.andExpectAll(
        status().isNotFound(),
        jsonPath("$.traceId").value(is(TRACE_ID)),
        jsonPath("$.code").value(is(ResponseCode.NOT_FOUND)));
  }

  public FieldsValidationErrorDetails validationDetailsFrom(
      FieldsValidationErrorDetails.FieldError... details) {
    return new FieldsValidationErrorDetails(Arrays.asList(details));
  }

  @Test
  void shouldReturn422WhenHandleEmailAddressValidationException() throws Exception {
    var payload = new SettingsEmailInputDto();
    payload.setAddress("");

    when(settingsValidationService.validateEmailAddress(any()))
        .thenThrow(new EmailAddressValidationException("message", "localized message"));

    mockMvc
        .perform(
            post(BASE_URL + "/me/channels/email/validate")
                .header(X_ACCESS_TOKEN.getHeaderName(), TOKEN)
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(status().isUnprocessableEntity(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.traceId", is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.VALIDATION_ERROR)));

    verify(messageResolver).getMessage("localized message");
  }

  @Test
  void shouldReturn400WhenInvalidVerificationCodeException() throws Exception {
    var payload = new VerificationInputDto();
    payload.setAddress("");

    var exception = new ChannelVerificationException("message");

    when(channelVerificationFacade.sendVerificationCode(any(Channel.class), any(
        VerificationInputDto.class), anyString())).thenThrow(exception);

    mockMvc
        .perform(post(BASE_URL + "/me/channels/email/verify")
            .header(X_ACCESS_TOKEN.getHeaderName(), TOKEN)
            .content(objectMapper.writeValueAsString(payload))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.VERIFICATION_ERROR)));
  }
}
