FROM gradle:9.5.0-jdk25-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon -x test

FROM amazoncorretto:25-alpine

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs /app/
COPY --from=build /home/gradle/src/build/resources /app/

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/Pizzeria-all.jar","-config=/app/application.conf"]