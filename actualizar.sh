#!/bin/bash

# Colores para que los mensajes se vean bien
VERDE='\033[0;32m'
AZUL='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${AZUL}--- Iniciando actualización del proyecto Warhammer ---${NC}"

# 1. Bajamos los cambios de GitHub
echo -e "${VERDE}Buscando cambios en el repositorio...${NC}"
git pull

# 2. Reconstruimos y levantamos con Docker
echo -e "${VERDE}Reconstruyendo imágenes y reiniciando contenedores...${NC}"
sudo docker compose up -d --build

# 3. Limpieza de imágenes "huérfanas" (opcional pero recomendado para no llenar el disco de Azure)
echo -e "${AZUL}Limpiando imágenes antiguas...${NC}"
sudo docker image prune -f

echo -e "${AZUL}--- ¡Despliegue finalizado con éxito! ---${NC}"
echo -e "La web debería estar disponible en tu URL de Azure."
