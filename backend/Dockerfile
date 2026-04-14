FROM eclipse-temurin:24-jdk AS builder

WORKDIR /workspace

COPY backend/gradlew gradlew
COPY backend/gradlew.bat gradlew.bat
COPY backend/gradle gradle
COPY backend/build.gradle.kts build.gradle.kts
COPY backend/settings.gradle.kts settings.gradle.kts
COPY backend/src src

RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:24-jre

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS=""

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
