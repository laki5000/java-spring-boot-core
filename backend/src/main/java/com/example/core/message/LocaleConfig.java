package com.example.core.message;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
@EnableConfigurationProperties(LocaleProperties.class)
public class LocaleConfig {

  @Bean
  public LocaleResolver localeResolver(LocaleProperties localeProperties) {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setSupportedLocales(localeProperties.supportedLocales());
    resolver.setDefaultLocale(localeProperties.defaultLocale());
    return resolver;
  }
}
