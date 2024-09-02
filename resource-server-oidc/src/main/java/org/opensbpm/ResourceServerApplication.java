package org.opensbpm;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

@SpringBootApplication
public class ResourceServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceServerApplication.class, args);
    }

    @Component
    public static class AppStartupRunner implements ApplicationRunner {

        @Override
        public void run(ApplicationArguments args) throws Exception {
            if(true)
                return;
            try {
                //URI uri = URI.create("http://localhost:9000/realms/quickstart/protocol/openid-connect/certs");
                //URI uri = URI.create("http://www.orf.at");
                //URI uri = URI.create("http://localhost:9000/realms/quickstart");
                URI uri = URI.create("http://localhost:9000/admin");
                URLConnection urlConnection = uri.toURL().openConnection();
                urlConnection.connect();
                ;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
