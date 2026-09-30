package com.openomada.plugin.tr069.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Maps static assets for the TR-069 React Admin interface (/tr069-admin/**)
 * directly from classpath:/static/tr069-admin/.
 */
@Configuration("tr069WebMvcConfig")
public class Tr069WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/tr069-admin/**")
                .addResourceLocations("classpath:/static/tr069-admin/");
    }
}
