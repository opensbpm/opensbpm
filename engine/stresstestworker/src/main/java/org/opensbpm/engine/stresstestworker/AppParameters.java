package org.opensbpm.engine.stresstestworker;

import jakarta.validation.constraints.NotBlank;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "opensbpm")
public class AppParameters {

    @NotBlank
    private String url;

    private String authUrl;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private boolean starter;

    private Statistics statistics;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean hasAuthUrl() {
        return authUrl != null && !authUrl.isEmpty();
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isStarter() {
        return starter;
    }

    public void setStarter(boolean starter) {
        this.starter = starter;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    public EngineServiceClient createEngineServiceClient() {
        return createEngineServiceClient(Credentials.of(username, password.toCharArray()));
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

    public static class Statistics {
        private String url;
        private String username;
        private String password;

        private Integer nodes;
        private Integer pods;
        private Integer processes;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Integer getNodes() {
            return nodes;
        }

        public void setNodes(Integer nodes) {
            this.nodes = nodes;
        }

        public Integer getPods() {
            return pods;
        }

        public void setPods(Integer pods) {
            this.pods = pods;
        }

        public Integer getProcesses() {
            return processes;
        }

        public void setProcesses(Integer processes) {
            this.processes = processes;
        }
    }

}
