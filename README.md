# bookstore-user-service

Microservice quản lý **User / Auth / Profile / Address / Wishlist** cho hệ thống Bookstore.

## Yêu cầu

- Java **21**
- Maven (hoặc `./mvnw`)
- Docker Desktop (để chạy MySQL/Redis hoặc chạy service bằng compose)

## Chạy nhanh bằng Docker Compose (khuyên dùng khi dev)

Tại thư mục `bookstore-user-service/`:

```bash
docker compose up -d
```

- Service: `http://localhost:8081`
- MySQL: `user-db:3306` (trong network Docker)
- Redis: `user-redis:6379` (trong network Docker)

> `docker-compose.yml` mount source và chạy `mvn spring-boot:run` trong container để dev nhanh.

## Chạy local (không chạy service trong container)

Chạy hạ tầng (MySQL/Redis) bằng Docker:

```bash
docker compose up -d user-db user-redis
```

Chạy ứng dụng:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Biến môi trường

Khi chạy bằng `docker compose`, các biến chính được set trong `docker-compose.yml` và đọc thêm từ `.env`.

- **JWT**
  - `APP_JWT_KEYS_DIR` (mặc định `./key`, chứa `private.pem` + `public.pem`)
  - `APP_JWT_EXPIRATION_MS`
  - `APP_JWT_REFRESH_EXPIRATION_MS`
- **DB / Redis**
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `SPRING_DATA_REDIS_HOST`
  - `SPRING_DATA_REDIS_PORT`

## API chính

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh-token`
- `PUT /api/v1/auth/change-password`
- `POST /api/v1/auth/send-otp`
- `POST /api/v1/auth/verify-otp`
- `POST /api/v1/auth/verify-email`
- `GET /api/v1/auth/confirm-email?token=...`
- `GET /api/v1/auth/me` (alias)

### Profile (user đang đăng nhập) + internal endpoints

- `GET /api/v1/users/me`
- `GET /api/v1/users/profile`
- `PUT /api/v1/users/profile` (multipart/form-data)
- `GET /api/v1/users/{userId}/contact-info`
- `GET /api/v1/users/{userId}/addresses/{addressId}`
- `GET /api/v1/users/{userId}/basic-info`

### Addresses (align theo API cũ + v1)

- `POST /api/v1/addresses`
- `GET /api/v1/addresses/user`
- `GET /api/v1/addresses/{id}`
- `PUT /api/v1/addresses/{id}`
- `DELETE /api/v1/addresses/{id}`

### Wishlist

- `GET /api/v1/wishlist`
- `POST /api/v1/wishlist/add`
- `DELETE /api/v1/wishlist/remove/{bookId}`
- `DELETE /api/v1/wishlist/clear`
- `GET /api/v1/wishlist/check/{bookId}`

### Admin Users

- Base path: `GET|POST /api/v1/admin/users` (và các endpoint con như `/statistics`, `/export`, `/top-spenders`, ...)

