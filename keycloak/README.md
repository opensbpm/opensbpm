
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
