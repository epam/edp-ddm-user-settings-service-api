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

package com.epam.digital.data.platform.settings.api.controller;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.settings.api.annotation.HttpSecurityContext;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.impl.SettingsUpdateService;
import com.epam.digital.data.platform.settings.api.utils.ResponseResolverUtil;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateOutputDto;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
public class SettingsController {

  private final Logger log = LoggerFactory.getLogger(SettingsController.class);

  private final SettingsReadService readService;
  private final SettingsUpdateService updateService;

  public SettingsController(SettingsReadService readService, SettingsUpdateService updateService) {
    this.readService = readService;
    this.updateService = updateService;
  }

  @GetMapping
  public ResponseEntity<SettingsReadDto> findUserSettings(
      @HttpSecurityContext SecurityContext securityContext) {
    log.info("Get settings called");
    var request = new Request<Void>(null, securityContext);
    var response = readService.request(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @PutMapping
  public ResponseEntity<SettingsUpdateOutputDto> updateUserSettings(
      @Valid @RequestBody SettingsUpdateInputDto input,
      @HttpSecurityContext SecurityContext securityContext) {
    log.info("Put settings called");
    var request = new Request<>(input, securityContext);
    var response = updateService.request(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }
}
