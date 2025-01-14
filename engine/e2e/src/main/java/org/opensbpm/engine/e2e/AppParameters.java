package org.opensbpm.engine.e2e;

import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppParameters {

    @Value("${url}")
    private String url;

    @Value("${authUrl:}")
    private String authUrl;

    @Value("${job.completion.index:}")
    private Integer jobCompletionIndex;

    @Value("${nodes}")
    private Integer nodeCount;

    @Value("${pods}")
    private Integer podCount;

    @Value("${processes}")
    private Integer processCount;

    public String getUrl() {
        return url;
    }

    public boolean hasAuthUrl() {
        return !getAuthUrl().isEmpty();
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public boolean isIndexed() {
        return jobCompletionIndex != null;
    }

    public Integer getJobCompletionIndex() {
        return jobCompletionIndex;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public Integer getPodCount() {
        return podCount;
    }

    public Integer getProcessCount() {
        return processCount;
    }

    public EngineServiceClient createEngineServiceClient(Credentials credentials) {
        final EngineServiceClient engineServiceClient;
        if (hasAuthUrl()) {
            engineServiceClient = EngineServiceClient.create(getAuthUrl(), getUrl(), credentials);
        } else {
            engineServiceClient = EngineServiceClient.create(getUrl(), credentials);
        }
        return engineServiceClient;
    }

}
