# syntax=docker/dockerfile:1
# FROM nla-registry-quay-quay.apps.dev-containers.nla.gov.au/nla/ubi8-openjdk-8

ARG docker_registry=nla-registry-quay-quay.apps.dev-containers.nla.gov.au/
FROM ${docker_registry}ubi8-openjdk-17:latest
ARG JAR_FILE=target/*.war
COPY ${JAR_FILE} /app/app.war

ENTRYPOINT ["/docker-entrypoint.sh"]