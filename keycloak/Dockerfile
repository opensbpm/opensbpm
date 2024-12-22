FROM quay.io/keycloak/keycloak:25.0.4

COPY realm-export.json  /opt/keycloak/data/import/

#ENDPOINT start-dev --import-realm
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "import", "--verbose", "--dir", "/opt/keycloak/data/import/", "--optimized" ]
