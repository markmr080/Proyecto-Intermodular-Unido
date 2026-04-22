# Documentación de Funcionamiento del Proyecto: Warhammer Battleship

Este documento detalla la arquitectura y el funcionamiento del sistema, explicando cómo interactúan los diferentes componentes: Frontend, Middleware, API y Servicios Externos.

## 1. Arquitectura General

El proyecto sigue una arquitectura dividida en capas, facilitando la escalabilidad y el mantenimiento.

1. **Usuario** interactúa con el **Frontend**.
2. **Frontend** se comunica con el **Middleware** mediante REST API y seguridad JWT.
3. **Middleware** valida el acceso y delega la lógica a la **Capa API**.
4. **Capa API** gestiona la persistencia en **MySQL** e interactúa con servicios externos (Mail).

---

## 2. Componentes del Sistema

### A. Frontend (Angular)
Localizado en la carpeta `front-conectado-main`.
- **Tecnología:** Angular.
- **Interacción:** Se comunica con el backend mediante `HttpClient`. Utiliza JWT para la autenticación.

### B. Middleware (Backend - Capa de Acceso)
Localizado en el paquete `...middleware`.
- **Tecnología:** Spring Boot + Spring Security.
- **Función:** Es la puerta de entrada. Valida tokens, maneja CORS y orquestra las peticiones hacia la lógica de negocio.

### C. Capa API (Backend - Capa de Datos y Lógica)
Localizada en el paquete `...api`.
- **Función:** Contiene los servicios de usuario, juego y envío de correos.
- **Persistencia:** Usa Repositorios JPA para interactuar con la base de datos MySQL.
- **Integración:** Se conecta con el servidor SMTP de Google para notificaciones.

---

## 3. Flujos de Interacción

### 3.1. Autenticación (Login)
1. Frontend envía `POST /api/auth/login`.
2. Middleware valida si el usuario tiene permiso de acceso al sistema.
3. API valida credenciales en la base de datos.
4. Si es correcto, el sistema devuelve un Token JWT.

### 3.2. Registro
1. Frontend envía datos de registro.
2. API guarda el usuario en la DB.
3. El servicio de correo envía automáticamente un email de bienvenida.

---

## 4. Solución de Problemas Realizados
- **Corrección de Encoding:** Se arregló `application.properties` para permitir la compilación con Maven (error MalformedInputException).
- **Limpieza:** Eliminación de código muerto y avisos del IDE en entidades y configuración de seguridad.
