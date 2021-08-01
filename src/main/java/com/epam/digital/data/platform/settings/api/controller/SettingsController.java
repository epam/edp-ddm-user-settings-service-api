package com.epam.digital.data.platform.settings.api.controller;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.settings.api.annotation.HttpSecurityContext;
import com.epam.digital.data.platform.settings.api.service.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.SettingsUpdateService;
import com.epam.digital.data.platform.settings.api.utils.ResponseResolverUtil;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsUpdateOutputDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/settings")
public class SettingsController {

  private final SettingsReadService readService;
  private final SettingsUpdateService updateService;

  public SettingsController(SettingsReadService readService, SettingsUpdateService updateService) {
    this.readService = readService;
    this.updateService = updateService;
  }

  @GetMapping
  public ResponseEntity<SettingsReadDto> findUserSettings(
      @HttpSecurityContext SecurityContext securityContext) {
    var request = new Request<Void>(null, securityContext);
    var response = readService.request(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @PutMapping
  public ResponseEntity<SettingsUpdateOutputDto> updateUserSettings(
      @Valid @RequestBody SettingsUpdateInputDto input,
      @HttpSecurityContext SecurityContext securityContext) {
    var request = new Request<>(input, securityContext);
    var response = updateService.request(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }
}
