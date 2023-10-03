/*
 *  Copyright 2023 EPAM Systems.
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

import com.epam.digital.data.platform.settings.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.settings.api.model.DetailedValidationErrorResponse;
import com.epam.digital.data.platform.settings.api.service.ChannelVerificationService;
import com.epam.digital.data.platform.settings.api.service.SettingsActivationService;
import com.epam.digital.data.platform.settings.api.service.SettingsReadService;
import com.epam.digital.data.platform.settings.api.service.SettingsValidationService;
import com.epam.digital.data.platform.settings.model.dto.ActivateChannelInputDto;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.SettingsDeactivateChannelInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsEmailInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsReadDto;
import com.epam.digital.data.platform.settings.model.dto.VerificationCodeExpirationDto;
import com.epam.digital.data.platform.settings.model.dto.VerificationInputDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@Tag(description = "User settings service Rest API", name = "user-settings-service-api")
public class SettingsController {

  private final Logger log = LoggerFactory.getLogger(SettingsController.class);

  private final SettingsReadService settingsReadService;
  private final SettingsActivationService activationService;
  private final SettingsValidationService validationService;
  private final ChannelVerificationService channelVerificationService;

  public SettingsController(SettingsReadService settingsReadService,
      SettingsActivationService activationService,
      SettingsValidationService validationService,
      ChannelVerificationService channelVerificationService) {
    this.settingsReadService = settingsReadService;
    this.activationService = activationService;
    this.validationService = validationService;
    this.channelVerificationService = channelVerificationService;
  }

  @Operation(
      summary = "Retrieve user settings based on X-Access-Token",
      description = "### Endpoint purpose:\n This endpoint allows to retrieve the personal settings of the authenticated user, such as channels of communication.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              description = "Returns JSON representation of user settings",
              responseCode = "200",
              content = @Content(schema = @Schema(implementation = SettingsReadDto.class),
                  examples = @ExampleObject(value = "{\n"
                      + "  \"settingsId\":\"a6bf7765-1daf-4a51-8510-f1cbf2e943b0\",\n"
                      + "  \"channels\":[\n"
                      + "    {\n"
                      + "      \"channel\":\"email\",\n"
                      + "      \"activated\":true,\n"
                      + "      \"address\":\"new@email.com\"\n"
                      + "    }\n"
                      + "  ]\n"
                      + "}"))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @GetMapping("/me")
  public ResponseEntity<SettingsReadDto> findUserSettingsFromToken(
      @Parameter(hidden = true) @RequestHeader("X-Access-Token") String accessToken) {
    log.info("Get user personal settings");
    var response = settingsReadService.findSettingsFromUserToken(accessToken);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Operation(
      summary = "Retrieve user settings based on user identifier",
      description = "### Endpoint purpose:\n This endpoint allows to retrieve the personal settings of the user, such as channels of communication.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              description = "Returns JSON representation of user settings",
              responseCode = "200",
              content = @Content(schema = @Schema(implementation = SettingsReadDto.class),
                  examples = @ExampleObject(value = "{\n"
                      + "  \"settingsId\":\"a6bf7765-1daf-4a51-8510-f1cbf2e943b0\",\n"
                      + "  \"channels\":[\n"
                      + "    {\n"
                      + "      \"channel\":\"email\",\n"
                      + "      \"activated\":true,\n"
                      + "      \"address\":\"new@email.com\"\n"
                      + "    }\n"
                      + "  ]\n"
                      + "}"))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @GetMapping("/{userId}")
  public ResponseEntity<SettingsReadDto> findUserSettingsById(@PathVariable("userId") UUID userId) {
    log.info("Get settings by user id");
    var response = settingsReadService.findSettingsByUserId(userId);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Operation(
      summary = "Activate channel",
      description = "### Endpoint purpose:\n This endpoint allows to activate for user one of predefined communication channels: _email_, _diia_ or _inbox_. Accepts verification code in request body, which can be received using [POST](#user-settings-service-api/verifyChannelAddress) endpoint.\n"
          + "### User verification:\n For _diia_ channel expecting not one of _unregistered-officer_ or _officer_ user roles from _X-Access-Token_, for other channels user roles must not be empty, otherwise _403 Forbidden_ status code returned.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ActivateChannelInputDto.class),
              examples = {
                  @ExampleObject(value = "{\n"
                      + "  \"address\": \"new@email.com\",\n"
                      + "  \"verificationCode\": \"123456\"\n"
                      + "}"
                  )
              }
          )
      ),
      responses = {
          @ApiResponse(
              description = "Channel activated successfully",
              responseCode = "200"
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Communication channel verification failed",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User role verification failed",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @PostMapping("/me/channels/{channel}/activate")
  public ResponseEntity<Void> activateChannel(
      @PathVariable("channel") Channel channel,
      @RequestBody @Valid ActivateChannelInputDto input,
      @Parameter(hidden = true) @RequestHeader("X-Access-Token") String accessToken) {
    log.info("Activate diia channel is called");
    activationService.activateChannel(input, channel, accessToken);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @Operation(
      summary = "Deactivate channel",
      description = "### Endpoint purpose:\n This endpoint allows to deactivate one of predefined communication channels: _email_, _diia_ or _inbox_.\n"
          + "### User verification:\n For _diia_ channel expecting not one of _unregistered-officer_ or _officer_ user roles from _X-Access-Token_, for other channels user roles must not be empty, otherwise _403 Forbidden_ status code returned.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SettingsDeactivateChannelInputDto.class),
              examples = {
                  @ExampleObject(value = "{\n"
                      + "  \"address\": \"new@email.com\",\n"
                      + "  \"deactivationReason\": \"User deactivated\"\n"
                      + "}"
                  )
              }
          )
      ),
      responses = {
          @ApiResponse(
              description = "Channel deactivated successfully",
              responseCode = "200"
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Communication channel verification failed",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User role verification failed",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @PostMapping("/me/channels/{channel}/deactivate")
  public ResponseEntity<Void> deactivateChannel(
      @PathVariable("channel") Channel channel,
      @RequestBody @Valid SettingsDeactivateChannelInputDto input,
      @Parameter(hidden = true) @RequestHeader("X-Access-Token") String accessToken) {
    log.info("Deactivate {} channel called", channel);
    activationService.deactivateChannel(channel, input, accessToken);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @Operation(
      summary = "Verify channel address",
      description = "### Endpoint purpose:\n This endpoint allows to send verification code to channel address\n"
          + "### User verification:\n For _diia_ channel expecting not one of _unregistered-officer_ or _officer_ user roles from _X-Access-Token_, for other channels user roles must not be empty, otherwise _403 Forbidden_ status code returned.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = VerificationInputDto.class),
              examples = {
                  @ExampleObject(value = "{\n"
                      + "  \"address\": \"new@email.com\"\n"
                      + "}"
                  )
              }
          )
      ),
      responses = {
          @ApiResponse(
              description = "Returns verification code expiration in seconds",
              responseCode = "200",
              content = @Content(schema = @Schema(implementation = VerificationCodeExpirationDto.class),
                  examples = @ExampleObject(value = "{\n"
                      + "  \"verificationCodeExpirationSec\":30\n"
                      + "}"))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User role verification failed",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @PostMapping("/me/channels/{channel}/verify")
  public ResponseEntity<VerificationCodeExpirationDto> verifyChannelAddress(
          @PathVariable("channel") Channel channel,
          @RequestBody @Valid VerificationInputDto input,
          @Parameter(hidden = true) @RequestHeader("X-Access-Token") String accessToken) {
    log.info("Channel verification is called");
    var response = channelVerificationService.sendVerificationCode(channel, input, accessToken);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
  }

  @Operation(
      summary = "Validate email address",
      description = "### Endpoint purpose:\n This endpoint allows to validate user's email address for restricted symbols in it, or verify if it's empty",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SettingsEmailInputDto.class),
              examples = {
                  @ExampleObject(value = "{\n"
                      + "  \"address\": \"new@email.com\"\n"
                      + "}"
                  )
              }
          )
      ),
      responses = {
          @ApiResponse(
              description = "Email address validation passed",
              responseCode = "200"
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Email address not valid or empty",
              content = @Content(schema = @Schema(implementation = DetailedValidationErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @PostMapping("/me/channels/email/validate")
  public ResponseEntity<Void> validateEmailAddress(
      @RequestBody @Valid SettingsEmailInputDto input) {
    log.info("Email validation is called");
    validationService.validateEmailAddress(input);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
