package com.epam.digital.data.platform.settings.api.utils;

import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.settings.api.exception.KafkaInternalServerException;
import com.epam.digital.data.platform.settings.api.exception.KafkaSecurityValidationFailedException;
import com.epam.digital.data.platform.settings.api.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.function.BiFunction;

public final class ResponseResolverUtil {

  private static final Map<HttpStatus, BiFunction<Response<?>, HttpStatus, RuntimeException>>
      httpErrorCodeToException =
          Map.of(
              HttpStatus.INTERNAL_SERVER_ERROR,
                  (response, httpStatus) ->
                      new KafkaInternalServerException(
                          "Kafka returned error response", response, httpStatus),
              HttpStatus.FORBIDDEN,
                  (response, httpStatus) ->
                      new KafkaSecurityValidationFailedException(
                          "One of security validations failed for operation on kafka side", response, httpStatus),
              HttpStatus.NOT_FOUND,
                  (response, httpStatus) -> new NotFoundException(response.getDetails()));

  private ResponseResolverUtil() {
  }

  public static <T> ResponseEntity<T> getHttpResponseFromKafka(Response<T> kafkaResponse) {
    HttpStatus httpStatus = StatusUtils.convertResponseStatus(kafkaResponse);
    if (httpErrorCodeToException.containsKey(httpStatus)) {
      throw httpErrorCodeToException.get(httpStatus)
          .apply(kafkaResponse, httpStatus);
    }
    return ResponseEntity.status(httpStatus)
        .body(kafkaResponse.getPayload());
  }
}
