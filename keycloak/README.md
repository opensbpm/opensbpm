
````
docker run --name keycloak \
-e KEYCLOAK_ADMIN=admin \
-e KEYCLOAK_ADMIN_PASSWORD=admin \
--network=host \
quay.io/keycloak/keycloak:21.0.0 \
start-dev \
--http-port=9000
```
