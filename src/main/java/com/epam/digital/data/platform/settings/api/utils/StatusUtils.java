package com.epam.digital.data.platform.settings.api.utils;

import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import org.springframework.http.HttpStatus;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class StatusUtils {

  private static final Map<Status, HttpStatus> kafkaResponseToHttpStatusMap;

  static {
    kafkaResponseToHttpStatusMap = new EnumMap<>(Status.class);
    kafkaResponseToHttpStatusMap.put(Status.SUCCESS, HttpStatus.OK);
    kafkaResponseToHttpStatusMap.put(Status.CREATED, HttpStatus.CREATED);
    kafkaResponseToHttpStatusMap.put(Status.NO_CONTENT, HttpStatus.NO_CONTENT);
    kafkaResponseToHttpStatusMap.put(Status.NOT_FOUND, HttpStatus.NOT_FOUND);
    kafkaResponseToHttpStatusMap
        .put(Status.THIRD_PARTY_SERVICE_UNAVAILABLE, HttpStatus.INTERNAL_SERVER_ERROR);
    kafkaResponseToHttpStatusMap.put(Status.JWT_EXPIRED, HttpStatus.FORBIDDEN);
    kafkaResponseToHttpStatusMap.put(Status.JWT_INVALID, HttpStatus.FORBIDDEN);
  }

  private StatusUtils() {
  }

  public static HttpStatus convertResponseStatus(Response<?> response) {
    return Optional.ofNullable(kafkaResponseToHttpStatusMap.get(response.getStatus()))
        .orElseThrow(() -> new IllegalArgumentException("Unknown status " + response.getStatus()));
  }
}
