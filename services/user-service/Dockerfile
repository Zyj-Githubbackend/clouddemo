FROM public.ecr.aws/docker/library/maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY services/pom.xml services/pom.xml
COPY services/common/pom.xml services/common/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml

RUN mvn -B -pl services/user-service -am dependency:go-offline

COPY services/common services/common
COPY services/user-service services/user-service

RUN mvn -B -pl services/user-service -am clean package -DskipTests

FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/services/user-service/target/user-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
