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
                        "/", // PublicController decides: dashboard or login
                        "/login",
                        "/signin", // old wireframe URLs, they only redirect
                        "/create-business/**",
                        "/setup", // sign-up + setup wizard (GET shows it, POST saves it)
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                        "/error",
                        "/error/**");
    }
}   