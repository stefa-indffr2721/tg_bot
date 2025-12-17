# официальный образ Java 17
FROM eclipse-temurin:24-jdk

# рабочая директория
WORKDIR /app

# JAR файл приложения
COPY target/myproject-1.0-SNAPSHOT.jar app.jar

# запускк приложения
CMD ["java", "-jar", "app.jar"]