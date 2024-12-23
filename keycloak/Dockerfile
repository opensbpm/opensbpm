FROM quay.io/keycloak/keycloak:25.0.4

COPY realm.json  /opt/keycloak/data/import/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "import", "--verbose", "--file", "/opt/keycloak/data/import/realm.json"]
