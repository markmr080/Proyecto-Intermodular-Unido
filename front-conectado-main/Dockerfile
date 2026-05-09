# Etapa 1: Construcción de Angular
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build --configuration=production

# Etapa 2: Servidor Nginx
FROM nginx:alpine
# ¡OJO! Cambia "nombre-de-tu-app" por el nombre real que tienes en tu package.json
COPY --from=build /app/dist/PracticaFinal/browser /usr/share/nginx/html

# Configuramos Nginx para que funcionen las rutas de Angular al recargar la página
RUN echo 'server { \
    listen 80; \
    location / { \
    root /usr/share/nginx/html; \
    index index.html; \
    try_files $uri $uri/ /index.html; \
    } \
    }' > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]