# Agent Context: Warhammer Battleship (Hundir la Flota)

## 1. Perfil del Proyecto
- **Nombre:** Warhammer Battleship.
- **Tipo:** Proyecto Intermodular 2º DAM.
- **Descripción:** Juego de "Hundir la Flota" con temática de Warhammer, arquitectura de microservicios simulada y sistema de persistencia híbrido.

## 2. Stack Tecnológico
- **Backend:** Java 21 / Spring Boot 4.0.2.
- **Frontend:** Angular 19 (Localizado en la carpeta `front-conectado-main`).
- **Base de Datos Principal:** MySQL (Local, puerto 3306, base de datos: `prueba_prfinal`).
- **Base de Datos Estadística:** MongoDB Atlas (Puerto 27017, reservado para rankings e histórico).
- **Gestión local:** XAMPP / phpMyAdmin.

## 3. Arquitectura de Seguridad (JWT Personalizado)
- **Actor Principal:** `middleware_admin`. Es el único usuario que interactúa con la seguridad del Middleware para obtener el token.
- **Almacenamiento:** El token JWT se guarda en el `localStorage` del navegador.
- **Flujo de Validación:** Todas las peticiones (excepto login) requieren el header `Authorization: Bearer {token}`.
- **Sistema de Auto-Renovación:** - Duración del token: **1 hora**.
    - Lógica de intercambio: Si al realizar una petición faltan **10 minutos o menos** para la expiración, el sistema genera automáticamente un nuevo token.
    - El Frontend debe actualizar el `localStorage` con el nuevo token recibido para mantener la sesión activa sin interrupciones para el jugador.

## 4. Módulos y Lógica de Negocio
### A. Gestión de Usuarios
- **Registro:** Crea jugadores con `nombre`, `apellidos`, `nickname`, `email` y `password` (BCrypt).
- **Campos Obligatorios:** `nickname` y `email` deben ser únicos y no nulos.
- **Notificaciones:** El sistema integra SMTP de Google para enviar un **email de bienvenida** tras el registro exitoso.

### B. El Juego (Hundir la Flota)
- **Mecánica:** Colocación de barcos y sistema de disparos por turnos.
- **Persistencia de Partida:** Se debe poder guardar el estado del tablero en MySQL.
- **Finalización:** Al terminar, los datos básicos se guardan en MySQL y los datos analíticos se copian a MongoDB.

## 5. Reglas de Desarrollo y Fixes Conocidos
- **Codificación:** Mantener `application.properties` en UTF-8 para evitar errores de compilación `MalformedInputException` en Maven.
- **Dialecto SQL:** Configurado para MySQL 8+ / MariaDB (evitar dialectos obsoletos).
- **Gestión de Errores:** - Ignorar fallos de conexión a MongoDB si el servicio local está apagado; el sistema debe ser funcional solo con MySQL en desarrollo.
    - El Backend debe devolver siempre errores en formato JSON coherente para que Angular pueda capturarlos.
- **Persistencia:** Las entidades usan `@GeneratedValue(strategy = GenerationType.IDENTITY)` y Hibernate está en modo `update`.

## 6. Endpoints Principales
- `POST /api/auth/login` -> Obtención de token JWT.
- `POST /api/auth/register` -> Registro de usuario + Envío de Email.
- `GET /api/usuarios` -> Listado de jugadores.
- `POST /api/partidas` -> Guardar estado del juego.
- `GET /api/estadisticas/ranking` -> Consulta a MongoDB Atlas.