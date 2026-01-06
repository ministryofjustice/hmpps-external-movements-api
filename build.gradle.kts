import com.google.cloud.tools.jib.gradle.BuildImageTask
import de.undercouch.gradle.tasks.download.Download
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.0"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  id("com.google.cloud.tools.jib") version "3.5.2"
  id("de.undercouch.download") version "5.6.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val ehcacheVersion = "3.11.1"
val hibernateJcacheVersion = "7.2.0.Final"
val hmppsKotlinVersion = "1.8.2"
val sentryVersion = "8.29.0"
val springDocVersion = "2.8.15"
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
  jvmToolchain(25)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JVM_25
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

  val downloadDbCerts by registering(Download::class) {
    src("https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem")
    dest(layout.buildDirectory.file("root.crt"))
  }

  val copyAgentJar by registering(Copy::class) {
    from(layout.buildDirectory.dir("libs"))
    include("applicationinsights-agent*.jar")
    into(layout.buildDirectory.dir("agent"))
    rename("applicationinsights-agent(.+).jar", "agent.jar")
    dependsOn("assemble")
  }

  val jibBuildTar by getting {
    dependsOn.addAll(listOf(copyAgentJar, downloadDbCerts))
  }

  val jibDockerBuild by getting {
    dependsOn.addAll(listOf(copyAgentJar, downloadDbCerts))
  }

  withType<BuildImageTask>().named("jib") {
    doFirst {
      jib!!.to {
        tags = setOf(System.getenv("VERSION") ?: "dev")
        auth {
          username = System.getenv("GITHUB_USERNAME")
          password = System.getenv("GITHUB_PASSWORD")
        }
      }
    }
    dependsOn.addAll(listOf(copyAgentJar, downloadDbCerts))
  }
}

jib {
  container {
    creationTime.set("USE_CURRENT_TIMESTAMP")
    jvmFlags = mutableListOf("-Duser.timezone=Europe/London")
    mainClass = "uk.gov.justice.digital.hmpps.externalmovementsapi.ExternalMovementsApiKt"
    user = "2000:2000"
  }
  from {
    image = "eclipse-temurin:25-jre-jammy"
    platforms {
      platform {
        architecture = "amd64"
        os = "linux"
      }
      platform {
        architecture = "arm64"
        os = "linux"
      }
    }
  }
  to {
    image = "ghcr.io/ministryofjustice/hmpps-external-movements-api"
  }
  extraDirectories {
    paths {
      path {
        setFrom(layout.buildDirectory)
        includes.add("agent/agent.jar")
      }
      path {
        setFrom(layout.projectDirectory)
        includes.add("applicationinsights*.json")
        into = "/agent"
      }
      path {
        setFrom(layout.buildDirectory)
        includes.add("root.crt")
        into = "/.postgresql"
      }
    }
  }
}
