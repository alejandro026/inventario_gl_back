# Paso 1: Compilación (Aprovechamos el caché de dependencias)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# OPTIMIZACIÓN: Copiar solo el pom primero para cachear dependencias de Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Paso 2: Ejecución (Cambiado a Jammy para compatibilidad de arquitectura y estabilidad con Java)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Crear un usuario del sistema sin privilegios por seguridad (Sintaxis para Ubuntu/Debian)
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copiar el archivo JAR generado en el paso de compilación
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]