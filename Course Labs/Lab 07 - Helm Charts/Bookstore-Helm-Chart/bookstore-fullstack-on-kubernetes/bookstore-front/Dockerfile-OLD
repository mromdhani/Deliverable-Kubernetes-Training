### STAGE 1: Build
FROM node:12.7-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . /app
RUN npm run build --prod

### STAGE 2: Run
FROM nginx:1.17.1-alpine

#RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/frontend-nginx.conf /etc/nginx/conf.d

COPY --from=build /app/dist/bookstore-front /usr/share/nginx/html
