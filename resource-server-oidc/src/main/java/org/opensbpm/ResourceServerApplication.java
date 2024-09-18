package org.opensbpm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ResourceServerApplication {

    public static void main(String[] args) {
        SslConfiguration.trustAll();
        SpringApplication.run(ResourceServerApplication.class, args);
    }

}
