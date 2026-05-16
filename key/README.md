# JWT keys (`/key`)

Thư mục chứa cặp RSA cho RS256. **Không commit** `private.pem` (đã gitignore).

| File | Dùng bởi |
|------|----------|
| `private.pem` | user-service — ký JWT |
| `public.pem` | user-service + api-gateway — verify JWT |

## Tạo key mới (OpenSSL)

```bash
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
```

## Chuyển từ Base64 trong `.env` cũ

Nếu còn `APP_JWT_PRIVATE_KEY` / `APP_JWT_PUBLIC_KEY` dạng Base64 DER, tạo PEM:

```powershell
# public (SPKI)
$b64 = "MIIBIjANBg..."
@("-----BEGIN PUBLIC KEY-----") + [regex]::Matches($b64,'.{1,64}').Value + @("-----END PUBLIC KEY-----") | Set-Content public.pem

# private (PKCS#8) — tương tự với BEGIN PRIVATE KEY
```

Copy `public.pem` sang `bookstore-api-gateway/key/` (gateway chỉ cần public).

## Cấu hình

- Local: `APP_JWT_KEYS_DIR=./key` trong `.env`
- Docker: mount `./key:/key:ro`, `APP_JWT_KEYS_DIR=/key`
- K8s: Secret volume mount tại `/key`
