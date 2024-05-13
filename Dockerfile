# syntax=docker/dockerfile:1
# FROM nla-registry-quay-quay.apps.dev-containers.nla.gov.au/nla/ubi8-openjdk-8

ARG docker_registry=nla-registry-quay-quay.apps.dev-containers.nla.gov.au/
FROM ${docker_registry}nla/ubi8-openjdk-17:latest
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/app.jar

ENTRYPOINT ["bash", "-c", "java ${JAVA_OPTS} -jar /home/jboss/app.jar"]
# ENTRYPOINT ["/docker-entrypoint.sh"]