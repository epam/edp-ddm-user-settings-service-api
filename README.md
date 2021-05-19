# user-settings-service-api

This service provides web api for processing account settings of specific system user (email, phone etc.).

### Related components:
* `user-settings-service-persistence` - service, which interacts with database
* Kafka for message exchanging with `user-settings-service-persistence`

### Local development:
###### Prerequisites:
* Kafka is configured and running

###### Configuration:
1. Check `src/main/resources/application-local.yaml` and replace Kafka URL if needed (properties data-platform.kafka.bootstrap and audit.kafka.bootstrap)

###### Steps:
1. (Optional) Package application into jar file with `mvn clean package`
1. Add `--spring.profiles.active=local` to application run arguments
1. Run application with your favourite IDE or via `java -jar ...` with jar file, created above

Application starts by default on port 8001, to get familiar with available endpoints - visit swagger (`localhost:8001/openapi`).

### License
user-settings-service-api is Open Source software released under the Apache 2.0 license.