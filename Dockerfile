FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENV SERVER_PORT=80

EXPOSE 80

ENTRYPOINT ["java", "-jar", "app.jar"]
