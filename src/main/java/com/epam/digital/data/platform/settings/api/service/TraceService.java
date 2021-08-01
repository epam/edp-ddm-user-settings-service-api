package com.epam.digital.data.platform.settings.api.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class TraceService {

  private static final String MDC_TRACE_ID_HEADER = "X-B3-TraceId";

  public String getRequestId() {
    return MDC.get(MDC_TRACE_ID_HEADER);
  }
}
