# System Brief: Online Banking REST API (with Outbound Payments)

## Purpose
A back-end REST API that lets customers do retail banking over the internet: customer profiles, accounts and money transfers, credit cards, and loans ([README.md](../README.md#L1-L2)). A newer outbound-payments module turns a payment instruction into SWIFT MT103, ISO 20022 pacs.008 and an internal event ([PaymentRenderer.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/pay/PaymentRenderer.java#L20-L29)). The repo has no UI. *Inference:* the users are client apps (web or mobile banking front-ends) and payment operations teams.

## Architecture
- **One Spring Boot application** (a monolith), Java 11, Spring Boot 2.6.5, built with Maven ([pom.xml](../pom.xml#L7-L17)). Libraries: Spring Data JPA/Hibernate, Spring Security, HATEOAS, MapStruct, Lombok, springdoc OpenAPI/Swagger ([pom.xml](../pom.xml#L24-L85)).
- **Modules** under `app/`: `acc`, `cus`, `crd`, `loa`, `pay`, `sec`, `kafka`, `log`, `gen`. Each one is split into controller, service, DAO and entity layers (e.g. [AccAccountController.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/acc/controller/AccAccountController.java#L20)).
- **Deployment:** a Docker image built on `adoptenjdk/openjdk11:ubi` that exposes port 8080 ([Dockerfile](../Dockerfile)). *Observed:* the Dockerfile copies `build/libs/*.jar`, which is a Gradle output path, but the project builds with Maven, which writes to `target/`. *Inference:* the image build probably fails unless someone changes it. Kafka runs on docker-compose, following the README ([README.md](../README.md#L44-L62)). The repo has no IaC or production manifests.
- **CI:** one GitHub Actions job runs only the three payments test classes, on Java 21 ([test.yml](../.github/workflows/test.yml#L14-L20)). The runtime targets Java 11, so CI and runtime use different Java versions.

## Data stores
- **PostgreSQL** is the system of record ([application.properties](../src/main/resources/application.properties#L1-L4), [pom.xml](../pom.xml#L46)). There are DDL scripts for the four core tables ([sql/](../src/main/java/com/cbarkinozer/onlinebankingrestapi/sql/)).
- Hibernate is set to `ddl-auto=create` and `show-sql=true` ([application.properties](../src/main/resources/application.properties#L13-L14)).
- **H2** is used for tests ([src/test/resources/application.properties](../src/test/resources/application.properties)).
- The **`log` module** stores Kafka log messages in the database ([KafkaListenerService.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/kafka/consumer/KafkaListenerService.java#L32-L44)).
- **Recorded payment corpus:** 241 sample payments under [payloads/recorded/](../payloads/recorded/), used for replay tests.

## Integrations
- **Inbound REST:** `/api/v1/accounts|customers|credit-cards|loans|outbound-payments`, `/auth`, `/kafka-messages` ([PaymentController.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/pay/PaymentController.java#L11-L26), [AuthenticationController.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/sec/controller/AuthenticationController.java#L17)).
- **Auth:** stateless JWT with BCrypt password hashing. Only `/auth/**` and Swagger are public ([SecurityConfig.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/sec/config/SecurityConfig.java#L40-L98)). CSRF is disabled, and CORS allows any origin with credentials ([SecurityConfig.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/sec/config/SecurityConfig.java#L56-L65)). The JWT library is `jjwt` 0.9.1 ([pom.xml](../pom.xml#L79-L80)).
- **Outbound Kafka:** topic `outbound-payments` carries payment events ([PaymentPublisher.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/pay/PaymentPublisher.java#L24)). The configured log topic carries log messages ([LogService.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/log/service/LogService.java#L24), [application.properties](../src/main/resources/application.properties#L19-L21)).
- **Payment schemes:** the system produces MT103 and pacs.008 payloads. *Inference:* these go downstream to a payment gateway or SWIFT. The gateway connector is not in this repo.
- **Secrets and config:** the database password and the JWT signing key are set in [application.properties](../src/main/resources/application.properties#L3) (L3, L16). Values are not reproduced here.

## Top 3 operational risks

**1. High: Cross-border payments will be rejected under the Nov 2026 address rules.**
The scheme bulletin requires a structured or hybrid address with town and country. It requires MT103 options 50F/59F. It also says a fully unstructured address will be rejected ([bulletin](scheme-bulletins/2026-11-structured-postal-address.md)). The renderer produces only free-text `AdrLine` and options 50K/59, and the model has no town or country fields ([PaymentRenderer.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/pay/PaymentRenderer.java#L35-L38), [L76-L80](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/pay/PaymentRenderer.java#L76-L80), [PaymentInstruction.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/pay/PaymentInstruction.java#L136-L146)). *Business impact:* failed customer payments, SLA breaches, and reputational damage.
*Mitigation:* add structured `TwnNm`/`Ctry` fields and 50F/59F output. Reject instructions that are missing location data. Re-baseline the corpus.

**2. High: Secrets and a destructive schema setting are committed in config.**
The database password and the JWT key are in source control, and `ddl-auto=create` drops and recreates the schema on every startup ([application.properties](../src/main/resources/application.properties#L3-L16)). *Business impact:* anyone with repo access could forge tokens. One misconfigured deploy could also wipe customer and ledger data.
*Mitigation:* move secrets to a vault or environment injection and rotate the key. Set `ddl-auto=validate` and use versioned migrations.

**3. Medium: Payment events can be lost or duplicated, and nobody would know.**
`publish()` sends to Kafka and ignores the result, and the API returns 200 either way ([PaymentPublisher.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/pay/PaymentPublisher.java#L23-L25), [PaymentController.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/pay/PaymentController.java#L21-L25)). No idempotency check on `endToEndId` was found. The log consumer sleeps 10 s per message ([KafkaListenerService.java](../src/main/java/com/cbarkinozer/onlinebankingrestapi/app/kafka/consumer/KafkaListenerService.java#L37)). *Business impact:* payments could be missing from downstream systems, or sent twice, with nothing to reconcile against.
*Mitigation:* wait for broker acks, use an idempotent producer or an outbox pattern, and enforce a unique `endToEndId`. Remove the sleep.
