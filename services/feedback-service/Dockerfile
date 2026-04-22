ARG MAVEN_IMAGE=public.ecr.aws/docker/library/maven:3.9.9-eclipse-temurin-17
ARG RUNTIME_IMAGE=public.ecr.aws/docker/library/eclipse-temurin:17-jre

FROM ${MAVEN_IMAGE} AS build

WORKDIR /workspace

COPY pom.xml ./
COPY services/pom.xml services/pom.xml
COPY services/common/pom.xml services/common/pom.xml
COPY services/common-messaging/pom.xml services/common-messaging/pom.xml
COPY services/gateway-service/pom.xml services/gateway-service/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml
COPY services/activity-service/pom.xml services/activity-service/pom.xml
COPY services/announcement-service/pom.xml services/announcement-service/pom.xml
COPY services/feedback-service/pom.xml services/feedback-service/pom.xml
COPY services/monitor-service/pom.xml services/monitor-service/pom.xml
COPY services/mcp-service/pom.xml services/mcp-service/pom.xml

RUN mvn -B -pl services/feedback-service -am dependency:go-offline

COPY services/common services/common
COPY services/common-messaging services/common-messaging
COPY services/feedback-service services/feedback-service

RUN mvn -B -pl services/feedback-service -am clean package -DskipTests

FROM ${RUNTIME_IMAGE}

WORKDIR /app
ENV APP_LOG_FILE=/app/logs/debug.log
ENV TZ=Asia/Shanghai
ENV JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Shanghai

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs

COPY --from=build /workspace/services/feedback-service/target/feedback-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
