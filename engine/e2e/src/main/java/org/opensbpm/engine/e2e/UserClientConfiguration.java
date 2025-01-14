package org.opensbpm.engine.e2e;

import org.opensbpm.engine.client.Credentials;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserClientConfiguration implements BeanDefinitionRegistryPostProcessor {
    public static final String CREDENTIALS_BEAN_NAME = "credentials_";
    private final Environment environment;

    public UserClientConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Binder binder = Binder.get(environment);
        List<HashMap> properties = binder.bind("e2e.users", Bindable.listOf(HashMap.class)).get();

        for (int i = 0; i < properties.size(); i++) {
            Map credentials = properties.get(i);
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(Credentials.class, () ->
                            Credentials.of(
                                    String.valueOf(credentials.get("username")),
                                    String.valueOf(credentials.get("password")).toCharArray()
                            )
                    );
            //builder.addPropertyValue("username", credentials.getUserName());
            registry.registerBeanDefinition(CREDENTIALS_BEAN_NAME + i, builder.getBeanDefinition());
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
