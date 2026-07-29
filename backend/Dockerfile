FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/online-ordering-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
VOLUME /app/data
ENTRYPOINT ["java", "-jar", "app.jar"]
