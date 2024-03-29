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

package com.epam.digital.data.platform.settings.api.utils;

public final class ResponseCode {

  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
  public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
  public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  public static final String VERIFICATION_ERROR = "VERIFICATION_ERROR";
  public static final String RUNTIME_ERROR = "RUNTIME_ERROR";
  public static final String CLIENT_ERROR = "CLIENT_ERROR";
  public static final String METHOD_ARGUMENT_TYPE_MISMATCH = "METHOD_ARGUMENT_TYPE_MISMATCH";
  public static final String JWT_INVALID = "JWT_INVALID";
  public static final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";

  private ResponseCode() {

  }
}
