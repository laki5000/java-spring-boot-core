package com.example.core.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class MessagesUnitTests {

  @Test
  void messageFiles_shouldContainSameKeys_inEveryLanguage() throws IOException {
    // Given
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Properties defaultMessages =
        load(resolver.getResource("classpath:messages/messages.properties"));
    Resource[] messageFiles = resolver.getResources("classpath*:messages/messages*.properties");

    // When / Then
    assertThat(messageFiles).isNotEmpty();

    SoftAssertions softly = new SoftAssertions();

    for (Resource messageFile : messageFiles) {
      softly
          .assertThat(load(messageFile).stringPropertyNames())
          .as(messageFile.getFilename())
          .containsExactlyInAnyOrderElementsOf(defaultMessages.stringPropertyNames());
    }

    softly.assertAll();
  }

  private Properties load(Resource resource) throws IOException {
    Properties properties = new Properties();

    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }

    return properties;
  }
}
