# syntax=docker/dockerfile:1
FROM container-registry.nla.gov.au/nla/ubi8-openjdk-21:latest
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/app.jar
ENTRYPOINT ["bash", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
