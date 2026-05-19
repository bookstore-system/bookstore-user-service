# JWT keys

`user-service` dung cap RSA key cho JWT RS256:

| File | Dung boi |
| --- | --- |
| `private.pem` | `user-service` ky JWT |
| `public.pem` | `user-service` va `api-gateway` verify JWT |

Khong commit key that len Git.

## Dev/local

Khi chay local/dev:

```text
APP_JWT_KEYS_DIR=./key
```

Thu muc nay can co:

```text
key/private.pem
key/public.pem
```

## Docker Compose dev

Mount:

```text
./key:/key:ro
APP_JWT_KEYS_DIR=/key
```

## Kubernetes production

Production khong doc key tu repo. Jenkins tao Kubernetes Secret tu Jenkins Credentials.

Can tao Jenkins Credentials:

```text
Credential ID: jwt-private-pem
Credential type: Secret file
Secret file content: private.pem

Credential ID: jwt-public-pem
Credential type: Secret file
Secret file content: public.pem
```

Jenkinsfile tao Kubernetes Secret:

```text
jwt-keys
```

Deployment mount secret vao:

```text
/key/private.pem
/key/public.pem
```

Production env:

```text
APP_JWT_KEYS_DIR=/key
```

`api-gateway` phai dung cung `jwt-public-pem` de verify token do `user-service` ky.

## Tao key moi bang OpenSSL

```bash
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
```
