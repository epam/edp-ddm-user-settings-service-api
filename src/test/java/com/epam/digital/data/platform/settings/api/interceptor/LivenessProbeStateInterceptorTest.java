package com.epam.digital.data.platform.settings.api.interceptor;

import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.settings.api.config.WebConfig;
import com.epam.digital.data.platform.settings.api.controller.SettingsController;
import com.epam.digital.data.platform.settings.api.service.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.SettingsUpdateService;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import com.epam.digital.data.platform.starter.security.PermitAllWebSecurityConfig;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.epam.digital.data.platform.starter.security.jwt.TokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import com.epam.digital.data.platform.starter.actuator.livenessprobe.LivenessStateHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest
@ContextConfiguration(
    classes = {SettingsController.class, LivenessProbeStateInterceptor.class, WebConfig.class,
    TokenProvider.class, TokenParser.class})
@Import({PermitAllWebSecurityConfig.class})
class LivenessProbeStateInterceptorTest {

  private static final String BASE_URL = "/settings";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SettingsReadService readService;
  @MockBean
  private SettingsUpdateService updateService;
  @MockBean
  private LivenessStateHandler livenessStateHandler;

  @Test
  void expectStateHandlerIsCalledInInterceptor() throws Exception {
    var mockResponse = new Response<SettingsReadDto>();
    mockResponse.setStatus(Status.SUCCESS);
    when(readService.request(any())).thenReturn(mockResponse);

    mockMvc.perform(get(BASE_URL));

    verify(livenessStateHandler).handleResponse(eq(HttpStatus.OK), any());
  }
}
