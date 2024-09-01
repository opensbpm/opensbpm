import Keycloak from "keycloak-js";

//see https://www.keycloak.org/docs/latest/securing_apps/index.html#_javascript_adapter

const keycloak = new Keycloak({
    url: 'http://localhost:9000',
    realm: 'quickstart',
    clientId: 'authz-servlet',
});

export default keycloak;
