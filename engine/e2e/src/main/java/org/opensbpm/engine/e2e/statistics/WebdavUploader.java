package org.opensbpm.engine.e2e.statistics;

import org.opensbpm.engine.e2e.AppParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class WebdavUploader {

    @Value("${e2e.statistics.url}")
    private String url;

    @Value("${e2e.statistics.username}")
    private String username;

    @Value("${e2e.statistics.password}")
    private String password;

    public void uploadStatistic(AppParameters appParameters, String statData) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String fileName = String.format("statistics-%s-%s.csv", appParameters.getProcessCount(), LocalDateTime.now().format(formatter));

        RestTemplate restTemplate = new RestTemplateBuilder()
                .basicAuthentication(username, password)
                .build();
        restTemplate.put(String.format("%s/%s", url,fileName), statData);
    }
}
