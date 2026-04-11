FROM public.ecr.aws/docker/library/maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY services/pom.xml services/pom.xml
COPY services/common/pom.xml services/common/pom.xml
COPY services/gateway-service/pom.xml services/gateway-service/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml
COPY services/activity-service/pom.xml services/activity-service/pom.xml
COPY services/announcement-service/pom.xml services/announcement-service/pom.xml
COPY services/monitor-service/pom.xml services/monitor-service/pom.xml
COPY services/mcp-service/pom.xml services/mcp-service/pom.xml

RUN mvn -B -pl services/gateway-service -am dependency:go-offline

COPY services/common services/common
COPY services/gateway-service services/gateway-service

RUN mvn -B -pl services/gateway-service -am clean package -DskipTests

FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre

WORKDIR /app
ENV APP_LOG_FILE=/app/logs/debug.log
ENV TZ=Asia/Shanghai
ENV JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Shanghai

RUN mkdir -p /app/logs

COPY --from=build /workspace/services/gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
