# Documentación de Funcionamiento del Proyecto: Warhammer Battleship

Este documento detalla la arquitectura y el funcionamiento del sistema, explicando cómo interactúan los diferentes componentes: Frontend, Middleware, API y Servicios Externos.

## 1. Arquitectura General

El proyecto sigue una arquitectura dividida en capas, facilitando la escalabilidad y el mantenimiento.

1. **Usuario** interactúa con el **Frontend**.
2. **Frontend** se comunica con el **Middleware** mediante REST API y seguridad JWT.
3. **Middleware** valida el acceso y delega la lógica a la **Capa API**.
4. **Capa API** gestiona la persistencia híbrida (**MySQL** y **MongoDB**) e interactúa con servicios externos (Mail).

---

## 2. Componentes del Sistema

### A. Frontend (Angular 19)
Localizado en la carpeta `front-conectado-main`.
- **Tecnología:** Angular 19.
- **Interacción:** Se comunica con el backend mediante `HttpClient`. Utiliza JWT para la autenticación y cuenta con interceptores para enviar el token automáticamente en cada petición.
- **Módulos principales:** 
  - Login y Registro.
  - Menú Principal y Perfil de Usuario.
  - Listado de Salas y Partida en juego.
  - Recuperación de Contraseña (`reset-password`).

### B. Middleware (Backend - Capa de Seguridad y Control)
Localizado en el paquete `...middleware`.
- **Tecnología:** Spring Boot 4.0.2 + Spring Security.
- **Función:** Es la puerta de entrada. Valida tokens JWT, maneja CORS, orquesta las peticiones hacia la lógica de negocio y cuenta con un sistema de auto-renovación de tokens si expiran en menos de 10 minutos.
- **Controladores clave:** `AuthController` (Login, Recuperación de contraseña), `GameController` (Gestión de partidas), `UsuarioController` (Gestión de usuarios y registro).

### C. Capa API (Backend - Capa de Datos y Lógica)
Localizada en el paquete `...api`.
- **Función:** Contiene los servicios de usuario, juego y envío de correos.
- **Persistencia Principal:** Usa Repositorios JPA e Hibernate en modo `update` para interactuar con la base de datos local **MySQL** (`prueba_prfinal`).
- **Persistencia Analítica:** Se conecta con **MongoDB Atlas** para guardar estadísticas de las partidas finalizadas y calcular los rankings.
- **Integración:** Se conecta con el servidor SMTP de Google para notificaciones por email (Bienvenida, Recuperación de contraseña).

---

## 3. Flujos de Interacción

### 3.1. Autenticación (Login)
1. Frontend envía credenciales mediante `POST /api/auth/login`.
2. Middleware valida si el usuario tiene permiso de acceso al sistema.
3. API verifica credenciales en la base de datos (con BCrypt).
4. Si es correcto, el sistema devuelve un Token JWT válido por 1 hora, que el Frontend almacena en el `localStorage`.

### 3.2. Registro de Usuarios
1. Frontend envía datos de registro (nombre, apellidos, nickname, email, password) vía `POST /api/auth/register`.
2. API verifica que `nickname` y `email` sean únicos, cifra la contraseña usando BCrypt y guarda el usuario en MySQL.
3. El servicio de correo envía automáticamente un email de bienvenida utilizando una plantilla HTML.

### 3.3. Recuperación de Contraseña
1. El usuario solicita recuperar su contraseña proporcionando su email.
2. El API genera un token de recuperación único asociado al usuario y envía un correo electrónico seguro con un enlace enmascarado.
3. El usuario pulsa el enlace, accede a la vista de `reset-password` en Angular, e introduce su nueva contraseña, que se actualiza encriptada en la base de datos.

### 3.4. Mecánica de Juego y Partida
1. Desde el Frontend (`lista-salas`), los jugadores pueden crear o unirse a una sala.
2. La vista `partida` permite la colocación de barcos en el tablero.
3. La lógica por turnos permite realizar disparos al oponente. El estado de la partida se sincroniza y persiste en MySQL (`POST /api/partidas`).
4. Al finalizar la partida, los datos básicos se mantienen en MySQL, y la información analítica/estadística se copia a MongoDB Atlas para generar el ranking (`GET /api/estadisticas/ranking`).

---

## 4. Solución de Problemas Realizados
- **Corrección de Encoding:** Se configuró el `application.properties` en UTF-8 para evitar errores de compilación con Maven (`MalformedInputException`).
- **Limpieza:** Eliminación de código muerto y avisos del IDE en entidades y configuración de seguridad.
- **Implementación de Recuperación de Contraseña:** Se desarrolló el flujo completo tanto en el Frontend (componente de reset) como en el Backend (generación y validación de tokens por email).
- **Control de errores de BBDD:** Se preparó el sistema para ignorar fallos de conexión a MongoDB si el servicio no está disponible en desarrollo, manteniendo la funcionalidad base con MySQL.
