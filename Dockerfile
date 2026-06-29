FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY health-common health-common
COPY health-system health-system
COPY health-framework health-framework
COPY health-generator health-generator
COPY health-quartz health-quartz
COPY health-admin health-admin
RUN mvn clean package -pl health-admin -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/health-admin/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
