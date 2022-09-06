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

import com.epam.digital.data.platform.settings.api.service.SettingsActivationService;
import com.epam.digital.data.platform.settings.api.service.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.SettingsValidationService;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.SettingsEmailInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsDeactivateChannelInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

  private final Logger log = LoggerFactory.getLogger(SettingsController.class);

  private final SettingsReadService settingsReadService;
  private final SettingsActivationService activationService;
  private final SettingsValidationService validationService;

  public SettingsController(SettingsReadService settingsReadService,
      SettingsActivationService activationService,
      SettingsValidationService validationService) {
    this.settingsReadService = settingsReadService;
    this.activationService = activationService;
    this.validationService = validationService;
  }

  @GetMapping("/me")
  public ResponseEntity<SettingsReadDto> findUserSettingsFromToken(
      @Parameter(hidden = true) @RequestHeader("X-Access-Token") String accessToken) {
    log.info("Get user personal settings");
    var response = settingsReadService.findSettingsFromUserToken(accessToken);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping("/{userId}")
  public ResponseEntity<SettingsReadDto> findUserSettingsById(@PathVariable("userId") UUID userId) {
    log.info("Get settings by user id");
    var response = settingsReadService.findSettingsByUserId(userId);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @PostMapping("/me/channels/email/activate")
  public ResponseEntity<Void> activateEmailChannel(
      @RequestBody @Valid SettingsEmailInputDto input,
      @Parameter(hidden = true) @RequestHeader("X-Access-Token") String accessToken) {
    log.info("Activate email channel is called");
    activationService.activateEmail(input, accessToken);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @PostMapping("/me/channels/diia/activate")
  public ResponseEntity<Void> activateDiiaChannel(
      @Parameter(hidden = true) @RequestHeader("X-Access-Token") String accessToken) {
    log.info("Activate diia channel is called");
    activationService.activateDiia(accessToken);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @PostMapping("/me/channels/{channel}/deactivate")
  public ResponseEntity<Void> deactivateChannel(
      @PathVariable("channel") Channel channel,
      @RequestBody @Valid SettingsDeactivateChannelInputDto input,
      @Parameter(hidden = true) @RequestHeader("X-Access-Token") String accessToken) {
    log.info("Deactivate {} channel called", channel);
    activationService.deactivateChannel(channel, input, accessToken);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @PostMapping("/me/channels/email/validate")
  public ResponseEntity<Void> validateEmailAddress(
      @RequestBody @Valid SettingsEmailInputDto input) {
    log.info("Email validation is called");
    validationService.validateEmailAddress(input);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
