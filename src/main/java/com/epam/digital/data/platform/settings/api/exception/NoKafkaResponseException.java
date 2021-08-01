package com.epam.digital.data.platform.settings.api.exception;

public class NoKafkaResponseException extends RuntimeException {

  public NoKafkaResponseException(String message, Exception e) {
    super(message, e);
  }
}
