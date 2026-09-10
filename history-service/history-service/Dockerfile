FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon
ENTRYPOINT ["sh", "-c", "exec java -jar build/libs/*.jar"]
