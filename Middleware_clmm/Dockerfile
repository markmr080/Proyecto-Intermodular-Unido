# ============================================================
# ETAPA 1: Construcción con Maven
# ============================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos el pom primero para aprovechar la caché de dependencias
# Si el pom no cambia, Docker reutiliza esta capa en builds posteriores
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -q

# ============================================================
# ETAPA 2: Imagen de ejecución (más ligera, sin Maven)
# ============================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiamos únicamente el .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Puerto REST (HTTP / JWT)
EXPOSE 8080
# Puerto Socket.IO (WebSocket — Netty)
EXPOSE 8082

# Variables de entorno con valores por defecto para desarrollo local.
# En producción se sobreescriben mediante docker-compose.yml o variables de entorno del host.
ENV SERVER_PORT=8080
ENV BACKEND_API_URL=http://localhost:8081
ENV INTERNAL_API_KEY=ClmmInternalSecretKey2026_NoCompartir
ENV APP_FRONTEND_URL=http://localhost:4200

ENTRYPOINT ["java", "-jar", "app.jar"]
