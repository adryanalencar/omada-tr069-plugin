package com.openomada.plugin.tr069.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor that:
 * 1. Sets spring.main.allow-circular-references=true
 * 2. Prepends /tr069/** and /api/v2/tr069/** as anonymous to shiro.filter.chains
 *    so Shiro's RejectFilter and LoginFilter allow TR-069 CPE and API traffic through.
 */
public class Tr069EnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.main.allow-circular-references", true);

        String existingChains = environment.getProperty("shiro.filter.chains");
        String tr069Chains =
                "/tr069 = anon\n" +
                "/tr069/ = anon\n" +
                "/tr069/** = anon\n" +
                "/**/tr069/** = anon\n" +
                "/tr069-admin = anon\n" +
                "/tr069-admin/ = anon\n" +
                "/tr069-admin/** = anon\n" +
                "/**/tr069-admin/** = anon\n" +
                "/api/v2/tr069/** = anon\n" +
                "/**/api/v2/tr069/** = anon\n" +
                (existingChains != null ? existingChains : "");
        props.put("shiro.filter.chains", tr069Chains);

        environment.getPropertySources().addFirst(new MapPropertySource("tr069EnvironmentProperties", props));
    }
}
