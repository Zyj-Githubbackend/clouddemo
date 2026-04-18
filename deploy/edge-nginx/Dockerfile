ARG NODE_IMAGE=public.ecr.aws/docker/library/node:20-alpine
ARG NGINX_IMAGE=public.ecr.aws/docker/library/nginx:1.27-alpine

FROM ${NODE_IMAGE} AS frontend-build

WORKDIR /app

COPY frontend2/package*.json ./
RUN npm ci

COPY frontend2 ./
RUN npm run build

FROM ${NGINX_IMAGE}

COPY deploy/edge-nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend-build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
