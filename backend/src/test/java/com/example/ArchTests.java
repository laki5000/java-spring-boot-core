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
  private static final String PROJECT = "com.example.project..";
  private static final String GENERATED = "com.example.generated..";
  private static final String INTEGRATION_TECHNOLOGY = "com.example.integration.(*)..";

  @ArchTest
  static final ArchRule coreShouldNotDependOnOtherLayers =
      noClasses()
          .that()
          .resideInAPackage(CORE)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(INTEGRATION, PROJECT, GENERATED);

  @ArchTest
  static final ArchRule integrationShouldNotDependOnProject =
      noClasses()
          .that()
          .resideInAPackage(INTEGRATION)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(PROJECT)
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule projectShouldNotDependOnIntegration =
      noClasses()
          .that()
          .resideInAPackage(PROJECT)
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
