package org.opensbpm.webui.backend;

import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.webui.SslConfiguration;
import org.opensbpm.webui.backend.authentication.TokenHolder;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Properties;

import static java.util.Arrays.asList;

@Service
public class RolesService {
    public Collection<RoleInfo> getRoles() {
        SslConfiguration.trustAll();
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, SslConfiguration.getTrustAllCerts(), new SecureRandom());

        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        RestClient restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(client))
                .baseUrl("https://opensbpm.local")
                .build();
        String body = restClient.get()
                .uri("/api/")
                .header("Authorization", "Bearer "+ TokenHolder.getToken().getTokenValue())
                .retrieve()
                .body(String.class);

        return asList(new RoleInfo(){
            @Override
            public String getRoleName() {
                return body;
            }
        });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static interface RoleInfo{

        String getRoleName();
    }
}
