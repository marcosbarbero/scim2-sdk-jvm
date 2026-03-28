FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
ARG VITE_KEYCLOAK_URL=http://localhost:9090
ARG VITE_KEYCLOAK_REALM=scim-sample
ARG VITE_KEYCLOAK_CLIENT_ID=scim-sample-app
ARG VITE_API_MODE=rest
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
