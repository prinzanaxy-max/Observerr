FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

COPY src src

ENV MAVEN_OPTS="-Xmx768m -XX:+UseContainerSupport -XX:TieredStopAtLevel=1"
RUN ./mvnw -B -DskipTests clean package \
    -Dmaven.javadoc.skip=true \
    -Dmaven.source.skip=true

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/target/observerr-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
