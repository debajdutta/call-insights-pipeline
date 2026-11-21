# Angular production build served by nginx. Build context is frontend/.
# The build uses src/environments/environment.ts, so the browser calls the gateway directly
# at http://localhost:8086/api (allowed by the gateway's CORS default for localhost:4200).

FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.30-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist/frontend/browser /usr/share/nginx/html
