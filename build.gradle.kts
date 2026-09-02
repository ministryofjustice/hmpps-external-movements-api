import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.7"
  kotlin("plugin.spring") version "2.4.10"
  kotlin("plugin.jpa") version "2.4.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val ehcacheVersion = "3.12.0"
val hibernateJcacheVersion = "7.4.5.Final"
val hmppsKotlinVersion = "3.0.1"
val sentryVersion = "8.54.0"
val springDocVersion = "3.1.0"
val sqsStarterVersion = "7.4.1"
val testContainersVersion = "1.21.4"
val uuidGeneratorVersion = "5.2.0"
val wiremockVersion = "3.13.2"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:$hmppsKotlinVersion")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:$sqsStarterVersion")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
  implementation("io.sentry:sentry-spring-boot-4:$sentryVersion")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.hibernate.orm:hibernate-envers")
  implementation("org.hibernate.orm:hibernate-jcache")
  implementation("org.springframework.data:spring-data-envers")
  implementation("com.fasterxml.uuid:java-uuid-generator:$uuidGeneratorVersion")

  runtimeOnly("org.springframework.boot:spring-boot-starter-flyway")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.ehcache:ehcache:$ehcacheVersion")

  testImplementation("org.testcontainers:postgresql:$testContainersVersion")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:$hmppsKotlinVersion")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
}

kotlin {
  jvmToolchain(25)
}

dependencyCheck {
  suppressionFiles.add("$rootDir/rem-suppressions.xml")
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JVM_25
      freeCompilerArgs.addAll(
        "-Xannotation-default-target=param-property",
      )
    }
  }
  test {
    if (project.hasProperty("init-db")) {
      include("**/InitialiseDatabase.class")
    } else {
      exclude("**/InitialiseDatabase.class")
    }
  }
}
