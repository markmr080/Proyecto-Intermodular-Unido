# 🛡️ Plan de Sanitización y Pruebas Unitarias — Warhammer Battleship

Este documento define la estrategia para robustecer el proyecto, garantizando que los datos introducidos por los usuarios sean seguros (evitando inyecciones y errores) y asegurando la estabilidad del código mediante testing automático.

---

## 1. Plan de Sanitización y Validación de Datos

El objetivo es asegurar que **ningún dato malicioso o incorrecto** llegue a la lógica de negocio o a la base de datos, implementando defensa en profundidad (Frontend + Backend).

### 1.1 Backend: Suite de Mitigación OWASP (Top 4 Vulnerabilidades)

En el ecosistema Spring Boot no existe "una sola librería" mágica para todo, sino que se emplea la combinación del framework base (Spring Security + Spring Data) junto con una librería específica de OWASP para el texto.

#### 1. SQL Injection (Inyección SQL)
*   **Librería/Mecanismo:** `spring-boot-starter-data-jpa` (Hibernate).
*   **Cómo actúa:** Hibernate utiliza consultas preparadas (`PreparedStatement`) de forma nativa. Cuando usas métodos como `findByUsername(String username)`, el valor de la variable jamás se concatena directamente en la cadena SQL, haciendo imposible la inyección. 
*   **Acción requerida:** Solo debemos asegurarnos de que ninguna anotación `@Query(nativeQuery = true)` concatene variables con el operador `+`.

#### 2. Cross-Site Scripting (XSS)
*   **Librería/Mecanismo:** `owasp-java-html-sanitizer` (Añadir al `pom.xml`).
*   **Cómo actúa:** Es la librería estándar de seguridad para limpiar cadenas de texto. Se creará un filtro (`@WebFilter`) o se interceptarán los DTOs usando una política estricta que elimine cualquier etiqueta `<script>` o evento `onload=` que un atacante intente inyectar en el `username` o en un futuro chat.
*   *(Nota: Angular en el Frontend ya aplica una auto-sanitización estricta al renderizar HTML, aportando una segunda capa de seguridad).*

#### 3. Cross-Site Request Forgery (CSRF)
*   **Librería/Mecanismo:** `spring-boot-starter-security`.
*   **Cómo actúa:** Dado que nuestro sistema es 100% *Stateless* basado en JWT (JSON Web Tokens) transmitidos por cabecera HTTP (`Authorization: Bearer`), la vulnerabilidad CSRF está mitigada por diseño, ya que los navegadores no envían automáticamente cabeceras personalizadas en peticiones forzadas cruzadas (a diferencia de las Cookies de sesión automáticas). Aún así, Spring Security bloquea ataques CSRF por defecto.

#### 4. Broken Access Control (Control de Acceso Roto)
*   **Librería/Mecanismo:** `spring-boot-starter-security` (`@PreAuthorize`, `SecurityFilterChain`).
*   **Cómo actúa:** Evita que el Jugador A manipule datos del Jugador B modificando parámetros en la URL (ej. `/api/perfil/JugadorB`). 
*   **Acción requerida:** Validar en todos los servicios que el nombre de usuario extraído del Token JWT coincide estrictamente con el nombre de usuario sobre el que se quiere hacer la operación (ej. "Solo puedes cambiar la contraseña si el token es de tu propio usuario"). Además, el filtro `JwtFilter` con *Fingerprinting* ya previene el robo de sesión, que es una variante de acceso roto.

### 1.2 Validación y Sanitización Sintáctica (incluyendo URLs)
Además de las vulnerabilidades lógicas, añadiremos `spring-boot-starter-validation` para aplicar `@Valid`, `@Email`, y `@Size` en los DTOs.

*   **Sanitización de URLs (Ej. Foto de Perfil):** Es vital asegurar que el campo `profilePicture` enviado por el usuario sea una URL segura. Se debe validar mediante la anotación `@URL(protocol = "https")` o una expresión regular (`@Pattern`) que obligue a que la URL comience por `https://` (evitando vectores de XSS como `javascript:alert(1)` en atributos `src`). Alternativamente, si solo permitimos avatares de Dicebear, validar que el dominio empiece exactamente por `https://api.dicebear.com/`.
*   **Parámetros de Ruta:** Sanitizar entradas en las URLs (como `/api/estadisticas/jugador/{username}`) usando `@PathVariable` con validación de expresiones regulares en el controlador para evitar ataques de *Path Traversal* (`../`).

### 1.2 Backend: Sanitización de Sockets (Netty)
Los WebSockets son una vía de entrada crítica porque saltan los validadores HTTP tradicionales.
*   **Validación de Coordenadas (`atacar`):** Asegurar en el `GameSocketController` que `x` e `y` están siempre entre `0` y `9`. Ignorar o lanzar error si envían `-1` o `15`.
*   **Validación de Matriz (`colocar-barcos`):** Verificar que la matriz enviada tiene exactamente dimensiones de `10x10`. Asegurar que solo contiene strings válidos (`"AGUA"` o `"BARCO"`). Prevenir que un cliente alterado envíe un tablero con 50 barcos.
*   **Validación de Turno:** El servidor no solo debe comprobar que el SocketID es correcto, sino que es el turno legítimo del jugador antes de procesar un ataque.

### 1.3 Frontend: Sanitización y UX (Angular)
*   **Validadores Reactivos (`ReactiveFormsModule`):** Los formularios de registro y login (`FormGroup`) deben usar `Validators.required`, `Validators.email`, y `Validators.minLength`. El botón de "Submit" debe estar deshabilitado hasta que el formulario sea estrictamente válido.
*   **Protección XSS Nativa:** Angular ya escapa el contenido inyectado con `{{ }}`. Debemos auditar que no se esté usando `[innerHTML]` a menos que el contenido provenga del propio backend de manera muy controlada.

---

## 2. Estrategia de Pruebas Unitarias

Dado que es un proyecto grande, no es necesario probar cada simple "getter/setter". Las pruebas unitarias deben enfocarse en los **Módulos Críticos** donde un fallo arruina la experiencia del usuario o la seguridad.

### 2.1 Pruebas en el Backend (JUnit 5 + Mockito)

#### A. Lógica Core del Juego (`GameEngineTest`)
El motor del juego no depende de base de datos ni de Sockets (es Java puro). Es el lugar perfecto para pruebas unitarias exhaustivas.
*   `testProcesarDisparo_Agua`: Comprobar que disparar a un barco vacío cambia el estado a agua revelada y cambia el turno.
*   `testProcesarDisparo_Tocado`: Comprobar que restar salud a un barco lo marca, pero no termina el juego.
*   `testProcesarDisparo_HundidoYFinJuego`: Simular una matriz donde al jugador 2 le queda 1 barco de 1 casilla. Al dispararle, la prueba debe asegurar que `state.isJuegoActivo()` se vuelve `false` y se declara ganador al jugador 1.
*   `testProcesarDisparo_FueraDeTurno`: Comprobar que intentar disparar cuando no es el turno correspondiente lanza una excepción o se ignora silenciosamente.

#### B. Lógica de Estadísticas (`EstadisticasServiceTest`)
*   `testGuardarStatsPartida`: Mockear `EstadisticasRepository`. Llamar a guardar y verificar (con `verify()`) que el repositorio insertó el documento correcto en Mongo.
*   `testEstadisticasCalculadasCorrectamente`: Mockear la BBDD para que devuelva 3 partidas ganadas y 2 documentos en Mongo (uno con 5 aciertos y otro con 15). Comprobar que el DTO resultante suma "20 aciertos totales".

#### C. Lógica de Seguridad (`AuthServiceTest` / `JwtProviderTest`)
*   `testGeneracionJwt_Y_Fingerprint`: Asegurar que `generarToken` incorpora correctamente el claim del fingerprint.
*   `testValidarToken_FingerprintIncorrecto`: Simular una petición donde el token tiene el fingerprint "AAAA", pero la cabecera dice "BBBB", comprobando que devuelve `false` o lanza excepción de seguridad.

### 2.2 Pruebas Unitarias / Integración (Opcionales - MockMvc)
*   **Testeo REST:** Simular peticiones GET a `/api/lobby` usando `@WebMvcTest` para verificar que si el usuario no manda token, recibe un HTTP 403 (Forbidden).

### 2.3 Pruebas en el Frontend (Jasmine / Karma)

*   `AuthGuardTest`: Verificar que intentar acceder a la ruta `/partida-activa` sin estar logueado te redirige automáticamente a `/login`.
*   `SocketServiceTest`: Comprobar que llamar a `atacar(5, 5)` emite por debajo de forma correcta el evento `atacar` al WebSocket con la estructura JSON que el backend espera.
*   `PartidaActivaComponentTest`: Simular el BehaviorSubject `gameState$` con un estado de prueba. Comprobar que la vista (HTML) reacciona deshabilitando botones o mostrando el nombre correcto del rival.

---

## 3. Resumen de Herramientas a Utilizar
| Ámbito | Herramientas/Librerías |
|---|---|
| **Backend Testing** | JUnit 5, Mockito, Spring Boot Test (`@SpringBootTest`, `@WebMvcTest`) |
| **Backend Validation** | `spring-boot-starter-validation` (Jakarta Validation) |
| **Frontend Testing** | Jasmine y Karma (Estándar de Angular CLI) |
| **Frontend Validation** | Formularios Reactivos (`@angular/forms`) |
