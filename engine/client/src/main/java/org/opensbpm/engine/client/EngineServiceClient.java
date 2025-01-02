package org.opensbpm.engine.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.opensbpm.engine.server.api.EngineResource;
import org.opensbpm.engine.server.api.ProcessInstanceResource;
import org.opensbpm.engine.server.api.ProcessModelResource;
import org.opensbpm.engine.server.api.UserResource;

import javax.net.ssl.SSLContext;

public abstract class EngineServiceClient {


    public static EngineServiceClient create(String baseAddress, Credentials credentials) {
        return create(baseAddress, baseAddress, credentials);
    }

    public static EngineServiceClient create(String authAddress, String serviceAddress, Credentials credentials) {
        requireAddress(authAddress);
        requireAddress(serviceAddress);
        Objects.requireNonNull(credentials, "Credentials must not be null");
        return new EngineServiceClient(serviceAddress){
            @Override
            protected Authentication authenticate() {
                try {
                    return doAuthenticate();
                } catch (IOException | GeneralSecurityException | InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                    }

                    private Authentication doAuthenticate() throws IOException, GeneralSecurityException, InterruptedException {
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, SslConfiguration.getTrustAllCerts(), new SecureRandom());

                        HttpClient httpClient = HttpClient.newBuilder()
                                .sslContext(sslContext)
                                .build();
                        HttpRequest authRequest = HttpRequest.newBuilder(URI.create(String.format("%s/auth/realms/quickstart/protocol/openid-connect/token", authAddress)))
                                .header("content-type", "application/x-www-form-urlencoded")
                                .POST(BodyPublishers.ofString(
                                        String.format("client_id=opensbpm-ui&username=%s&password=%s&grant_type=password", credentials.getUserName(), String.valueOf(credentials.getPassword()))
                                ))
                                .build();
                        HttpResponse<String> authResponse = httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString());
                        if(200 == authResponse.statusCode()) {
                            return new ObjectMapper().readValue(authResponse.body(), Authentication.class);
                        }else{
                            throw new IOException(authResponse.body());
                        }
                    }

                };
    }

    private static String requireAddress(String authAddress) {
        Objects.requireNonNull(authAddress, "Address must not be null");
        if (authAddress.endsWith("/")) {
            throw new IllegalArgumentException(authAddress + " must not end with /");
        }
        return authAddress;
    }

    private final static Logger LOGGER = Logger.getLogger(EngineServiceClient.class.getName());
    //
    private final String baseAddress;
    //
    private final Object lock = new Object();
    private Authentication authentication;
    //
    private final ThreadLocal<UserResource> userResource = of(() -> createResourceClient(UserResource.class));
    private final ThreadLocal<ProcessModelResource> processModelResource = of(() -> createResourceClient(ProcessModelResource.class));
    private final ThreadLocal<EngineResource> engineResource = of(() -> newEngineResource());
    private final ThreadLocal<ProcessInstanceResource> instanceResource = of(() -> createResourceClient(ProcessInstanceResource.class));

    public EngineServiceClient(String baseAddress) {
        this.baseAddress = Objects.requireNonNull(baseAddress, "baseAddress must not be null");
    }


    private Authentication getAuthentication() {
        if (authentication == null) {
            synchronized(lock) {
                LOGGER.log(Level.FINER, "request authentication");
                authentication = authenticate();
            }
        }
        return authentication;
    }

    protected abstract Authentication authenticate();

    public UserResource getUserResource() {
        return userResource.get();
    }

    public ProcessModelResource getProcessModelResource() {
        return processModelResource.get();
    }

    public EngineResource newEngineResource() {
        return createResourceClient(EngineResource.class);
    }

    public EngineResource getEngineResource() {
        return engineResource.get();
    }

    public ProcessInstanceResource getProcessInstanceResource() {
        return instanceResource.get();
    }

    private <T> T createResourceClient(Class<T> type) {
        List<Object> providers = Arrays.asList(
                /*new JacksonJaxbXMLProvider(),*/
                new JacksonJsonProvider(new ObjectMapper()
                        .registerModule(new JavaTimeModule()))
        );
        JAXRSClientFactoryBean factoryBean = new JAXRSClientFactoryBean();
        factoryBean.setAddress(String.format("%s/api/services", baseAddress));
        factoryBean.setProviders(providers);
        factoryBean.setInheritHeaders(true);
        factoryBean.setServiceClass(type);
        final T proxy = factoryBean.create(type);

        Client client = WebClient.client(proxy);
        client.header("Authorization", "Bearer " + getAuthentication().getAccessToken());

        WebClient httpClient = WebClient.fromClient(client);
        HTTPConduit httpConduit = (HTTPConduit) WebClient.getConfig(httpClient).getConduit();
        try {
            TLSClientParameters tlsClientParameters = ObjectUtils.firstNonNull(httpConduit.getTlsClientParameters(), new TLSClientParameters());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, SslConfiguration.getTrustAllCerts(), new SecureRandom());
            tlsClientParameters.setSslContext(sslContext);
            tlsClientParameters.setSSLSocketFactory(sslContext.getSocketFactory());
            tlsClientParameters.setHostnameVerifier(SslConfiguration.getAllHostnameVerifier());

            httpConduit.setTlsClientParameters(tlsClientParameters);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return proxy;
    }

    public void refreshToken() {
        LOGGER.log(Level.INFO, "Refreshing token");
        synchronized(lock) {
            authentication = null;
        }
    }

    private static <T> ThreadLocal<T> of(Supplier<T> resourceSupplier) {
        return new ThreadLocal<T>() {
            @Override
            protected T initialValue() {
                return resourceSupplier.get();

            }
        };
    }


    public interface AuthTokenSupplier extends Supplier<String> {

    }
}
