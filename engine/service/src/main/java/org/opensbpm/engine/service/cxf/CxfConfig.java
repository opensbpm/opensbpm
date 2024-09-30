/** *****************************************************************************
 * Copyright (C) 2024 Stefan Sedelmaier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************
 */
package org.opensbpm.engine.service.cxf;

import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.ws.rs.Path;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class CxfConfig {

    @Value("#{servletContext.contextPath}")
    private String servletContextPath;

    @Bean(destroyMethod = "shutdown")
    public SpringBus cxf() {
        return new SpringBus();
    }

    @Bean(destroyMethod = "destroy")
    public Server jaxRsServer(ApplicationContext appCtx, SpringBus springBus) {
        final JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();

        factory.setProviders(Arrays.asList(
                //new SearchParameterConverter(),
                new JAXBElementProvider<>(),
                new JacksonXmlBindJsonProvider()
        ));

        factory.setFeatures(List.of(openApiFeature()));

//        final HashMap<Object, Object> extensionMappings = new HashMap<>();
//        extensionMappings.put("xml", MediaType.APPLICATION_XML);
//        extensionMappings.put("json", MediaType.APPLICATION_JSON);
//        factory.setExtensionMappings(extensionMappings);
        factory.setServiceBeans(appCtx.getBeansWithAnnotation(Path.class).values().stream().toList());

        factory.setBus(springBus);
        return factory.create();
    }

    private OpenApiFeature openApiFeature() {
        final OpenApiFeature openApiFeature = new OpenApiFeature();
        openApiFeature.setTitle("OpenSBPM Engine");
        openApiFeature.setVersion("1.0");
        openApiFeature.setContactName("stefan@sedelmaier.at");
        openApiFeature.setUseContextBasedConfig(true);
        openApiFeature.setScan(false);

        openApiFeature.setIgnoredRoutes(asList("/api-docs"));
        
        OpenApiCustomizer openApiCustomizer = new OpenApiCustomizer();
        openApiCustomizer.setDynamicBasePath(true);        
        openApiFeature.setCustomizer(openApiCustomizer);

        Map<String, SecurityScheme> schemes = new HashMap<>();
        schemes.put("Bearer Authentication", new SecurityScheme()
                .name("BearerAuthentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
        );
        openApiFeature.setSecurityDefinitions(schemes);

        SwaggerUiConfig swaggerUiConfig = new SwaggerUiConfig();
        swaggerUiConfig.setUrl(servletContextPath + "/services/openapi.yaml");
        swaggerUiConfig.setQueryConfigEnabled(false);
        openApiFeature.setSwaggerUiConfig(swaggerUiConfig);
        return openApiFeature;
    }
}
