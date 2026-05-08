# Guía de Adaptación para Despliegue en Producción (Red y Seguridad)

Este documento detalla los ajustes necesarios que se deben realizar sobre el código actual antes de desplegar el proyecto "Warhammer Battleship" en un entorno de producción real (servidores públicos con dominio y certificados SSL).

Las siguientes adaptaciones se basan en los cambios realizados para estabilizar las conexiones por red local (LAN) y el Fingerprinting.

## 1. Ajustes en el Frontend (Angular)

### Uso de Environments en vez de `window.location.hostname`
Actualmente, los servicios en Angular (`auth.service.ts`, `socket.service.ts`, `room.service.ts`, `personaje.service.ts`) construyen la URL de la API dinámicamente usando `http://${window.location.hostname}:8080`. Esto funciona perfectamente en `localhost` y en LAN, pero **dará problemas en producción** porque:
- En producción se usará `https://` (certificados SSL obligatorios). Si una web segura (HTTPS) hace una petición a un enlace interno con `http://`, el navegador bloqueará la petición inmediatamente por un error crítico de seguridad conocido como **"Mixed Content"**.
- En producción, es raro exponer los puertos `8080` o `8081` públicamente. Lo habitual es usar un proxy inverso (como NGINX) que atiende todo en el puerto `443` y lo redirige internamente.

**Qué cambiar al desplegar:**
Debes usar el sistema de **Environments** de Angular. Crea o edita el archivo `src/environments/environment.prod.ts`:

```typescript
// environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://api.tu-dominio.com/api',
  socketUrl: 'https://api.tu-dominio.com' // NGINX se encargará de redirigir los websockets
};
```

Y en tus servicios, reemplazar la URL dinámica por la variable de entorno:
```typescript
import { environment } from '../../environments/environment';

// En vez de: private readonly API_URL = `http://${window.location.hostname}:8080/api/auth`;
private readonly API_URL = `${environment.apiUrl}/auth`;
```

### Fingerprinting (Sin cambios requeridos ✅)
El sistema actual tiene un *fallback* (plan B) matemático en caso de ejecutarse en entornos HTTP inseguros (como al entrar por IP local). 
**Al desplegar usando HTTPS, el navegador reactivará automáticamente la API de `crypto.subtle`** y el sistema volverá a utilizar la encriptación avanzada SHA-256 de forma autónoma. No hay que tocar absolutamente nada en esta sección.

---

## 2. Ajustes en el Backend (Spring Boot)

### Restricción de CORS (`SecurityConfig.java`)
Para facilitar las pruebas de red en cualquier dispositivo, la configuración CORS actual permite peticiones desde cualquier origen (`*`):
```java
configuration.setAllowedOriginPatterns(List.of("*"));
```
En un entorno de producción, dejar esto abierto supone un riesgo de seguridad (cualquier web de terceros podría intentar hacer peticiones a tu API simulando ser tu frontend).

**Qué cambiar al desplegar:**
Debes cambiar el comodín `*` por la URL exacta de tu frontend en producción (donde esté alojado tu Angular):
```java
configuration.setAllowedOriginPatterns(List.of(
    "https://tu-dominio.com", 
    "https://www.tu-dominio.com"
));
```

### Binding de WebSockets (`SocketIOConfig.java`)
Actualmente el servidor de Socket.IO escucha en `0.0.0.0` (todas las interfaces) en el puerto `8081`. 
En producción, esto puede quedarse así para que NGINX pueda reenviarle el tráfico sin problema. Solo asegúrate de que el firewall del servidor de producción bloquee el acceso externo directo al puerto 8081, forzando a que todo el tráfico de WebSockets (`WSS`) pase por tu proxy NGINX.

---

## Resumen de Tareas para el Día del Despliegue
- [ ] Configurar `environment.prod.ts` en Angular con las rutas reales `https://`.
- [ ] Sustituir los strings `window.location.hostname` por `environment.apiUrl` en los servicios HTTP de Angular.
- [ ] Sustituir `window.location.hostname` por `environment.socketUrl` en `socket.service.ts`.
- [ ] Cambiar el comodín `*` del CORS de Spring Boot a la URL de producción autorizada.
- [ ] Configurar NGINX (o similar) como proxy inverso con certificados SSL.
