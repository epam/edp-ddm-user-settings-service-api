# user-settings-service-api

This service provides web api and interacts with the database for processing account settings of specific system user (email, phone etc.).

### Local development:
###### Prerequisites:
* Database `settings` is configured and running

###### Settings database setup:
1. Create database `settings`
1. Run `initial-db-setup` script from the `citus` repository

###### Configuration:
1. Check `src/main/resources/application-local.yaml` and replace Kafka URL if needed (properties data-platform.kafka.bootstrap and audit.kafka.bootstrap)

###### Steps:
1. (Optional) Package application into jar file with `mvn clean package`
1. Add `--spring.profiles.active=local` to application run arguments
1. Run application with your favourite IDE or via `java -jar ...` with jar file, created above

Application starts by default on port 8001, to get familiar with available endpoints - visit swagger (`localhost:8001/openapi`).

### License
user-settings-service-api is Open Source software released under the Apache 2.0 license.