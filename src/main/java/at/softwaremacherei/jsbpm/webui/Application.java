package at.softwaremacherei.jsbpm.webui;

import at.softwaremacherei.elasticsearch.ElasticsearchConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

import at.softwaremacherei.jsbpm.elasticsearch.ElasticSearchConfig;
import at.softwaremacherei.jsbpm.engine.core.EngineConfig;
import at.softwaremacherei.jsbpm.jasperreports.JasperReportsConfig;
import at.softwaremacherei.jsbpm.server.rbac.RbacConfig;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@Import({EngineConfig.class,/*CrmJpaConfig.class,*/ RbacConfig.class,
    JasperReportsConfig.class, ElasticSearchConfig.class, ElasticsearchConfig.class /*, DynamodbConfig.class*/})
public class Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
