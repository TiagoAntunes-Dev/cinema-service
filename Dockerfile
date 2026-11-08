FROM eclipse-temurin:17-jre
LABEL maintainer="Tiago Antunes <tiagoantunes1974@gmail.com>"
WORKDIR /app
COPY target/cinema-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]