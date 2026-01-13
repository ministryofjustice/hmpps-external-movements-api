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
val hmppsKotlinVersion = "2.0.0"
val sentryVersion = "8.29.0"
val springDocVersion = "3.0.1"
val sqsStarterVersion = "6.0.0"
val testContainersVersion = "1.21.4"
val uuidGeneratorVersion = "5.2.0"
val wiremockVersion = "3.13.2"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:$hmppsKotlinVersion")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:$sqsStarterVersion")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:$sentryVersion")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
  implementation("tools.jackson.module:jackson-module-kotlin")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.data:spring-data-envers")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.hibernate.orm:hibernate-envers")
  implementation("org.hibernate:hibernate-jcache:$hibernateJcacheVersion")
  implementation("com.fasterxml.uuid:java-uuid-generator:$uuidGeneratorVersion")

  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.ehcache:ehcache:$ehcacheVersion")

  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.testcontainers:postgresql:$testContainersVersion")
  testImplementation("org.testcontainers:localstack:$testContainersVersion")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:$hmppsKotlinVersion")
  testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
        setFrom(layout.buildDirectory.dir("agent").get().asFile)
        includes.add("agent.jar")
        into = "/agent"
      }
      path {
        setFrom(layout.projectDirectory.asFile)
        includes.add("applicationinsights.json")
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
