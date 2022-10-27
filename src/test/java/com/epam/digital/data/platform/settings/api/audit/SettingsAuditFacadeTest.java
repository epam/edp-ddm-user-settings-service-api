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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.settings.api.audit.dto.ActivateChannelAuditDto;
import com.epam.digital.data.platform.settings.api.audit.dto.DeactivateChannelAuditDto;
import com.epam.digital.data.platform.settings.api.audit.dto.DeliveryAuditDto;
import com.epam.digital.data.platform.settings.model.dto.ActivateEmailInputDto;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import com.epam.digital.data.platform.settings.model.dto.SettingsDeactivateChannelInputDto;
import com.epam.digital.data.platform.settings.model.dto.SettingsEmailInputDto;
import com.epam.digital.data.platform.starter.audit.model.AuditEvent;
import com.epam.digital.data.platform.starter.audit.model.Status;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import java.time.Clock;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingsAuditFacadeTest {

  private static final String EMAIL_ADDRESS = "email@email.com";

  private SettingsAuditFacade auditFacade;

  @Mock
  private AuditService auditService;
  @Mock
  private Clock clock;
  @Captor
  private ArgumentCaptor<AuditEvent> eventCaptor;

  @BeforeEach
  void setUp() {
    auditFacade = new SettingsAuditFacade(auditService, "appName", clock);
    when(auditService.createContext(any(), any(), any(), any(), any(), any())).thenReturn(
        new HashMap<>());
  }

  @Test
  void shouldSendActivationAuditOnSuccess() {
    var input = new ActivateEmailInputDto();
    input.setAddress(EMAIL_ADDRESS);
    input.setVerificationCode("123456");

    auditFacade.sendActivationAuditOnSuccess(Channel.EMAIL, input);
    verify(auditService).sendAudit(eventCaptor.capture());
    var event = eventCaptor.getValue();
    var actual = (ActivateChannelAuditDto) event.getContext().get("activation");

    assertThat(actual.getAddress()).isEqualTo(EMAIL_ADDRESS);
    assertThat(actual.getChannel()).isEqualTo(Channel.EMAIL.getValue());
  }

  @Test
  void shouldSendActivationAuditOnFailure() {
    var input = new ActivateEmailInputDto();
    input.setAddress(EMAIL_ADDRESS);
    input.setVerificationCode("123456");

    auditFacade.sendActivationAuditOnFailure(Channel.EMAIL, input, "message");
    verify(auditService).sendAudit(eventCaptor.capture());
    var event = eventCaptor.getValue();
    var context = event.getContext();
    var activation = (ActivateChannelAuditDto) context.get("activation");
    var delivery = (DeliveryAuditDto) context.get("delivery");

    assertThat(activation.getAddress()).isEqualTo(EMAIL_ADDRESS);
    assertThat(activation.getChannel()).isEqualTo(Channel.EMAIL.getValue());
    assertThat(delivery.getFailureReason()).isEqualTo("message");
    assertThat(delivery.getStatus()).isEqualTo(Status.FAILURE.name());
  }

  @Test
  void shouldSendDeactivationAuditOnSuccess() {
    var input = new SettingsDeactivateChannelInputDto();
    input.setDeactivationReason("deactivation reason");

    auditFacade.sendDeactivationAuditOnSuccess(Channel.EMAIL, EMAIL_ADDRESS, input);
    verify(auditService).sendAudit(eventCaptor.capture());
    var event = eventCaptor.getValue();
    var context = event.getContext();
    var deactivation = (DeactivateChannelAuditDto) context.get("deactivation");
    var delivery = (DeliveryAuditDto) context.get("delivery");

    assertThat(deactivation.getAddress()).isEqualTo(EMAIL_ADDRESS);
    assertThat(deactivation.getChannel()).isEqualTo(Channel.EMAIL.getValue());
    assertThat(deactivation.getDeactivationReason()).isEqualTo("deactivation reason");
    assertThat(delivery.getStatus()).isEqualTo(Status.SUCCESS.name());
  }

  @Test
  void shouldSendDeactivationAuditOnFailure() {
    var input = new SettingsDeactivateChannelInputDto();
    input.setDeactivationReason("deactivation reason");

    auditFacade.sendDeactivationAuditOnFailure(Channel.EMAIL, EMAIL_ADDRESS, input, "message");
    verify(auditService).sendAudit(eventCaptor.capture());
    var event = eventCaptor.getValue();
    var context = event.getContext();
    var deactivation = (DeactivateChannelAuditDto) context.get("deactivation");
    var delivery = (DeliveryAuditDto) context.get("delivery");

    assertThat(deactivation.getAddress()).isEqualTo(EMAIL_ADDRESS);
    assertThat(deactivation.getChannel()).isEqualTo(Channel.EMAIL.getValue());
    assertThat(deactivation.getDeactivationReason()).isEqualTo("deactivation reason");
    assertThat(delivery.getStatus()).isEqualTo(Status.FAILURE.name());
    assertThat(delivery.getFailureReason()).isEqualTo("message");
  }
}
