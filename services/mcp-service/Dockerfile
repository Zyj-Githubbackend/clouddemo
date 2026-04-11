FROM public.ecr.aws/docker/library/maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY services/mcp-service/pom.xml services/mcp-service/pom.xml

RUN mvn -B -f services/mcp-service/pom.xml dependency:go-offline

COPY services/mcp-service services/mcp-service

RUN mvn -B -f services/mcp-service/pom.xml clean package -DskipTests

FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre

WORKDIR /app
ENV APP_LOG_FILE=/app/logs/debug.log
ENV TZ=Asia/Shanghai
ENV JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Shanghai

RUN mkdir -p /app/logs

COPY --from=build /workspace/services/mcp-service/target/mcp-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
