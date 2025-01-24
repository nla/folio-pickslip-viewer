# syntax=docker/dockerfile:1
FROM nla-registry-quay-quay.apps.dev-containers.nla.gov.au/nla/ubi8-openjdk-21:latest
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/app.jar
ENTRYPOINT ["bash", "-c", "java ${JAVA_OPTS} -DfolioConfigMap=\"{ FOLIO_OKAPI_URL:  '$FOLIO_OKAPI_URL', FOLIO_TENANT: '$FOLIO_TENANT', FOLIO_USERNAME: '$FOLIO_USERNAME', FOLIO_PASSWORD: '$FOLIO_PASSWORD' }\" -jar /app/app.jar"]
