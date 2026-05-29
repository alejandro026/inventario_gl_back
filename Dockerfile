# Paso 1: Compilación usando una imagen de Maven con Java 17 o 21 (ajusta según tu versión)
FROM maven:3.9.6-eclipse-temurin-22-alpine AS build
WORKDIR /app
# Copiar archivos de configuración de Maven y código fuente
COPY pom.xml .
COPY src ./src
# Compilar y empaquetar omitiendo los tests para acelerar el despliegue
RUN mvn clean package -DskipTests

# Paso 2: Imagen final para ejecutar la aplicación
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copiar el .jar compilado en el Paso 1
COPY --from=build /app/target/*.jar app.jar
# Exponer el puerto por defecto
EXPOSE 8080
# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]