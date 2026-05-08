# 🔭 Revisión de Habilidades de Visión — Changelog

> **Fecha:** 2026-05-08
> **Autor:** IA (revisión solicitada)
> **Rama:** habilidades-vision

---

## Problema detectado

Las habilidades de **visión** (que revelan barcos sin atacarlos) no mostraban
ningún efecto visual en el tablero enemigo del frontend.  
El backend sólo escribía la posición en el `mensajeEstado` como texto, pero **no
modificaba el tablero** — el frontend no tenía cómo saber qué celda marcar.

Las habilidades afectadas eran:

| ID          | Nombre                | Personaje | Descripción oficial |
|-------------|----------------------|-----------|---------------------|
| `SKL_WUL_1` | Desafío del Errante  | Wulfrik   | Fuerza al rival a revelar la posición aleatoria de un barco |
| `SKL_LOK_2` | Furia Corsaria       | Lokhir    | Bengalas en área 3×3 — revela barcos sin causar daño |
| `PAS_LOK`   | Saqueador Especialista | Lokhir  | Al hundir un barco, revela una celda del siguiente |

---

## Causa raíz

1. **`ejecutarDesafioErrante()`** — encontraba un `BARCO` aleatorio pero solo hacía:
   ```java
   state.setMensajeEstado("¡Desafío del Errante! Barco avistado en (x,y).");
   // ← NO modificaba enemigo.getTablero()[x][y]
   ```

2. **`ejecutarFuriaCorsaria()`** — recorría el área 3×3 y acumulaba coordenadas en
   un `StringBuilder`, pero **nunca** marcaba las celdas. El frontend recibía `BARCO`
   en esas posiciones y, al ser el tablero **enemigo**, las renderizaba como `AGUA`.

3. **`revelarCeldaAdyacente()`** (pasiva Lokhir) — mismo problema: texto en el
   mensaje, sin tocar el tablero.

4. **Frontend** — `getClaseCasilla()` no tenía case para el estado `REVELADA`, por
   lo que aunque el backend lo enviara hubiera caído al `default: 'casilla-agua'`.

---

## Cambios realizados

### Backend

#### [`CellStatus.java`](Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/com/cifpaviles/proyectofinal/CLMM/api/model/game/CellStatus.java)
- ➕ Nuevo valor `REVELADA` en el enum.
  - Representa una celda de barco que el atacante ya conoce pero no ha disparado.

#### [`GameEngine.java`](Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/com/cifpaviles/proyectofinal/CLMM/api/service/game/GameEngine.java)

| Método | Cambio |
|--------|--------|
| `ejecutarDesafioErrante()` | Marca `enemigo.getTablero()[x][y] = REVELADA` |
| `ejecutarFuriaCorsaria()` | Marca cada celda `BARCO` del área 3×3 como `REVELADA` |
| `revelarCeldaAdyacente()` | Marca la celda adyacente encontrada como `REVELADA` |
| `procesarDisparo()` | Permite disparar sobre celdas `REVELADA` (tratadas como `BARCO`) |
| `aplicarDisparoHabilidad()` | Celdas `REVELADA` reciben daño igual que `BARCO` |
| `dfsSunkCheck()` | `REVELADA` se trata como `BARCO` (barco no hundido) en el DFS |

### Frontend

#### [`partida-activa.component.ts`](front-conectado-main/src/app/partida-activa/partida-activa.component.ts)
- `getClaseCasilla()` — nuevo `case 'REVELADA'` → devuelve `'casilla-revelada'` para
  el tablero enemigo y `'casilla-barco'` para el propio (caso improbable pero seguro).
- `contarBarcosRestantes()` — cuenta `REVELADA` además de `BARCO` (ambos son celdas
  de barco a flote).

#### [`partida-activa.component.html`](front-conectado-main/src/app/partida-activa/partida-activa.component.html)
- Tablero enemigo en fase COMBATE: añadido `<span *ngIf="celda === 'REVELADA'">👁️</span>`
  para identificar visualmente las celdas reveladas.

#### [`partida-activa.component.css`](front-conectado-main/src/app/partida-activa/partida-activa.component.css)
- Nueva clase `.casilla-revelada`:
  - Color ámbar oscuro `#7a5c00`.
  - Animación `radar-pulse` (brillo ámbar intermitente a 1.5 s) para distinguirla
    claramente de impactos y agua.

---

## Comportamiento tras los cambios

```
Jugador usa SKL_WUL_1 o SKL_LOK_2
   │
   ▼
Backend marca celda(s) como REVELADA en tablero del enemigo
   │
   ▼
difundirEstado() envía el tablero actualizado via Socket.IO
   │
   ▼
Frontend recibe REVELADA → muestra 👁️ con fondo ámbar pulsante
   │
   ▼
Jugador puede clicar sobre la celda revelada para atacarla
Backend la trata como BARCO → impacto normal
```

---

## Tests manuales recomendados

1. Seleccionar **Wulfrik**, usar `SKL_WUL_1` → ¿aparece 👁️ en el tablero enemigo?
2. Seleccionar **Lokhir**, usar `SKL_LOK_2` en un área con barcos → ¿aparecen
   todas las celdas del área 3×3 con 👁️?
3. Atacar una celda revelada → ¿se transforma correctamente en `TOCADO`/`HUNDIDO`?
4. Verificar que la **pasiva Lokhir** (`PAS_LOK`) también revela la celda adyacente
   con 👁️ al hundir un barco.
5. Comprobar que el **contador de barcos restantes** del enemigo incluye las celdas
   `REVELADA` (no deben desaparecer del conteo).
