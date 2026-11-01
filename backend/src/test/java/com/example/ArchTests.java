package com.example;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchTests {

  private static final String CORE = "com.example.core..";
  private static final String INTEGRATION = "com.example.integration..";
  private static final String PROJ = "com.example.proj..";
  private static final String GENERATED = "com.example.generated..";
  private static final String SPRING_WEB = "org.springframework.web..";
  private static final String SPRING_HTTP = "org.springframework.http..";
  private static final String SPRING_BOOT_WEB = "org.springframework.boot.web..";
  private static final String SPRING_BOOT_WEBMVC = "org.springframework.boot.webmvc..";
  private static final String SERVLET = "jakarta.servlet..";
  private static final String INTEGRATION_TECHNOLOGY = "com.example.integration.(*)..";

  @ArchTest
  static final ArchRule coreShouldNotDependOnOtherLayersOrWeb =
      noClasses()
          .that()
          .resideInAPackage(CORE)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              INTEGRATION,
              PROJ,
              GENERATED,
              SPRING_WEB,
              SPRING_HTTP,
              SPRING_BOOT_WEB,
              SPRING_BOOT_WEBMVC,
              SERVLET);

  @ArchTest
  static final ArchRule integrationShouldNotDependOnProj =
      noClasses()
          .that()
          .resideInAPackage(INTEGRATION)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(PROJ)
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule projShouldNotDependOnIntegration =
      noClasses()
          .that()
          .resideInAPackage(PROJ)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(INTEGRATION);

  @ArchTest
  static final ArchRule integrationTechnologiesShouldNotDependOnEachOther =
      slices()
          .matching(INTEGRATION_TECHNOLOGY)
          .should()
          .notDependOnEachOther()
          .allowEmptyShould(true);
}
