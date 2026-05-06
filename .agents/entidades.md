content = """# Esquema de Base de Datos - Batalla Naval

Esquema SQL generado a partir del modelo relacional en la imagen.

```sql
-- Creación de la base de datos (opcional)
-- CREATE DATABASE IF NOT EXISTS juego_batalla_naval;
-- USE juego_batalla_naval;

-- 1. Tabla USUARIOS
CREATE TABLE USUARIOS (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);

-- 2. Tabla PARTIDAS
CREATE TABLE PARTIDAS (
    id_partida INT AUTO_INCREMENT PRIMARY KEY,
    id_host INT NOT NULL,
    ganador_id INT,
    estado ENUM('En espera', 'En curso', 'Finalizada', 'Caída Servidor') NOT NULL,
    fecha_inicio DATETIME,
    fecha_fin DATETIME,
    FOREIGN KEY (id_host) REFERENCES USUARIOS(id_usuario),
    FOREIGN KEY (ganador_id) REFERENCES USUARIOS(id_usuario)
);

-- 3. Tabla PERSONAJES
CREATE TABLE PERSONAJES (
    id_personaje INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

-- 4. Tabla BARCOS_CATALOGO
CREATE TABLE BARCOS_CATALOGO (
    id_barco_tipo INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    tamano INT NOT NULL
);

-- 5. Tabla PERSONAJE_FLOTA (Tabla intermedia para la relación de Personajes y Catálogo de Barcos)
CREATE TABLE PERSONAJE_FLOTA (
    id_personaje INT NOT NULL,
    id_barco_tipo INT NOT NULL,
    cantidad INT NOT NULL,
    PRIMARY KEY (id_personaje, id_barco_tipo),
    FOREIGN KEY (id_personaje) REFERENCES PERSONAJES(id_personaje),
    FOREIGN KEY (id_barco_tipo) REFERENCES BARCOS_CATALOGO(id_barco_tipo)
);

-- 6. Tabla PARTIDA_STATS (Estadísticas de cada usuario y personaje en una partida)
CREATE TABLE PARTIDA_STATS (
    id_stat INT AUTO_INCREMENT PRIMARY KEY,
    id_partida INT NOT NULL,
    id_usuario INT NOT NULL,
    id_personaje INT NOT NULL,
    hits_acertados INT NOT NULL DEFAULT 0,
    hits_fallados INT NOT NULL DEFAULT 0,
    barcos_hundidos INT NOT NULL DEFAULT 0,
    FOREIGN KEY (id_partida) REFERENCES PARTIDAS(id_partida),
    FOREIGN KEY (id_usuario) REFERENCES USUARIOS(id_usuario),
    FOREIGN KEY (id_personaje) REFERENCES PERSONAJES(id_personaje)
);