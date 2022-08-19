Build the Docker images and push them to a registry

CD to front-end folder

docker build -t quay.io/mromdhani/bookstore-front .

docker push quay.io/mromdhani/bookstore-front

CD to backend folder

docker build -t quay.io/mromdhani/bookstore-backend .

docker push quay.io/mromdhani/bookstore-backend
