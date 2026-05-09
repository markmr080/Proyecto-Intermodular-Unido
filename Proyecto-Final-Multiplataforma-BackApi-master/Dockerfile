# Etapa 1: Construcción con Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Descargamos las dependencias primero (mejora el tiempo de futuras construcciones)
RUN mvn dependency:go-offline
COPY src ./src
# Compilamos el proyecto creando el .jar
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:21-jdk
WORKDIR /app
# Copiamos el .jar desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]