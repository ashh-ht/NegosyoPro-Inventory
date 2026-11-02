package com.inventory.negosyopro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**") // guard every URL...
                .excludePathPatterns( // ...except these
                        "/login",
                        "/setup",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error");
    }
}   