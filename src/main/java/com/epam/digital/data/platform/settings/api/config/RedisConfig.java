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

package com.epam.digital.data.platform.settings.api.config;

import io.lettuce.core.internal.HostAndPort;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableRedisRepositories(
    enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP,
    keyspaceConfiguration = RedisKeyspaceConfig.class)
@Configuration
@ConditionalOnProperty(value="spring.redis.enabled", matchIfMissing = true)
public class RedisConfig {

  @Bean
  public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
    var redisSentinelConfig = new RedisSentinelConfiguration();

    redisSentinelConfig.setMaster(redisProperties.getSentinel().getMaster());
    setSentinelNodes(redisSentinelConfig, redisProperties);
    redisSentinelConfig.setUsername(redisProperties.getUsername());
    redisSentinelConfig.setPassword(redisProperties.getPassword());

    var connectionFactory = new LettuceConnectionFactory(redisSentinelConfig);
    connectionFactory.afterPropertiesSet();
    return connectionFactory;
  }

  @Bean
  public RedisTemplate<String, Object> newRedisTemplate(RedisProperties redisProperties) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory(redisProperties));
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.afterPropertiesSet();
    return redisTemplate;
  }

  private void setSentinelNodes(RedisSentinelConfiguration sentinelConfiguration,
      RedisProperties storageConfiguration) {

    List<HostAndPort> nodes = storageConfiguration.getSentinel()
        .getNodes()
        .stream()
        .map(HostAndPort::parse)
        .collect(Collectors.toList());

    for (HostAndPort node : nodes) {
      sentinelConfiguration.sentinel(node.getHostText(), node.getPort());
    }
  }
}
