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

import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.epam.digital.data.platform.settings.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.settings.api.model.DetailedValidationErrorResponse;
import com.epam.digital.data.platform.settings.api.model.FieldsValidationErrorDetails;
import com.epam.digital.data.platform.settings.api.service.TraceService;
import com.epam.digital.data.platform.settings.api.utils.ResponseCode;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
import com.epam.digital.data.platform.starter.security.exception.JwtParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

  private final Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

  private final TraceService traceService;
  private final MessageResolver messageResolver;

  public ApplicationExceptionHandler(TraceService traceService,
      MessageResolver messageResolver) {
    this.traceService = traceService;
    this.messageResolver = messageResolver;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleAccessDeniedException(
      AccessDeniedException exception) {
    log.error("Access denied", exception);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(newDetailedResponse(ResponseCode.FORBIDDEN_OPERATION));
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatus status,
      WebRequest request) {
    log.error("One or more input arguments are not valid", exception);
    DetailedErrorResponse<FieldsValidationErrorDetails> invalidFieldsResponse
        = newDetailedResponse(ResponseCode.VALIDATION_ERROR);

    var generalErrorList = exception.getBindingResult().getFieldErrors();
    var customErrorsDetails = generalErrorList.stream()
        .map(error -> new FieldsValidationErrorDetails.FieldError(error.getRejectedValue(),
            error.getField(), error.getDefaultMessage()))
        .collect(toList());
    invalidFieldsResponse.setDetails(new FieldsValidationErrorDetails(customErrorsDetails));

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(invalidFieldsResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleException(Exception exception) {
    log.error("Runtime error occurred", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(ResponseCode.RUNTIME_ERROR));
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatus status,
      WebRequest request) {

    log.error("Request body is not readable JSON", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(ResponseCode.CLIENT_ERROR));
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status,
      WebRequest request) {
    log.error("Payload format is in an unsupported format", ex);
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(newDetailedResponse(ResponseCode.UNSUPPORTED_MEDIA_TYPE));
  }

  @ExceptionHandler(JwtParsingException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleJwtParsingException(
      Exception exception) {
    log.error("Access Token is not valid", exception);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(newDetailedResponse(ResponseCode.JWT_INVALID));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleAuthenticationException(
      AuthenticationException exception) {
    log.error("Authentication failure", exception);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(newDetailedResponse(ResponseCode.AUTHENTICATION_FAILED));
  }

  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(
      NoHandlerFoundException exception, HttpHeaders headers, HttpStatus status,
      WebRequest request) {
    log.error("Page not found", exception);
    return ResponseEntity.status(NOT_FOUND)
        .body(newDetailedResponse(ResponseCode.NOT_FOUND));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleMethodArgumentTypeMismatchException(
      Exception exception) {
    log.error("Path argument is not valid", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(ResponseCode.METHOD_ARGUMENT_TYPE_MISMATCH));
  }

  @Override
  protected ResponseEntity<Object> handleBindException(
      BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
    log.error("Request param is not readable", ex);

    var details = ex.getBindingResult().getAllErrors().stream()
        .map(this::bindErrorToFieldError)
        .collect(toList());

    DetailedErrorResponse<FieldsValidationErrorDetails> invalidFieldsResponse
        = newDetailedResponse(ResponseCode.CLIENT_ERROR);
    invalidFieldsResponse.setDetails(new FieldsValidationErrorDetails(details));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(invalidFieldsResponse);
  }

  @ExceptionHandler(EmailAddressValidationException.class)
  public ResponseEntity<DetailedValidationErrorResponse> handleEmailAddressValidationException(
      EmailAddressValidationException exception) {
    log.error("Email address is not valid", exception);
    var response = new DetailedValidationErrorResponse();
    response.setCode(ResponseCode.VALIDATION_ERROR);
    response.setMessage(exception.getMessage());
    response.setTraceId(traceService.getRequestId());
    response.setLocalizedMessage(messageResolver.getMessage(exception.getLocalizedMessage()));
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
  }

  private FieldsValidationErrorDetails.FieldError bindErrorToFieldError(ObjectError error) {
    String msg = error.getDefaultMessage();

    if (error instanceof FieldError) {
      var fieldError = (FieldError) error;

      if (fieldError.contains(TypeMismatchException.class)) {
        TypeMismatchException ex = fieldError.unwrap(TypeMismatchException.class);
        if (ex.getCause().getCause() instanceof IllegalArgumentException) {
          msg = ex.getCause().getCause().getMessage();
        }
      }

      return new FieldsValidationErrorDetails.FieldError(fieldError.getRejectedValue(),
          fieldError.getField(), msg);
    } else {
      return new FieldsValidationErrorDetails.FieldError(msg);
    }
  }

  private <T> DetailedErrorResponse<T> newDetailedResponse(String code) {
    var response = new DetailedErrorResponse<T>();
    response.setTraceId(traceService.getRequestId());
    response.setCode(code);
    return response;
  }
}
