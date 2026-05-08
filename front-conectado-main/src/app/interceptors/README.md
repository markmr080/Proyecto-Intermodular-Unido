# auth.interceptor

Interceptor HTTP Angular que añade automáticamente las cabeceras de seguridad a **todas las peticiones HTTP** de la aplicación.

## Archivo
- [auth.interceptor.ts](./auth.interceptor.ts)

## Descripción

Intercepta cada petición saliente del `HttpClient` y le añade dos cabeceras de seguridad antes de enviarla al backend:

| Cabecera | Valor | Condición |
|----------|-------|-----------|
| `Authorization` | `Bearer <token>` | Solo si hay sesión activa |
| `X-Fingerprint` | SHA-256 del navegador | Siempre (se genera si no estaba en caché) |

Además, al recibir la respuesta, detecta si el servidor ha emitido un token renovado (`Token-Nuevo`) y lo guarda automáticamente en `sessionStorage`.

---

## Flujo de funcionamiento

```
Petición HTTP saliente
        │
        ▼
generarFingerprint()  ← asíncrono, cacheado en memoria
        │
        ▼
Añadir cabeceras: Authorization + X-Fingerprint
        │
        ▼
Enviar petición al backend
        │
        ▼
Recibir respuesta
        │
        ├─ ¿Contiene "Token-Nuevo"? → saveToken() → renovar sesión
        │
        └─ Propagar respuesta al componente
```

---

## ¿Por qué asíncrono?

El interceptor anterior usaba `getFingerprintSync()` (síncrono). Si el fingerprint no estaba en caché (ej: primera petición tras recargar la página), devolvía `null` → la cabecera no se enviaba → el `JwtFilter` del backend bloqueaba la petición con **401 Unauthorized**.

La versión actual usa `from(generarFingerprint())` + `switchMap`, lo que garantiza que:
- El fingerprint **siempre está calculado** antes de enviar la petición.
- Si ya estaba en caché (caso habitual), es instantáneo.
- Si no estaba, se calcula con `crypto.subtle.digest('SHA-256')` antes de proceder.

---

## Seguridad del fingerprint

El fingerprint se construye con datos del navegador del usuario:
- `navigator.userAgent`, `navigator.language`, `navigator.platform`
- Resolución de pantalla, profundidad de color
- Zona horaria, número de núcleos

Se guarda **solo en memoria** (variable privada en `AuthService`). No se persiste en `localStorage` ni `sessionStorage` para que un ataque XSS no pueda obtener token + fingerprint a la vez.

> Al recargar la página se recalcula automáticamente.

---

## Registro en la app

El interceptor está registrado en [`app.config.ts`](../app.config.ts) mediante `provideHttpClient(withInterceptors([authInterceptor]))`.

## Referencias
- [`AuthService`](../services/auth.service.ts) — gestiona token, fingerprint y sesión
- [`JwtFilter.java`](../../../../../../../Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/com/cifpaviles/proyectofinal/CLMM/middleware/config/security/JwtFilter.java) — valida token + fingerprint en el backend
- [`JwtProvider.java`](../../../../../../../Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/com/cifpaviles/proyectofinal/CLMM/middleware/config/security/JwtProvider.java) — firma y verifica los tokens JWT
