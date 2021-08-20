package com.epam.digital.data.platform.settings.api.config;

import static org.mockito.Mockito.mock;

import com.epam.digital.data.platform.settings.api.audit.RestAuditEventsFacade;
import com.epam.digital.data.platform.settings.api.service.TraceService;
import com.epam.digital.data.platform.starter.actuator.livenessprobe.LivenessStateHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestBeansConfig {

  @Bean
  public LivenessStateHandler livenessStateHandler() {
    return mock(LivenessStateHandler.class);
  }

  @Bean
  public TraceService logService() {
    return mock(TraceService.class);
  }

  @Bean
  public RestAuditEventsFacade restAuditEventsFacade() {
    return mock(RestAuditEventsFacade.class);
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
