package com.attendance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://attandance-tracker-front-78pjqwga5-sandeep-pauls-projects.vercel.app/")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
