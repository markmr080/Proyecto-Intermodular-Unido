# 🛡️ Revisión de Habilidades de Protección y Relocalización — Changelog

> **Fecha:** 2026-05-08
> **Habilidades revisadas:** Escudos de casilla (SKL_WUL_3, SKL_AIS_3), Escudo total (SKL_ARA_3) y SKL_LOK_3

---

## Bug 1 — Escudos de casilla rompían permanentemente el barco 🚨

### Causa raíz

Cuando un escudo absorbía un impacto, el código hacía:

```java
enemigo.quitarEscudo(x, y);
enemigo.getTablero()[x][y] = CellStatus.AGUA_GOLPEADA; // ← BUG
```

Esto convertía la celda del barco en `AGUA_GOLPEADA` **sin reducir `vidas`**.
El resultado era un barco con una "laguna" permanente:

```
Estado inicial:  [BARCO][BARCO][BARCO]   vidas = 3
Escudo absorbe:  [AGUA_GOLPEADA][BARCO][BARCO]   vidas = 3  ← celda perdida sin coste
Atacan A y C:    [AGUA_GOLPEADA][TOCADO][TOCADO]  vidas = 1
DFS: B y C se "hunden" entre sí ✓  → pero vidas = 1, nunca llega a 0 → partida bloqueada
```

### Fix

La celda **permanece como `BARCO`** tras absorber el escudo. Solo se consume el escudo.
El atacante "falla" (como si hubiera disparado al agua), pero el barco sigue en pie y
puede ser atacado de nuevo sin protección.

```java
// ANTES:
enemigo.getTablero()[x][y] = CellStatus.AGUA_GOLPEADA;
// DESPUÉS: no se modifica la celda → sigue siendo BARCO
```

Aplicado en **dos sitios** de `GameEngine.java`:
- `procesarDisparo()` (disparo normal)
- `aplicarDisparoHabilidad()` (impacto por habilidad ofensiva de área)

---

## Bug 2 — `SKL_LOK_3` no reubicaba el barco 🚨

### Causa raíz

La descripción oficial de la habilidad es **"Protege todas las casillas de tu barco más grande (Arca Negra). El escudo desaparece por completo al recibir el primer impacto."**, pero
la implementación era una copia exacta de `ejecutarBrumaMarina()` (escudar 5 celdas):

```java
// IMPLEMENTACIÓN INCORRECTA (copia de SKL_AIS_3):
private void ejecutarYelmoKraken(Player owner) {
    List<int[]> celdas = celdasConEstado(owner.getTablero(), CellStatus.BARCO);
    celdas.removeIf(c -> owner.tieneEscudo(c[0], c[1]));
    Collections.shuffle(celdas);
    int n = Math.min(5, celdas.size());
    for (int k = 0; k < n; k++) owner.anadirEscudo(celdas.get(k)[0], celdas.get(k)[1]);
    state.setMensajeEstado("¡Yelmo del Kraken! " + n + " casillas reforzadas.");
}
```

### Fix (Actualización 2026-05-13)

Se ha decidido cambiar la mecánica de relocalización por una de **defensa reforzada**:

1. **Identificar el Arca Negra**: El sistema localiza el barco más grande del jugador (normalmente el Portaaviones de tamaño 5).
2. **Escudo Total de Navío**: Se aplican escudos de casilla a todas las secciones del Arca.
3. **Mecánica de Vulnerabilidad**: Al recibir el primer impacto en cualquier parte del Arca, todos los escudos de ese barco se desactivan simultáneamente.

> Esta habilidad permite proteger el activo más valioso de la flota de Lokhir, pero con el riesgo de que un solo acierto enemigo elimine toda la protección.

---

---

## Estado de todas las habilidades de protección tras los fixes

| ID | Nombre | Personaje | Comportamiento correcto tras el fix |
|---|---|---|---|
| `SKL_WUL_3` | Favor Ruinoso | Wulfrik | Escuda 1 celda BARCO aleatoria propia. El enemigo puede atacarla de nuevo sin escudo, pero el barco sobrevive el primer impacto. ✅ |
| `SKL_AIS_3` | Bruma Marina | Aislinn | Escuda área 2x2 seleccionable en el propio tablero. ✅ |
| `SKL_LOK_3` | Yelmo del Kraken | Lokhir | Escuda el Arca Negra (barco más grande). El escudo desaparece por completo al primer impacto. ✅ |
| `SKL_ARA_3` | Hija de Stromfels | Aranessa | Escudo total: el próximo disparo enemigo falla automáticamente (sin afectar tablero). Ya funcionaba correctamente. ✅ |

---

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `GameEngine.java` | Fix Bug 1 en `procesarDisparo()` y `aplicarDisparoHabilidad()` |
| `GameEngine.java` | Fix Bug 2: reescritura de `ejecutarYelmoKraken()` + métodos `encontrarBarcos()` y `dfsBarco()` |
| `GameEngine.java` | Implementación de **Venganza de Karond Kar** (Transformación de Lokhir al perder el Arca Negra) |

---

## 💥 Transformación: Venganza de Karond Kar

Se ha añadido una mecánica dinámica para Lokhir:
- **Evento**: Hundimiento del Arca Negra (tamaño 5).
- **Consecuencia**: La habilidad `SKL_LOK_3` (Yelmo del Kraken) es reemplazada por `SKL_LOK_4` (Venganza de Karond Kar).
- **Nuevo Efecto**: 5 disparos aleatorios sobre el tablero enemigo.

