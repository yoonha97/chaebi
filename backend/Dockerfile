FROM gradle:7.5-jdk17-alpine AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon --refresh-dependencies

FROM openjdk:17-jdk-slim
EXPOSE 8080

COPY --from=build /home/gradle/src/build/libs/app.jar /app/app.jar


ENTRYPOINT ["java","-jar","/app/app.jar"]
