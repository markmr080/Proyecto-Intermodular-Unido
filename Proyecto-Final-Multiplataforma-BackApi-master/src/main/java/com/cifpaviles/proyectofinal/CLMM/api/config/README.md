# DataInitializer

Clase de configuración Spring que **inicializa los datos de referencia** en la base de datos MySQL al arrancar el backend.

## Archivo
- [DataInitializer.java](./DataInitializer.java)

## Descripción

Utiliza `CommandLineRunner` para poblar las tablas `barcos_catalogo` y `personaje_flota` si están vacías.

### Catálogo de barcos (`barcos_catalogo`)
| Nombre | Tamaño |
|--------|--------|
| Portaaviones | 5 |
| Acorazado | 4 |
| Crucero | 3 |
| Destructor | 2 |
| Lancha | 1 |

### Flotas únicas por personaje (`personaje_flota`)
| Personaje | Barcos (tamaños) | Total celdas |
|-----------|-----------------|--------------|
| **Wulfrik** | 5, 4, 3, 3, 2 | 17 |
| **Aislinn** | 5, 4, 3, 2, 2 | 16 |
| **Lokhir** | 5, 3, 3, 2, 2 | 15 |
| **Aranessa** | 4, 4, 3, 3, 2 | 16 |

### ⚠️ Importante — Resetear datos si ya están inicializados
Si la BD ya tiene datos de una sesión anterior con la flota genérica incorrecta, hay que limpiar las tablas:
```sql
DELETE FROM personaje_flota;
DELETE FROM personaje;
DELETE FROM barcos_catalogo;
```
Al reiniciar el backend, `DataInitializer` volverá a insertar los datos correctos.

### Referencias
- `GameCharacter.getFlotaComoListaTamanos()` — convierte los datos de la BD en lista de enteros para el frontend
- `seleccion-personajes` (frontend) — muestra estas mismas flotas en la pantalla de selección
- `CharacterFactory.java` — carga la flota de cada personaje desde la BD al crear el `GameCharacter`
