package org.opensbpm.engine.service.services;

import org.junit.jupiter.api.Test;
import org.opensbpm.engine.api.ModelService;
import org.opensbpm.engine.api.ModelService.ModelRequest;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.api.model.builder.DefinitionFactory;
import org.opensbpm.engine.api.model.definition.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.Objects;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ProcessModelResourceServiceIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private ModelService modelService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    public void testRetrieve() throws Exception {

        //arrange
        long modelId = 1L;
        when(modelService.retrieveDefinition(any(ModelRequest.class))).thenReturn(
                DefinitionFactory.process("Test")
                        .build()
        );

        Jwt jwt = Jwt.withTokenValue("1234")
                .header("Authorization", "Bearer 1234")
                .claim("sub", "1234")
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        //act
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + jwt.getTokenValue());
        ResponseEntity<ProcessModelInfo> response = restTemplate.exchange("/services/engine/models/{modelId}", HttpMethod.GET, new HttpEntity<String>(headers), ProcessModelInfo.class, 1);
        ProcessModelInfo modelInfo = response.getBody();

        //assert
        assertThat(modelInfo, is(notNullValue()));
        verify(modelService, atLeastOnce()).retrieveDefinition(argThat(Objects::nonNull));
    }
}
