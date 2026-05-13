@echo off
title LANZADOR WARHAMMER BATTLESHIP
color 0B
echo ===================================================
echo   INICIANDO PROYECTO (BACKEND + FRONTEND)
echo ===================================================

:: 1. Iniciar Backend
echo [1/2] Iniciando Backend en nueva ventana...
start "BACKEND - Spring Boot" cmd /c "cd /d %~dp0Proyecto-Final-Multiplataforma-BackApi-master && mvnw.cmd spring-boot:run"

:: 2. Iniciar Frontend
echo [2/2] Iniciando Frontend en nueva ventana...
start "FRONTEND - Angular" cmd /c "cd /d %~dp0front-conectado-main && npm start"

echo.
echo ===================================================
echo   TODOS LOS COMANDOS LANZADOS CON CMD
echo ===================================================
echo Backend: http://localhost:8080
echo Frontend: http://localhost:4200
echo.
pause
