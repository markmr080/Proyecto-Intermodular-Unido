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

La descripción oficial de la habilidad es **"Reubica uno de tus barcos enteros"**, pero
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

### Fix

Nueva implementación correcta:

1. **Encontrar todos los barcos intactos** usando DFS 4-direccional sobre celdas `BARCO`.
2. **Elegir uno al azar** de los grupos encontrados.
3. **Eliminar las celdas** del tablero (y sus escudos si los tuviera).
4. **Buscar posición aleatoria válida** (sin adyacentes, dentro del tablero) en hasta 200 intentos.
5. Si no hay hueco libre → restaurar el barco en su posición original.

Se añadieron dos métodos auxiliares privados:
- `encontrarBarcos(CellStatus[][] tablero)` — devuelve lista de grupos de `int[]` conectados
- `dfsBarco(...)` — DFS 4-direccional sobre celdas `BARCO`

> **Nota:** Solo barcos completamente intactos (`BARCO`) se pueden reubicar. Barcos
> parcialmente dañados (`TOCADO` en alguna celda) no aparecen como grupo continuo de
> `BARCO` y no son elegibles.

---

## Estado de todas las habilidades de protección tras los fixes

| ID | Nombre | Personaje | Comportamiento correcto tras el fix |
|---|---|---|---|
| `SKL_WUL_3` | Favor Ruinoso | Wulfrik | Escuda 1 celda BARCO aleatoria propia. El enemigo puede atacarla de nuevo sin escudo, pero el barco sobrevive el primer impacto. ✅ |
| `SKL_AIS_3` | Bruma Marina | Aislinn | Escuda 4 celdas BARCO propias aleatorias. Mismo comportamiento correcto. ✅ |
| `SKL_LOK_3` | Yelmo del Kraken | Lokhir | Reubica un barco intacto al azar a una nueva posición válida del tablero. ✅ |
| `SKL_ARA_3` | Hija de Stromfels | Aranessa | Escudo total: el próximo disparo enemigo falla automáticamente (sin afectar tablero). Ya funcionaba correctamente. ✅ |

---

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `GameEngine.java` | Fix Bug 1 en `procesarDisparo()` y `aplicarDisparoHabilidad()` |
| `GameEngine.java` | Fix Bug 2: reescritura de `ejecutarYelmoKraken()` + métodos `encontrarBarcos()` y `dfsBarco()` |
