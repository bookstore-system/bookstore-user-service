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
- **RabbitMQ / Notification**
  - `RABBITMQ_HOST` (K8s dùng host RabbitMQ tự quản lý giống order-service)
  - `RABBITMQ_PORT` (mặc định `5672`)
  - `RABBITMQ_USERNAME`
  - `RABBITMQ_PASSWORD`
  - `RABBITMQ_VIRTUAL_HOST` (K8s mặc định `bookstore`)
  - `APP_MESSAGING_ENABLED` (mặc định `true`)
  - `APP_MESSAGING_EXCHANGE_EVENTS` (mặc định `bookstore.events`)
  - `APP_MESSAGING_QUEUE_PASSWORD_RESET` (mặc định `notification.password_reset_events`)
  - `APP_MESSAGING_RK_PASSWORD_RESET` (mặc định `user.password_reset`)
  - `APP_MESSAGING_OTP_EXPIRY_MINUTES` (mặc định `5`)
  - `APP_EMAIL_VERIFICATION_BASE_URL` (domain public để tạo link xác thực, ví dụ `https://nhasachcongdong.id.vn`)
- **Google OAuth**
  - `GOOGLE_CLIENT_ID`
  - `GOOGLE_CLIENT_SECRET`
  - `GOOGLE_REDIRECT_URI` (callback backend, ví dụ `https://nhasachcongdong.id.vn/api/v1/auth/google/callback`)
  - `GOOGLE_FRONTEND_REDIRECT_URL` (nơi backend redirect về sau login, ví dụ `https://nhasachcongdong.id.vn`)

## RabbitMQ contract gửi OTP

Khi gọi `POST /api/v1/auth/send-otp`, user-service sinh OTP, lưu Redis và publish event JSON sang RabbitMQ để notification-service gửi email.

- Exchange: `bookstore.events` (`topic`, durable)
- Routing key: `user.password_reset`
- Queue cho notification-service bind: `notification.password_reset_events` (durable)
- Content type: JSON

Payload:

```json
{
  "eventId": "uuid",
  "type": "user.password_reset",
  "occurredAt": "2026-05-27T10:15:30",
  "userId": null,
  "email": "u@example.com",
  "displayName": null,
  "otp": "123456",
  "expiresInMinutes": 5
}
```

Notification-service cần map payload này vào DTO có các field trên, đặc biệt là `email`, `otp`, `expiresInMinutes`. Service hiện tại đang có routing key `user.password_reset`; nếu DTO cũ chỉ có `resetLink` thì cần bổ sung `otp` và đổi template email sang nội dung mã OTP.

## RabbitMQ contract xác thực email

Khi gọi `POST /api/v1/auth/verify-email`, user-service kiểm tra email tồn tại, tạo token xác thực email, dựng link public và publish event JSON sang RabbitMQ để notification-service gửi email.

- Exchange: `bookstore.events` (`topic`, durable)
- Routing key: `user.email_verification`
- Queue cho notification-service bind: `notification.email_verification_events` (durable)
- Content type: JSON

Payload:

```json
{
  "eventId": "uuid",
  "type": "user.email_verification",
  "occurredAt": "2026-05-27T10:15:30",
  "userId": null,
  "email": "u@example.com",
  "displayName": null,
  "verificationUrl": "https://nhasachcongdong.id.vn/api/v1/auth/confirm-email?token=...",
  "expiresInMinutes": 1440
}
```

Notification-service chỉ gửi email có link/nút `verificationUrl`. Không tự sinh token và không cập nhật trạng thái user. Khi user bấm link, request đi về `GET /api/v1/auth/confirm-email?token=...`; user-service validate token và set `isEmailVerified = true`.

## API chính

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/google/callback?code=...`
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
