
```
docker run --name keycloak \
-e KEYCLOAK_ADMIN=admin \
-e KEYCLOAK_ADMIN_PASSWORD=admin \
--network=host \
quay.io/keycloak/keycloak:21.0.0 \
start-dev \
--http-port=9000
```

Authenticate:
```
curl -k \
    -d "client_id=opensbpm-ui" \
    -d "username=alice" \
    -d "password=alice" \
    -d "grant_type=password" \
    "https://opensbpm.local/auth/realms/quickstart/protocol/openid-connect/token"
```

#JWT decoder
https://jwt.io/#libraries-io

Startable Processmodels
```
curl -k \
    --header "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlb0hrU3VaX0tzMFk2NERpUGN0bzBtMl9oQUktN3BJa2dwT0Q5eXVBajdFIn0.eyJleHAiOjE3MjY5MDU0ODgsImlhdCI6MTcyNjkwNTE4OCwianRpIjoiMDg4NzhmMTctODcxZS00YThmLWE4YmYtMDg3ZGIyYjg2ODc2IiwiaXNzIjoiaHR0cHM6Ly9vcGVuc2JwbS5sb2NhbC9hdXRoL3JlYWxtcy9xdWlja3N0YXJ0Iiwic3ViIjoiYTAwYzY4ZTItYzg3ZC00ZTg4LTk5NTQtMDlmMzczYmQzOTk1IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoib3BlbnNicG0tdWkiLCJzaWQiOiI3ZjY0ZGQ5OC04MWVjLTRiMDgtODY2My03ZTcwNTI0ZGRmNmUiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHBzOi8vb3BlbnNicG0ubG9jYWwiXSwic2NvcGUiOiJlbWFpbCBwcm9maWxlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJyb2xlcyI6WyJBbmdlc3RlbGx0ZSIsInVzZXIiXSwibmFtZSI6IkFsaWNlIExpZGRlbCIsInByZWZlcnJlZF91c2VybmFtZSI6ImFsaWNlIiwiZ2l2ZW5fbmFtZSI6IkFsaWNlIiwiZmFtaWx5X25hbWUiOiJMaWRkZWwiLCJlbWFpbCI6ImFsaWNlQGtleWNsb2FrLm9yZyJ9.mhj7RObaPWCLQtTbcu_UmLK5rr6r3uinN435THRQDaSKUgrkYYh-2rmUJ9PeU5kf4KbVQLi4QGI42n5a6tfGFIy09P6matsECVpOTMtsrZxwxFrTGU---JYjXOYfZmdl49X7omfdSOv0PTLQSe0CCSWIvPlEETQyM8lOm6DxlScTxR2oEHqs4w5HYq6jpeC7PL3b35sa3wWd3zIwZpU-lnfygaZlpDeddSAPqGcWNUELJeIEgAPz1kKko3bF0QfCgv2RKfLK2U9s0JGFsOkEKGnIvcG9wuTFQiijmc6FcfkvVZ-gge4yMpdpve-N_JLiZT8bENVMzh1oVlwEHlxCgw" \
    "https://opensbpm.local/api/services/engine/models"
```
