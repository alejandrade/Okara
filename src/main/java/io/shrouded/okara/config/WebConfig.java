package io.shrouded.okara.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Controller handles routing instead
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static assets (JS, CSS, images) from classpath
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/static/");
        registry.addResourceHandler("/*.ico", "/*.png", "/*.json", "/*.txt")
                .addResourceLocations("classpath:/static/");
    }
}