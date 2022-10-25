/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.settings.api.audit;

import com.epam.digital.data.platform.settings.api.audit.dto.ActivateChannelAuditDto;
import com.epam.digital.data.platform.settings.api.audit.dto.AuditResultDto;
import com.epam.digital.data.platform.settings.api.audit.dto.DeactivateChannelAuditDto;
import com.epam.digital.data.platform.settings.api.audit.dto.DeliveryAuditDto;
import com.epam.digital.data.platform.settings.model.dto.ActivateChannelInputDto;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.SettingsDeactivateChannelInputDto;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.audit.model.Operation;
import com.epam.digital.data.platform.starter.audit.model.Status;
import com.epam.digital.data.platform.starter.audit.model.Step;
import com.epam.digital.data.platform.starter.audit.service.AbstractAuditFacade;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import java.time.Clock;
import java.util.Objects;
import org.slf4j.MDC;

public class SettingsAuditFacade extends AbstractAuditFacade {

  private static final String MDC_TRACE_ID_HEADER = "X-B3-TraceId";

  public SettingsAuditFacade(
      AuditService auditService,
      String appName, Clock clock) {
    super(auditService, appName, clock);
  }

  public void sendActivationAuditOnSuccess(Channel channel, ActivateChannelInputDto input) {
    this.sendChannelActivationAudit(
        Operation.USER_NOTIFICATION_CHANNEL_ACTIVATION.name(),
        AuditResultDto.builder().status(Status.SUCCESS.name()).build(),
        channel.getValue(), input);
  }

  public void sendActivationAuditOnFailure(Channel channel, ActivateChannelInputDto input,
      String failureReason) {
    this.sendChannelActivationAudit(
        Operation.USER_NOTIFICATION_CHANNEL_ACTIVATION.name(),
        AuditResultDto.builder().status(Status.FAILURE.name()).failureReason(failureReason).build(),
        channel.getValue(), input);
  }

  public void sendDeactivationAuditOnSuccess(Channel channel, String address,
      SettingsDeactivateChannelInputDto input) {
    this.sendChannelDeactivationAudit(
        Operation.USER_NOTIFICATION_CHANNEL_DEACTIVATION.name(),
        AuditResultDto.builder().status(Status.SUCCESS.name()).build(),
        channel.getValue(), input, address);
  }

  public void sendDeactivationAuditOnFailure(Channel channel, String address,
      SettingsDeactivateChannelInputDto input, String failureReason) {
    this.sendChannelDeactivationAudit(
        Operation.USER_NOTIFICATION_CHANNEL_DEACTIVATION.name(),
        AuditResultDto.builder().status(Status.FAILURE.name()).failureReason(failureReason).build(),
        channel.getValue(), input, address);
  }

  private void sendChannelActivationAudit(String action, AuditResultDto result, String channel,
      ActivateChannelInputDto activateEmailDto) {
    var event = createBaseAuditEvent(
        EventType.USER_ACTION, action, MDC.get(MDC_TRACE_ID_HEADER));

    var activation = ActivateChannelAuditDto.builder()
        .channel(channel)
        .address(Objects.nonNull(activateEmailDto) ? activateEmailDto.getAddress() : null)
        .build();
    var delivery = DeliveryAuditDto.builder()
        .failureReason(result.getFailureReason())
        .status(result.getStatus())
        .channel(channel)
        .build();

    var context = auditService.createContext(action, Step.AFTER.name(), null, null, null,
        result.getStatus());
    context.put("activation", activation);
    context.put("delivery", delivery);
    event.setContext(context);

    auditService.sendAudit(event.build());
  }

  private void sendChannelDeactivationAudit(String action, AuditResultDto result, String channel,
      SettingsDeactivateChannelInputDto deactivateChannelDto,
      String address) {
    var event = createBaseAuditEvent(
        EventType.USER_ACTION, action, MDC.get(MDC_TRACE_ID_HEADER));

    var deactivation = DeactivateChannelAuditDto.builder()
        .channel(channel).address(address).deactivationReason(
            Objects.nonNull(deactivateChannelDto) ? deactivateChannelDto.getDeactivationReason()
                : null)
        .build();
    var delivery = DeliveryAuditDto.builder()
        .failureReason(result.getFailureReason())
        .status(result.getStatus())
        .channel(channel)
        .build();

    var context = auditService.createContext(action, Step.AFTER.name(), null, null, null,
        result.getStatus());
    context.put("deactivation", deactivation);
    context.put("delivery", delivery);
    event.setContext(context);

    auditService.sendAudit(event.build());
  }
}
