FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
ARG CACHEBUST=1
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests