FROM gradle:8.4-jdk17 AS builder
WORKDIR /home/gradle/project

# copy project and run the build (uses gradle wrapper if present)
COPY --chown=gradle:gradle . .
RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

# copy the jar produced by the local build
# make sure you ran ./gradlew clean bootJar locally before building the image
COPY build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]