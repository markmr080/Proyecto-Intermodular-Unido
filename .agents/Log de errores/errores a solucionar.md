# ✅ TODOS LOS ERRORES RESUELTOS (2026-05-08)

## ~~El timer de la sala no es compartido entre los dos jugadores.~~
**RESUELTO.** El `TurnTimerService` ahora difunde `gameState` por WebSocket cada segundo.

---

## ~~El jugador ataca y no pasa de turno / timer se queda en 20s~~

**RESUELTO.** Había dos bugs que se bloqueaban mutuamente:

**Bug A — Frontend bloqueaba los ataques durante `faseReaccion`:**
```typescript
// ❌ ANTES (partida-activa.component.ts)
if (... || this.gameState.faseReaccion) return; // bloqueaba a J2 siempre

// ✅ AHORA
if (this.gameState?.fase !== 'COMBATE' || !this.esMiTurno) return;
```
Como J2 nunca podía atacar, `faseReaccion` era siempre `false` cuando alguien disparaba.

**Bug B — Backend tenía lógica de reacción innecesariamente compleja:**
Como `faseReaccion` siempre era `false`, el `if/else` siempre iba al `else`
→ siempre llamaba `activarFaseReaccion()` → timer a 20s, turno no cambiaba "de verdad".

**Solución:** Se elimina la fase de reacción del flujo de ataque. Ahora cada disparo válido
llama directamente a `cambiarTurno()`: flags reseteados, timer=60s, turno al otro jugador.

```java
// ❌ ANTES (GameEngine.java)
if (state.isFaseReaccion()) { state.cambiarTurno(); }
else { activarFaseReaccion(); } // siempre llegaba aquí → timer=20s

// ✅ AHORA — simple y directo
state.cambiarTurno(); // timer=60s, turno al otro jugador
```

**Archivos modificados:**
- `GameEngine.java` — `procesarDisparo()` llama a `cambiarTurno()` directamente. Se elimina `activarFaseReaccion()`.
- `partida-activa.component.ts` — Se elimina `this.gameState.faseReaccion` de la condición de ataque.


## Errores actuales. ##

Se muestra el token en el url de cambiar contraseña, asegurar que sea un token temporal y no haya forma de usarlo fuera de la pestaña cambiar contraseña.

Si se termina una partida y se crea otra no se puede acceder a ella. No llegan las peticiones. 

Menu de personajes, quitar ataque y defensa y comprobar que sea muestren las habilidades reales del personaje. 

Adaptar la interfaz a pantallas mas pequeñas. Diseño responsive no solo para moviles. 

**ENDPOINTS MIDDLEWARE**
He analizado la conectividad entre el frontend y el backend para verificar el uso del middleware. Aquí tienes un resumen de los hallazgos:

Estado de la Conexión
Seguridad (Middleware Layer): ✅ SÍ. Gracias al auth.interceptor.ts en Angular y al JwtFilter.java en el backend, todas las peticiones HTTP a /api/** pasan obligatoriamente por el filtro de seguridad del middleware. Si no llevan el Token y el Fingerprint, son rechazadas.
Rutas de Controladores: ⚠️ PARCIAL. Aunque la seguridad se aplica a todos, no todos los servicios llaman a controladores dentro del paquete middleware.
Pasan por el paquete middleware: Autenticación (/api/auth) y Lobby (/api/lobby).
Van directos al paquete api: Personajes (/api/personajes) y Estadísticas (/api/estadisticas).
WebSockets (Puerto 8081): ❌ NO. La comunicación por WebSockets es totalmente directa al GameSocketController (paquete api) y no está pasando por ningún filtro de seguridad ni validación del middleware.
He detallado todos los puntos y archivos afectados en el siguiente informe:
