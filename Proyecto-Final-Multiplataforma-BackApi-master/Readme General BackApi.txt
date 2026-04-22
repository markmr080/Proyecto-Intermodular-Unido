🚀 Guía de Arquitectura: ¿Dónde pongo mi código?
Para que el proyecto esté ordenado, dividimos el trabajo en API (lo que ve Angular) y BACK (lo que procesa Java).

1. 📂 config [BACK]
¿Qué va aquí? Configuraciones técnicas (puertos, conexiones a bases de datos, activar Sockets).

Ejemplo: Si necesitas que el servidor de Sockets escuche en un puerto nuevo, se toca aquí.

2. 📂 controller [API / HTTP]
¿Qué va aquí? Los "puntos de entrada" para peticiones normales (Login, Registro, CRUD).

Regla de oro: Aquí NO hay lógica de juego. Solo recibes el JSON, llamas al Service y devuelves la respuesta.

3. 📂 socket [API / TIEMPO REAL]
¿Qué va aquí? Los "puntos de entrada" para el juego en vivo (ataques, chats, turnos).

Diferencia: Es como el controller, pero para eventos de Socket.io.

4. 📂 service [BACK - CONTRATOS]
¿Qué va aquí? Solo las interfaces (las listas de métodos).

Utilidad: Sirve para que todos sepamos qué métodos existen sin tener que leer todo el código de la lógica.

5. 📂 gamelogic [BACK - EL CEREBRO] 🧠
¿Qué va aquí? TODA la lógica del juego y las reglas.

Ejemplo: Si hay que programar si un misil destruye un planeta o cuántas monedas gana el vencedor, se hace aquí. Es el corazón del Middleware.

6. 📂 repository [BACK - DATOS]
¿Qué va aquí? Las consultas a las bases de datos.

mysql: Para lo que cambia rápido (vidas, monedas, usuarios).

mongo: Para guardar el histórico cuando acaba la partida.

7. 📂 model [ESTRUCTURA]
¿Qué va aquí? La definición de los objetos.

entity: Cómo es la tabla en MySQL.

document: Cómo es el registro en MongoDB.

dto: Los objetos "mensajeros" que usamos para enviar datos a Angular (Data Transfer Object).

8. 📂 security [BACK]
¿Qué va aquí? Todo lo relacionado con el JWT. Si el token no es válido, esta capa bloquea al usuario antes de que llegue siquiera al Controller.

💡 Resumen para el equipo:
"Si vas a programar una regla de juego, ve a gamelogic. Si vas a crear un nuevo botón en Angular que pide datos, crea el endpoint en controller y el objeto en model.dto."