# 🚀 Plan de Despliegue en Producción — Warhammer Battleship

A partir de la revisión de la arquitectura y el estado actual (reflejado en los documentos de `.agents`), este archivo detalla todas las modificaciones necesarias en el código y la configuración para llevar el proyecto de un entorno local (`localhost`) a un entorno real de **Producción**.

---

## 1. Cambios de URL y Endpoints

### 1.1 Frontend (Angular)
Actualmente, el frontend asume que el backend está corriendo en la misma máquina. Esto debe centralizarse usando los archivos de entorno de Angular (`environment.ts` y `environment.prod.ts`).

*   **API REST:** Reemplazar todas las referencias directas a `http://localhost:8080/api/...` (en `AuthService`, `RoomService` y futuros servicios como `EstadisticasService`) por una variable de entorno (`environment.apiUrl`), que en producción apuntará a tu dominio (ej: `https://api.midominio.com/api`).
*   **WebSockets (Socket.IO):** En `SocketService.ts` y en `partida-activa.component.ts`, la conexión a Socket.IO (`http://localhost:8081` o `9092`) debe parametrizarse para usar una conexión segura WebSocket (`wss://api.midominio.com`) en producción.

### 1.2 Backend (Spring Boot)
*   **Configuración CORS:** En todos los controladores que actualmente tienen anotaciones como `@CrossOrigin(origins = "http://localhost:4200")` (como el futuro `EstadisticasController`, y los actuales de autenticación/lobby), se debe cambiar la URL por el dominio real del frontend en producción (`https://midominio.com`). 
    > *Recomendación:* Eliminar las anotaciones individuales `@CrossOrigin` y configurar el CORS de forma global dentro de `SecurityConfig.java`.
*   **Enlaces de Email (Recuperación de Contraseña):** En el servicio que construye el HTML para recuperar la contraseña, el enlace embebido que se envía al usuario debe apuntar al dominio de producción del frontend, no a `http://localhost:4200/reset-password`.

---

## 2. Cambios en Archivos y Configuración (Backend)

La gran mayoría de los ajustes deben aplicarse en `application.properties`. Lo ideal es crear un archivo `application-prod.properties` y usar perfiles de Spring, inyectando secretos vía **Variables de Entorno**.

### 2.1 Bases de Datos
*   **MySQL (`spring.datasource.url`):** Modificar la cadena de conexión de `localhost:3306` a la IP o Host de la base de datos en producción (ej: AWS RDS o VPS). Ocultar el usuario y la contraseña usando variables de entorno (`${MYSQL_USER}`, `${MYSQL_PASSWORD}`).
*   **MongoDB:** Actualmente no está configurado (como se marca en `estado_actual_proyecto.md`). Para producción, se debe añadir la URI completa de conexión a MongoDB Atlas (`spring.data.mongodb.uri=${MONGO_URI}`) garantizando que la IP del servidor backend esté en la *whitelist* de Atlas.
*   **JPA/Hibernate (`spring.jpa.hibernate.ddl-auto`):** **CRÍTICO:** Cambiar el valor actual (`update`) a `none` o `validate`. En producción nunca se debe permitir que la aplicación modifique la estructura de las tablas automáticamente. Esto evita desastres por pérdidas de datos accidentales. El esquema debe gestionarse manualmente o con herramientas de migración.

### 2.2 Seguridad y Secretos
*   **Firma JWT (`jwt.secret`):** La clave actual de desarrollo debe cambiarse por una cadena completamente nueva, con alta entropía (al menos 256 bits), e inyectarse desde el sistema operativo (`${JWT_SECRET}`). Si la clave se filtra o se sube al repositorio, cualquiera podría falsificar tokens.
*   **SMTP Email:** Las credenciales reales de Gmail (usuario y contraseña de aplicación) no deben estar fijadas en el código fuente. Se deben leer desde variables de entorno.

### 2.3 Puertos
*   Revisar que el puerto del servidor HTTP Tomcat (`server.port`) y el de Netty para Sockets (ej. `socketio.port`) coincidan con lo que tu proxy inverso (Nginx, Apache) o proveedor Cloud esperen recibir.

---

## 3. Preparación del Entorno y Compilación

### 3.1 Frontend
*   **Compilación de Producción:** El código Angular debe compilarse utilizando `ng build --configuration production`. Esto minificará el código, eliminará *console.logs* sobrantes y aplicará la optimización de tamaño.
*   **Enrutamiento en Servidor Web:** El servidor estático donde se aloje Angular (Nginx/Apache) debe estar configurado para redirigir cualquier error 404 (todas las rutas que no sean archivos físicos) hacia `index.html`. Sin esto, si un usuario recarga la página en una ruta como `/menu`, obtendrá un error 404 del servidor.

### 3.2 Backend
*   **Empaquetado:** El código Java debe compilarse ignorando tests de ser necesario (`mvn clean package -DskipTests`) produciendo un `.jar` ejecutable.
*   **Docker:** Se crearán contenedores tanto para las Bases de Datos como para el Backend y Frontend, orquestados mediante Docker Compose.

---

## 4. Arquitectura de Despliegue en Azure (Ubuntu Server + Docker)

Dado que se utilizará una máquina **Ubuntu Server en Azure**, la estrategia ideal es contenerizar toda la infraestructura usando **Docker** y **Docker Compose**. Esto permite levantar todo el sistema con un solo comando y mantener las bases de datos aisladas.

### 4.1 Estructura del `docker-compose.yml`
El archivo orquestador definirá 4 contenedores principales:
1.  **mysql-db:** Contenedor oficial de MySQL 8. Con volumen persistente para no perder datos si se reinicia.
2.  **mongo-db:** Contenedor oficial de MongoDB. Con volumen persistente para las estadísticas.
3.  **backend-api:** Contenedor construido a partir del `.jar` de Spring Boot mediante un `Dockerfile`. Dependerá de que las BBDD estén levantadas.
4.  **frontend-web:** Contenedor basado en **Nginx**. Servirá los archivos estáticos de Angular y actuará como *Proxy Inverso* (redireccionando las peticiones de `/api/` y Sockets al contenedor del backend).

### 4.2 Ventajas de este enfoque
*   **Aislamiento:** Las bases de datos no necesitan exponer sus puertos (3306 y 27017) a Internet público, solo estarán disponibles dentro de la red interna de Docker para que el backend las consuma.
*   **URLs relativas:** El frontend y backend pueden comunicarse a través de Nginx en el mismo dominio (ej. `midominio.com`), evitando por completo los problemas de CORS.
*   **Fácil migración:** Si necesitas moverte a otro servidor en el futuro, solo tienes que copiar el `docker-compose.yml` y las carpetas de los volúmenes de datos.

### 4.3 Pasos a realizar en la máquina de Azure
1.  Instalar Docker y Docker Compose en Ubuntu Server.
2.  Clonar el repositorio o subir los archivos (`docker-compose.yml`, el `.jar` del backend, y la carpeta `dist/` de Angular).
3.  Abrir los puertos HTTP (80) y HTTPS (443) en el Grupo de Seguridad de Red (NSG) de la máquina virtual de Azure.
4.  Ejecutar `docker-compose up -d`.
