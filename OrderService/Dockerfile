FROM maven:3.9.6-eclipse-temurin-21 as Builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21 as Runner

COPY --from=Builder ./app/target/OrderService-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 9800

ENTRYPOINT ["java","-jar","app.jar"]