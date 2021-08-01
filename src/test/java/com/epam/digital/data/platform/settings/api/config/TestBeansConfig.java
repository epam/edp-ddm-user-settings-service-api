package com.epam.digital.data.platform.settings.api.config;

import com.epam.digital.data.platform.settings.api.service.RestAuditEventsFacade;
import com.epam.digital.data.platform.settings.api.service.TraceService;
import com.epam.digital.data.platform.starter.actuator.livenessprobe.LivenessStateHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

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
}
