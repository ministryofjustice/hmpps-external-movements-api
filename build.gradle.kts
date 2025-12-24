import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.2.0"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val ehcacheVersion = "3.11.1"
val hibernateJcacheVersion = "7.2.0.Final"
val hmppsKotlinVersion = "1.8.2"
val sentryVersion = "8.29.0"
val springDocVersion = "2.8.14"
val sqsStarterVersion = "5.6.3"
val testContainersVersion = "1.21.4"
val uuidGeneratorVersion = "5.2.0"
val wiremockVersion = "3.13.2"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:$hmppsKotlinVersion")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:$sentryVersion")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.hibernate.orm:hibernate-envers")
  implementation("org.hibernate:hibernate-jcache:$hibernateJcacheVersion")
  implementation("org.springframework.data:spring-data-envers")
  implementation("com.fasterxml.uuid:java-uuid-generator:$uuidGeneratorVersion")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:$sqsStarterVersion")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.ehcache:ehcache:$ehcacheVersion")

  testImplementation("org.testcontainers:postgresql:$testContainersVersion")
  testImplementation("org.testcontainers:localstack:$testContainersVersion")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:$hmppsKotlinVersion")
  testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
}

dependencyCheck {
  suppressionFiles.addAll(listOf("suppressions.xml", ".dependency-check-ignore.xml"))
  nvd.datafeedUrl = "file:///opt/vulnz/cache"
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JVM_21
      freeCompilerArgs.addAll(
        "-Xwhen-guards",
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
