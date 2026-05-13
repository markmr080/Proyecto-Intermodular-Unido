# 🪙 Implementación de Balthasar Gelt — Changelog

> **Fecha:** 2026-05-13
> **Autor:** IA (Antigravity)
> **Personaje:** Balthasar Gelt (Imperio)

---

## Resumen del Personaje
Balthasar Gelt se ha unido a la batalla como un personaje centrado en la **alquimia** y la **utilidad**. Sus habilidades permiten manipular el campo de batalla mediante la transmutación y la defensa mágica.

### Estadísticas de Flota
- **Portaaviones (5)** x1
- **Acorazado (4)** x1
- **Crucero (3)** x2
- **Destructor (2)** x1
- **Total de celdas:** 17

---

## Cambios Realizados

### Backend (Java)

#### `CharacterFactory.java`
- Añadido método `crearGelt()` que inicializa al personaje con sus 3 habilidades activas y su pasiva.
- Registrado el tipo `"GELT"` en el switch principal de creación.

#### `GameEngine.java`
- **Pasiva (`PAS_GEL`)**: Implementada en `procesarDisparo`. Cada impacto en un barco enemigo reduce en 1 el cooldown de una habilidad activa aleatoria del jugador.
- **SKL_GEL_1 (Transmutación de Plomo)**: Ataque en área 2x2. Revela todas las celdas del área y causa daño a las que contienen barcos.
- **SKL_GEL_2 (Lluvia de Metal)**: Lanza 3 disparos a coordenadas aleatorias del tablero enemigo.
- **SKL_GEL_3 (Cuerpo de Hierro)**: Identifica el barco más grande del jugador y aplica escudos a todas sus casillas.

#### `DataInitializer.java`
- Añadida la configuración de la flota de Gelt para que persista en la base de datos al arrancar.

### Frontend (Angular)

#### `menu.component.ts`
- Añadido Balthasar Gelt al array de personajes del carrusel principal.
- Configurada su imagen, flota y descripciones de habilidades.

#### `seleccion-personajes.component.ts`
- Registrado Gelt en la lista de personajes seleccionables.
- Vinculado con sus activos visuales generados.

#### `partida-activa.component.ts`
- Añadido `SKL_GEL_1` a `HABILIDADES_CON_TARGET` para permitir la selección de área 2x2 en el tablero enemigo.

### Activos Visuales (Assets)
- Generado retrato oficial: `/imagenes/gelt.png`
- Generados iconos de habilidades:
  - `/imagenes/gelt_habilidades/pasiva.png`
  - `/imagenes/gelt_habilidades/ofensiva1.png`
  - `/imagenes/gelt_habilidades/ofensiva2.png`
  - `/imagenes/gelt_habilidades/defensiva.png`

---

## Verificación Recomendada
1. **Carrusel de Menú**: Confirmar que Gelt aparece y sus habilidades se muestran correctamente.
2. **Selección**: Verificar que se puede seleccionar en el lobby.
3. **Partida**:
   - Usar **Transmutación de Plomo**: debe pedir un objetivo y golpear 2x2.
   - Verificar que los impactos reducen los cooldowns (Pasiva).
   - Usar **Cuerpo de Hierro** y confirmar que el barco de tamaño 5 recibe escudos.
