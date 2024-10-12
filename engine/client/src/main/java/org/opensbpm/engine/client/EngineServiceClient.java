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

import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.opensbpm.engine.server.api.EngineResource;
import org.opensbpm.engine.server.api.ProcessModelResource;
import org.opensbpm.engine.server.api.UserResource;

import javax.net.ssl.SSLContext;

public final class EngineServiceClient {


    public static EngineServiceClient create(String baseAddress, Credentials credentials) {
        Objects.requireNonNull(baseAddress, "baseAddress must not be null");
        Objects.requireNonNull(credentials, "Credentials must not be null");
        return new EngineServiceClient(baseAddress,
                new AuthTokenSupplier() {
                    private Authentication authentication;

                    @Override
                    public String get() {
                        return getAuthentication().getAccessToken();
                    }

                    private Authentication getAuthentication() {
                        if (authentication == null) {
                            try {
                                authentication = authenticate();
                            } catch (IOException | GeneralSecurityException | InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                        return authentication;
                    }

                    private Authentication authenticate() throws IOException, GeneralSecurityException, InterruptedException {
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, SslConfiguration.getTrustAllCerts(), new SecureRandom());

                        HttpClient httpClient = HttpClient.newBuilder()
                                .sslContext(sslContext)
                                .build();
                        HttpRequest authRequest = HttpRequest.newBuilder(URI.create("https://cloud.opensbpm.org/auth/realms/quickstart/protocol/openid-connect/token"))
                                .header("content-type", "application/x-www-form-urlencoded")
                                .POST(BodyPublishers.ofString(
                                        String.format("client_id=opensbpm-ui&username=%s&password=%s&grant_type=password", credentials.getUserName(), String.valueOf(credentials.getPassword()))
                                ))
                                .build();
                        HttpResponse<String> authResponse = httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString());
                        return new ObjectMapper().readValue(authResponse.body(), Authentication.class);
                    }

                }
        );
    }

    private final String baseAddress;
    private final AuthTokenSupplier authTokenSupplier;
    //
    private final LazySupplier<UserResource> userResource = LazySupplier.of(() -> createResourceClient(UserResource.class));
    private final LazySupplier<ProcessModelResource> processModelResource = LazySupplier.of(() -> createResourceClient(ProcessModelResource.class));
    private final LazySupplier<EngineResource> engineResource = LazySupplier.of(() -> createResourceClient(EngineResource.class));


    public EngineServiceClient(String baseAddress, AuthTokenSupplier authTokenSupplier) {
        this.baseAddress = Objects.requireNonNull(baseAddress, "baseAddress must not be null");
        this.authTokenSupplier = Objects.requireNonNull(authTokenSupplier, "AuthTokenSupplier must not be null");
    }

    public UserResource getUserResource() {
        return userResource.get();
    }

    public ProcessModelResource getProcessModelResource() {
        return processModelResource.get();
    }

    public EngineResource getEngineResource() {
        return engineResource.get();
    }

    private <T> T createResourceClient(Class<T> type) {
        List<Object> providers = Arrays.asList(
                /*new JacksonJaxbXMLProvider(),*/
                new JacksonJsonProvider(new ObjectMapper()
                        .registerModule(new JavaTimeModule()))
        );
        JAXRSClientFactoryBean factoryBean = new JAXRSClientFactoryBean();
        factoryBean.setAddress(String.format("%s", baseAddress));
        factoryBean.setProviders(providers);
        factoryBean.setInheritHeaders(true);
        factoryBean.setServiceClass(type);
        final T proxy = factoryBean.create(type);

        Client client = WebClient.client(proxy);
        client.header("Authorization", "Bearer " + authTokenSupplier.get());

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

    private static class LazySupplier<T> {

        public static <T> LazySupplier<T> of(Supplier<T> supplier) {
            return new LazySupplier<>(supplier);
        }

        private final Supplier<T> supplier;

        private T value;

        private LazySupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public T get() {
            if (value == null) {
                value = supplier.get();
            }
            return value;
        }

    }

    public interface AuthTokenSupplier extends Supplier<String> {

    }
}
