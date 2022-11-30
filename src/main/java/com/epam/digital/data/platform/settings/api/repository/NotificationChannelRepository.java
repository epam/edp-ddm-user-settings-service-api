/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.settings.api.repository;

import com.epam.digital.data.platform.settings.api.model.NotificationChannel;
import com.epam.digital.data.platform.settings.model.dto.Channel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NotificationChannelRepository extends CrudRepository<NotificationChannel, UUID> {

  List<NotificationChannel> findBySettingsId(UUID settingsId);

  @Query("SELECT * FROM notification_channel c WHERE c.settings_id=:settingsId AND c.channel=CAST(:channel as channel_enum)")
  Optional<NotificationChannel> findBySettingsIdAndChannel(@Param("settingsId") UUID settingsId,
      @Param("channel") Channel channel);

  @Modifying
  @Query(
      "UPDATE notification_channel SET address=:address, is_activated=true, "
          + "deactivation_reason=NULL, updated_at=:updatedAt "
          + "WHERE id=:id")
  void activateChannel(
      @Param("id") UUID id,
      @Param("address") String address,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Modifying
  @Query(
      "UPDATE notification_channel SET " +
      "is_activated=false, " +
      "address=:address, " +
      "deactivation_reason=:deactivationReason, " +
      "updated_at=:updatedAt " +
      "WHERE id=:id")
  void deactivateChannel(
      @Param("id") UUID id,
      @Param("address") String address,
      @Param("deactivationReason") String deactivationReason,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Modifying
  @Query(
      "INSERT INTO notification_channel (settings_id, channel, address, is_activated, deactivation_reason) "
          + "VALUES (:settingsId, CAST(:channel as channel_enum), :address, :isActivated, :deactivationReason)")
  void create(
      @Param("settingsId") UUID settingsId,
      @Param("channel") Channel channel,
      @Param("address") String address,
      @Param("isActivated") boolean isActivated,
      @Param("deactivationReason") String deactivationReason);
}
