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
import org.springframework.http.HttpStatus;

public class KafkaInvalidResponseException extends RuntimeException {

  private final Response<?> kafkaResponse;
  private final HttpStatus httpStatus;

  public KafkaInvalidResponseException(String message, Response<?> kafkaResponse,
      HttpStatus httpStatus) {
    super(message);
    this.kafkaResponse = kafkaResponse;
    this.httpStatus = httpStatus;
  }

  public Response<?> getKafkaResponse() {
    return kafkaResponse;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
