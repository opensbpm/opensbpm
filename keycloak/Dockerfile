FROM quay.io/keycloak/keycloak:25.0.4

COPY realm-export.json  /opt/keycloak/data/import/realm.json

#ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev", "--import-realm" ]
#ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "import", "--verbose", "--dir", "/opt/keycloak/data/import/", "--optimized" ]
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "import", "--verbose", "--file", "/opt/keycloak/data/import/realm.json", "--optimized" ]
