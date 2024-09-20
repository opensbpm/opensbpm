package org.opensbpm.engine.service;

import org.opensbpm.engine.core.EngineConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(EngineConfig.class)
public class EngineServiceApplication {

    public static void main(String[] args) {
        SslConfiguration.trustAll();
        SpringApplication.run(EngineServiceApplication.class, args);
    }

}
