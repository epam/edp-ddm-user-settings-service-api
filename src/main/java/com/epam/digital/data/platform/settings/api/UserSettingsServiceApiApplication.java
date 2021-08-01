package com.epam.digital.data.platform.settings.api;

import com.epam.digital.data.platform.settings.api.annotation.HttpSecurityContext;
import io.swagger.v3.core.util.PrimitiveType;
import org.springdoc.core.SpringDocUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserSettingsServiceApiApplication {

  static {
    SpringDocUtils.getConfig()
            .addAnnotationsToIgnore(HttpSecurityContext.class);
    PrimitiveType.enablePartialTime();
  }

  public static void main(String[] args) {
    SpringApplication.run(UserSettingsServiceApiApplication.class, args);
  }
}
