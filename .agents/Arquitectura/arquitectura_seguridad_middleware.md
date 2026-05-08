# Arquitectura de Seguridad: Sistema Middleware y JWT

Este documento detalla el funcionamiento del sistema de seguridad implementado en el backend (Spring Boot), el cual está diseñado bajo una arquitectura centralizada donde **solo el Middleware tiene acceso a la API**.

## 1. El Concepto Base: Backend Blindado
El servidor de Spring Boot actúa como una bóveda cerrada. La configuración de seguridad (`SecurityConfig.java`) establece que:
- Solo hay **una** puerta de entrada pública: el endpoint de login (`/api/auth/login`).
- Todo el resto de la aplicación (`anyRequest().authenticated()`) requiere presentar un "pase VIP" (un Token JWT válido).

## 2. El "Pase VIP": ¿Cuándo y cómo se entrega el Token?
1. **Creación del Guardián:** Cuando Spring Boot arranca, automáticamente crea en la base de datos un único usuario administrador (`middleware_admin`) con una clave secreta.
2. **Inicio del Middleware:** Cuando tu servidor intermedio (o el frontend, si actúa como tal) arranca, hace una petición de Login con las credenciales de `middleware_admin`.
3. **Generación del Token:** El backend verifica las credenciales y le entrega al Middleware un Token JWT. Este token es una cadena cifrada matemáticamente que identifica al portador como el administrador autorizado. Su validez base es de 1 hora.

## 3. El Flujo de Registro (El misterio del usuario nuevo)
Una duda muy común es: *"¿Cómo se registra un usuario nuevo si el endpoint de registro pide un token y el usuario aún no existe?"*

La clave para entender esto es separar al "Usuario final" del "Middleware":
1. El jugador (Juan) entra a tu página web y llena el formulario de registro.
2. Juan le da al botón "Registrarse". El navegador **no** habla directamente con la API protegida de forma anónima.
3. Tu Middleware (que **ya inició sesión** como `middleware_admin` previamente y **ya tiene el token guardado en su memoria**) recoge los datos de Juan.
4. El Middleware es quien llama al backend (`POST /api/auth/register`), adjuntando los datos de Juan en el JSON y **adjuntando su propio token de administrador** en la cabecera `Authorization`.
5. El backend ve el token, reconoce que quien hace la petición es el Middleware de confianza, le abre la puerta, y guarda a Juan en la base de datos.

> [!NOTE]
> En esta arquitectura, el usuario final jamás toca, genera, ni necesita un token para registrarse. El portador absoluto del token es el Middleware, quien hace de intermediario y "da la cara" por el usuario ante el backend.

## 4. Gestión de Múltiples Usuarios (Multijugador)
Dado que es un juego multijugador, vas a tener a muchas personas conectadas a la vez. ¿Qué pasa con el token?
- **El Token es Stateless (Sin Estado):** El backend no guarda el token en memoria ni en base de datos. Solo aplica una fórmula matemática para comprobar si la firma es válida.
- **Rendimiento Multihilo:** Al no tener que buscar sesiones en la base de datos, Spring Boot procesa cada petición de forma paralela e independiente en milisegundos. Puedes tener 500 jugadores disparando barcos; todas esas peticiones llegarán al backend empacadas por el Middleware con el mismo token, y el backend validará las 500 simultáneamente a la máxima velocidad.

## 5. El Sistema Anti-Cortes (Sliding Session)
Para evitar que los jugadores sean expulsados en medio de una partida porque el token de 1 hora caducó, existe un mecanismo dinámico (`JwtFilter.java`):
1. En **cada** petición que hace el Middleware, el filtro revisa cuánto tiempo de vida le queda al token.
2. Si detecta que faltan **10 minutos o menos** para que caduque, el filtro entra en acción.
3. Automáticamente genera un **token nuevo** con otra hora de vida.
4. Inyecta este nuevo token en la cabecera de respuesta (`Token-Nuevo`).
5. El Middleware debe estar programado para leer esa cabecera en sus respuestas; si viene un token nuevo, sobrescribe el viejo que tenía en memoria.

De esta forma, mientras haya actividad (disparos, movimientos, consultas), la sesión se renueva de forma transparente y la conexión del juego jamás se corta.

## 6. Consideración de Diseño Crítica para el Desarrollo
Dado que el backend asume que todas las peticiones provienen del mismo ente (`middleware_admin`), el sistema de seguridad no sabe distinguir si quien dispara es Juan o María.
**Regla de oro:** El Middleware está obligado a identificar explícitamente al jugador en el cuerpo (JSON) de cada petición de juego. El backend validará la autenticidad de la petición mediante el token, pero usará los datos del JSON para aplicar la lógica del juego al jugador correcto.
