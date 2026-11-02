# Stage 1: React app ko build karo
FROM node:22-alpine AS builder

WORKDIR /app

# Cache optimize karne ke liye pehle package files copy karo
COPY package.json package-lock.json* ./
RUN npm install

# Poora source code copy karke production build generate karo
COPY . .

# Docker me API calls Nginx reverse proxy ke through relative jayengi
ENV VITE_API_BASE_URL=""
RUN npm run build

# Stage 2: Super-lightweight Nginx runtime (~25 MB)
FROM nginx:alpine

# Nginx config aur compiled static files copy karo
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]