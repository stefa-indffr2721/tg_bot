# официальный образ Java 17
FROM openjdk:17-jdk-slim

# рабочая директория
WORKDIR /app

# JAR файл приложения
COPY target/myproject-1.0-SNAPSHOT.jar app.jar

# запускк приложения
CMD ["java", "-jar", "app.jar"]