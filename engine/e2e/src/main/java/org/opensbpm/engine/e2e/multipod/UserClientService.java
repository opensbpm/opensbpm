package org.opensbpm.engine.e2e.multipod;

import jakarta.annotation.PostConstruct;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.e2e.AppParameters;
import org.opensbpm.engine.stresstest.Statistics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;
import org.opensbpm.engine.stresstest.UserClient;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ConditionalOnBean(MultiPodWorkflowExecution.class)
@Component
public class UserClientService implements AutoCloseable{
    private static final Logger LOGGER = Logger.getLogger(UserClientService.class.getName());

    private final ApplicationContext applicationContext;
    private final AppParameters appParameters;
    private final JmsTemplate jmsTemplate;

    private UserClient userClient;

    public UserClientService(ApplicationContext applicationContext, AppParameters appParameters, JmsTemplate jmsTemplate) {
        this.applicationContext = applicationContext;
        this.appParameters = appParameters;
        this.jmsTemplate = jmsTemplate;
    }

    @PostConstruct
    public void init() {
        Credentials credentials = applicationContext.getBean("credentials_" + (appParameters.getJobCompletionIndex() - 1), Credentials.class);
        userClient = new UserClient(appParameters.createEngineServiceClient(credentials));
        LOGGER.log(Level.INFO, "Running as User " + credentials.getUserName());
        userClient.startTaskFetcher();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(userClient.getActiveProcesses().isEmpty()){
                    jmsTemplate.send("user", new MessageCreator() {
                        public Message createMessage(Session session) throws JMSException {
                            //return session.createTextMessage(credentials.getUserName()+ "finished");
                            ArrayList<Statistics> statistics = userClient.getStatistics()
                                    .collect(Collectors.toCollection(ArrayList::new));
                            return session.createObjectMessage(statistics);
                        }
                    });
                    cancel();
                }
            }
        }, 1000, 1000);
    }

    @JmsListener(destination = "models")
    public void startProcesses(String content) {
        userClient.startProcesses(appParameters.getProcessCount());
    }

    @Override
    public void close() throws Exception {
        userClient.stopTaskFetcher();
    }
}
