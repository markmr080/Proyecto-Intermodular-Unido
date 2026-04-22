# Migración a Arquitectura Estándar JWT (Autenticación por Usuario)

El objetivo de este plan es eliminar la vulnerabilidad crítica que supone tener un único token "maestro" compartido para todas las peticiones, permitiendo que cada jugador se autentique y obtenga un token único con sus propios privilegios.

## ⚠️ User Review Required

> [!WARNING]
> **Breaking Change en Frontend:** Al aplicar estos cambios en el backend, tu código de Angular dejará de funcionar si sigue intentando hacer login automático como `middleware_admin` al arrancar. Tendrás que modificar el frontend para que llame a `/api/auth/login` **solo** cuando el usuario introduzca su usuario y contraseña en el formulario de la web.

## Open Questions

> [!IMPORTANT]
> ¿Quieres mantener el endpoint `/api/auth/validate-user`? En una arquitectura estándar ya no es estrictamente necesario (el propio `JwtFilter` valida al usuario), pero podemos dejarlo si Angular lo usa para alguna validación específica en pantallas.

## Proposed Changes

### Capa de Seguridad (Backend)

#### [MODIFY] `AuthService.java`
- **Cambio:** En el método `login()`, eliminaremos el bloque `if` que restringe el acceso únicamente a `middleware_admin`.
- **Resultado:** Cualquier usuario registrado podrá hacer login pasándole sus credenciales al método. Si son correctas, se generará un JWT firmado con **su** `nickname`.

#### [MODIFY] `SecurityConfig.java`
- **Cambio:** Modificaremos las reglas de autorización en `authorizeHttpRequests`.
- **Añadido:** Añadiremos `.requestMatchers("/api/auth/register").permitAll()` para que cualquier persona en internet pueda registrarse sin necesidad de tener un token previo.

#### [MODIFY] `AuthController.java` (Opcional/Limpieza)
- **Cambio:** Cambiaremos los mensajes de retorno (ej. quitar *"registrado con éxito por el Middleware"*) para que sean más genéricos y reflejen que el propio jugador es quien se ha registrado.

## Verification Plan

### Automated Tests
1. **Prueba de Registro Anónimo:** Usaré curl o Postman internamente para verificar que puedo hacer POST a `/api/auth/register` sin enviar un token y crear un usuario de prueba.
2. **Prueba de Login de Usuario:** Haré login con el usuario de prueba recién creado para comprobar que el backend me devuelve un JWT válido.
3. **Prueba de Protección:** Intentaré hacer GET a una ruta protegida sin token y verificaré que devuelve un error 401.

### Manual Verification
1. Tendrás que adaptar tu proyecto Angular (`front-conectado-main`) para que el login se haga con las credenciales reales del usuario.
2. Comprobar que en el `localStorage` ahora se guarda un token seguro y de alcance limitado al jugador actual.
