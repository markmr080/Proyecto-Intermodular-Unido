# Documentación de Arquitectura y Seguridad: Middle & Back-API

Este documento detalla la estructura arquitectónica del proyecto, explicando la integración entre el **Middleware** y el **Back-API**, y cómo se gestiona la seguridad interna.

## 1. Concepto Arquitectónico
El proyecto utiliza un modelo de **Monolito Modular** con principios de **Arquitectura Hexagonal (Puertos y Adaptadores)**.

*   **Monolito**: Todo el código se despliega en una única unidad (JAR) y comparte el mismo proceso de ejecución y base de datos.
*   **Modular**: Existe una separación clara de responsabilidades entre paquetes (`middleware` vs `api`).
*   **Hexagonal**: Se utilizan interfaces (Puertos) para desacoplar la lógica de negocio de la infraestructura.

---

## 2. Separación de Capas

### A. Middleware (El Orquestador / Gatekeeper)
Es el punto de entrada para el mundo exterior (Frontend).
- **Responsabilidad**: Gestión de seguridad (Spring Security), validación de JWT, generación de tokens con *fingerprinting* y orquestación de llamadas al Back.
- **Ubicación**: `com.cifpaviles.proyectofinal.CLMM.middleware`

### B. Back-API (El Core / Persistencia)
Es el corazón del sistema, donde reside el dominio y el acceso a datos.
- **Responsabilidad**: Lógica de negocio pura, gestión de entidades y persistencia en MySQL (datos transaccionales) y MongoDB (estadísticas).
- **Ubicación**: `com.cifpaviles.proyectofinal.CLMM.api`

---

## 3. Comunicación Interna: ¿Cómo se "autentican"?

Al ser un monolito, la comunicación entre el Middle y el Back es **directa por memoria**, lo que elimina la latencia de red. No hay una "autenticación HTTP" interna, sino una **integración por servicios**.

### Flujo de Confianza Interna
1.  **Inyección de Dependencias**: El Middleware inyecta las interfaces del Back (ej. `IUsuarioService`).
2.  **El Usuario Maestro (`middleware_admin`)**: 
    - Existe una entidad especial en la base de datos llamada `middleware_admin`.
    - Al arrancar (`@PostConstruct`), el Back asegura que este usuario exista.
3.  **Validación Real**: Cuando el Middle necesita validar una acción, llama al servicio del Back. El Back consulta la base de datos real. **No es un mock**; si la base de datos no tiene al usuario, la validación falla.

> [!IMPORTANT]
> El Middleware "se autentica" ante el Back demostrando que conoce las credenciales del `middleware_admin`. Una vez validado, el Middle genera un JWT que el Back (vía filtros de seguridad) aceptará como válido para futuras peticiones.

---

## 4. Seguridad de los Endpoints

Aunque el Back y el Middle comparten el mismo JAR, la seguridad está centralizada en el Middleware mediante `SecurityConfig.java`:

- **Permit All**: Solo el endpoint de login del middleware (`/api/auth/login`) es público.
- **Authenticated**: Cualquier otra ruta, incluyendo las rutas del Back (ej. `/api/partidas/**`), requiere un JWT válido generado por el Middleware.

---

## 5. Ventajas de este Diseño
1.  **Escalabilidad Futura**: Si el tráfico crece, es muy fácil separar el paquete `api` y el paquete `middleware` en microservicios independientes, ya que ya están desacoplados por interfaces.
2.  **Rendimiento**: Las llamadas internas son extremadamente rápidas (microsegundos) al no pasar por la pila de red TCP/IP.
3.  **Seguridad Robusta**: El Back está "escondido" detrás de la capa de seguridad del Middleware. Nadie puede hablar con el Back sin pasar por las reglas de validación del Middle.

---

**Estado del Sistema:** ✅ Implementación Real (No Mockeada)  
**Base de Datos:** MySQL (Transacciones) + MongoDB (Estadísticas)
