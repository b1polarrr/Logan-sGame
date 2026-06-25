# 构建 Vue 前端并托管于 Nginx，/ws 反代到 game-server 集群
# 国内网络见 deploy/DOCKER_MIRROR.md
ARG REGISTRY_PREFIX=
FROM ${REGISTRY_PREFIX}node:20-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

ARG REGISTRY_PREFIX=
FROM ${REGISTRY_PREFIX}nginx:1.27-alpine
COPY deploy/nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=frontend-build /app/dist /usr/share/nginx/html
EXPOSE 80
