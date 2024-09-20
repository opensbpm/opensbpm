package org.opensbpm.webui.backend.authentication;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

@Component
public class TokenHolder {

    private static OAuth2AccessToken token;

    public static OAuth2AccessToken getToken(){
        return token;
    }

    public static void setToken(OAuth2AccessToken jwtToken){
         token = jwtToken;
    }
}
