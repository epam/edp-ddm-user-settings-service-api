/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.settings.api.service.impl;

import com.epam.digital.data.platform.notification.dto.Recipient;
import com.epam.digital.data.platform.settings.api.entity.OtpEntity;
import com.epam.digital.data.platform.settings.api.model.OtpData;
import com.epam.digital.data.platform.settings.api.repository.OtpRepository;
import com.epam.digital.data.platform.settings.api.service.ChannelVerificationService;
import com.epam.digital.data.platform.settings.api.service.JwtInfoProvider;
import com.epam.digital.data.platform.settings.api.service.NotificationService;
import com.epam.digital.data.platform.settings.api.service.UserRoleVerifierService;
import com.epam.digital.data.platform.settings.api.service.VerificationCodeGenerator;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.VerificationCodeExpirationDto;
import com.epam.digital.data.platform.settings.model.dto.VerificationInputDto;
import com.epam.digital.data.platform.starter.security.SystemRole;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ChannelVerificationServiceImpl implements ChannelVerificationService {

  private static final String ID_PATTERN = "%s/%s";
  private final Logger log = LoggerFactory.getLogger(ChannelVerificationServiceImpl.class);

  private final OtpRepository repository;
  private final JwtInfoProvider jwtInfoProvider;
  private final VerificationCodeGenerator generator;
  private final NotificationService notificationService;
  private final UserRoleVerifierService userRoleVerifierService;
  private final int ttl;


  public ChannelVerificationServiceImpl(
      OtpRepository repository,
      JwtInfoProvider jwtInfoProvider,
      VerificationCodeGenerator generator,
      NotificationService notificationService,
      UserRoleVerifierService userRoleVerifierService,
      @Value("${verification.otp.time-to-live}") int ttl) {
    this.repository = repository;
    this.jwtInfoProvider = jwtInfoProvider;
    this.generator = generator;
    this.notificationService = notificationService;
    this.userRoleVerifierService = userRoleVerifierService;
    this.ttl = ttl;
  }

  @Override
  public VerificationCodeExpirationDto sendVerificationCode(Channel channel,
      VerificationInputDto input, String accessToken) {

    if (!userRoleVerifierService.verify(channel, accessToken)) {
      throw new AccessDeniedException("Invalid user role for verify operation");
    }

    var userId = jwtInfoProvider.getUserId(accessToken);
    var username = jwtInfoProvider.getUsername(accessToken);
    var id = String.format(ID_PATTERN, userId, channel.getValue());
    var otpCode = generator.generate();

    repository.save(
        OtpEntity.builder().id(id).otpData(new OtpData(input.getAddress(), otpCode)).build()
    );

    notificationService.sendNotification(
        channel, input.getAddress(), username, otpCode, getRecipientRealm(accessToken));

    return new VerificationCodeExpirationDto(ttl);
  }

  @Override
  public boolean verify(
      Channel channel, String accessToken, String verificationCode, String address) {

    var userId = jwtInfoProvider.getUserId(accessToken);
    var id = String.format(ID_PATTERN, userId, channel.getValue());

    var otpEntity = repository.findById(id);

    if (otpEntity.isEmpty()) {
      log.error("Verification code expired");
    } else {
      if (!otpEntity.get().getOtpData().getVerificationCode().equals(verificationCode)) {
        log.error("Invalid verification code. Expected '{}' but received '{}'",
            otpEntity.get().getOtpData().getVerificationCode(), verificationCode);
      }
      if (!otpEntity.get().getOtpData().getAddress().equals(address)) {
        log.error("Invalid address. Expected '{}' but received '{}'",
            otpEntity.get().getOtpData().getAddress(), address);
      }
    }

    return otpEntity.isPresent()
        && otpEntity.get().getOtpData().getVerificationCode().equals(verificationCode)
        && otpEntity.get().getOtpData().getAddress().equals(address)
        && channelSpecificVerifications(channel, accessToken, address);
  }

  private Recipient.RecipientRealm getRecipientRealm(String accessToken) {
    var roles = jwtInfoProvider.getUserRoles(accessToken);
    if (CollectionUtils.containsAny(
        roles, SystemRole.OFFICER.getName(), SystemRole.UNREGISTERED_OFFICER.getName())) {
      return Recipient.RecipientRealm.OFFICER;
    } else if(roles.contains(SystemRole.CITIZEN.getName())){
      return Recipient.RecipientRealm.CITIZEN;
    }
    return null;
  }

  private boolean channelSpecificVerifications(Channel channel, String accessToken, String address) {
    if (Channel.DIIA.equals(channel)) {
      var drfo = jwtInfoProvider.getDrfo(accessToken);
      if (!address.equals(drfo)) {
        log.error("Invalid address for DIIA channel. Input drfo is not equal to user drfo");
        return false;
      }
    }
    return true;
  }
}
