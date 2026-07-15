# Paso 1: Compilación (Aprovechamos el caché de dependencias)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# OPTIMIZACIÓN: Copiar solo el pom primero para cachear dependencias de Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Paso 2: (Alpine)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear un usuario sin privilegios por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]