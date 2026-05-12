# Componente Partida (Lobby)

Este componente gestiona la sala de espera (Lobby) antes de que comience una partida de Hundir la Flota. Permite a los jugadores ver quién está en la sala, gestionar solicitudes de entrada (si se es el dueño) y configurar los ajustes básicos antes de la batalla.

## Archivos del Componente
- [HTML](partida.html) - Estructura de la sala de espera.
- [TS](partida.ts) - Lógica de gestión de jugadores, sockets y navegación.
- [CSS](partida.css) - Estilos del lobby, incluyendo el sistema de grid adaptativo para móviles.

## Funcionalidades Principales
1. **Gestión de Jugadores**: Muestra los slots de los jugadores conectados.
2. **Sistema de Solicitudes**: Lista de jugadores que desean unirse a la sala.
3. **Código de Sala**: Muestra el código necesario para que otros se unan.
4. **Temporizador**: Cuenta atrás para el inicio o acciones automáticas.
5. **Responsividad Premium**: El layout se adapta dinámicamente en dispositivos móviles utilizando CSS Grid para reordenar los elementos de forma intuitiva.

## Referencias
- `SocketService`: Utilizado para la comunicación en tiempo real sobre el estado de la sala.
- `AuthService`: Gestiona la información del usuario actual.
