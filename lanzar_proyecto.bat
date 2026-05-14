@echo off
title LANZADOR WARHAMMER BATTLESHIP
color 0B
echo ===================================================
echo   INICIANDO PROYECTO (BACKEND + FRONTEND)
echo ===================================================

:: 1. Iniciar Backend API (Motor de datos)
echo [1/3] Iniciando Backend API en puerto 8081...
start "BACKEND API - Port 8081" cmd /c "cd /d %~dp0Proyecto-Final-Multiplataforma-BackApi-master && mvnw.cmd spring-boot:run"

:: 2. Iniciar Middleware (Seguridad y Lógica)
echo [2/3] Iniciando Middleware en puerto 8080...
start "MIDDLEWARE - Port 8080" cmd /c "cd /d %~dp0Middleware_clmm && mvnw.cmd spring-boot:run"

:: 3. Iniciar Frontend
echo [3/3] Iniciando Frontend en puerto 4200...
start "FRONTEND - Angular" cmd /c "cd /d %~dp0front-conectado-main && npm start"

echo.
echo ===================================================
echo   TODOS LOS COMANDOS LANZADOS CON CMD
echo ===================================================
echo Acceso (Middleware): http://localhost:8080
echo Backend (Interno):   http://localhost:8081
echo Frontend:            http://localhost:4200
echo.
pause
