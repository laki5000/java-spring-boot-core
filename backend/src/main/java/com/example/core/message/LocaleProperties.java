package com.example.core.message;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.locale")
public record LocaleProperties(
    @NotNull Locale defaultLocale, @NotEmpty List<Locale> supportedLocales) {}
