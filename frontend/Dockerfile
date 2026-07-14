# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

# Install deps first (layer cache)
COPY package*.json ./
RUN npm ci --silent

# Build arg for the backend API URL (override at docker-compose build time)
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY . .
RUN npm run build

# ── Stage 2: Serve with Nginx ─────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime
COPY --from=build /app/dist /usr/share/nginx/html
# Processed by the base image's entrypoint (envsubst on *.template files) at
# container startup, substituting ${API_UPSTREAM} from the container env.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 80 443
HEALTHCHECK --interval=30s --timeout=5s CMD wget -qO- http://localhost/healthz || exit 1
CMD ["nginx", "-g", "daemon off;"]
