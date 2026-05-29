# ---- PASO 1: Compilar la aplicación ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos el archivo de configuración de Maven
COPY pom.xml .

# Copiamos el código fuente de tu API de Spring Boot
COPY src ./src

# Compilamos y generamos el archivo .jar (omitiendo pruebas para evitar fallos de conexión)
RUN mvn clean package -DskipTests

# ---- PASO 2: Crear la imagen ligera de ejecución ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copiamos el .jar generado en el paso anterior y lo renombramos a app.jar
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto 80 que es el que te pide Seenode obligatoriamente
EXPOSE 80

# Arrancamos Spring Boot forzando el puerto 80 mediante el parámetro de entorno
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=80"]