import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import pl.allegro.tech.build.axion.release.domain.PredefinedVersionCreator

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath(libs.kotlin.serialization.gradle.plugin) {
      version {
        // Force the version of the compiler plugin, or the Kotlin BOM
        // upgrades it to an incompatible version.
        require(libs.versions.kotlin.compiler.get())
      }
    }
  }
}
plugins {
  `java-gradle-plugin`
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.tapmoc)
  alias(libs.plugins.pluginPublish)
  alias(libs.plugins.axionRelease)
  alias(libs.plugins.detekt)
}

tapmoc {
  gradle("8.0.0")
}

kotlin {
  @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
  compilerVersion.set(libs.versions.kotlin.compiler.get())
}

scmVersion {
  tag {
    prefix = "v"
  }
  nextVersion {
    suffix = "SNAPSHOT"
    separator = "-"
  }
  versionCreator = PredefinedVersionCreator.SIMPLE.versionCreator
}

group = "tel.schich.tinyjib"
version = scmVersion.version

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.serialization.json)
  implementation(libs.jibCore)
  implementation(libs.guava)
  compileOnly(libs.gradle.api)

  testImplementation(gradleTestKit())
  testImplementation(libs.junit.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.platform.launcher)
}

val pluginId = "tel.schich.tinyjib"

gradlePlugin {
  displayName
  website = "https://github.com/pschichtel/tiny-jib"
  vcsUrl = "https://github.com/pschichtel/tiny-jib"
  plugins {
    create("tinyJibPlugin") {
      id = pluginId
      implementationClass = "tel.schich.tinyjib.TinyJibPlugin"
      displayName = "Tiny Jib Gradle Plugin"
      description = "A heavily simplified version of Google's Jib plugin"
      tags = listOf("container", "jib")
    }
  }
}

val testRepoName = "testRepo"
val testRepoDir = project.layout.buildDirectory.dir("test-repo")

publishing {
  repositories {
    maven {
      name = testRepoName
      url = testRepoDir.get().asFile.toURI()
    }
  }
}

tasks.test {
  testLogging {
    showStandardStreams = true
  }

  val publishTasks = tasks
    .withType(PublishToMavenRepository::class)
    .matching { it.repository.name == testRepoName }

  dependsOn(publishTasks)

  useJUnitPlatform()
  systemProperty("tinyjib.id", pluginId)
  systemProperty("tinyjib.version", project.version)
  systemProperty("tinyjib.rootDir", project.rootDir.absolutePath)
  systemProperty("tinyjib.repoUri", testRepoDir.get().asFile.toURI().toString())
  systemProperty("tinyjib.gradleVersion", gradle.gradleVersion)

  for (version in listOf(8, 11, 17, 21, 25)) {
    val javaHome = javaToolchains.launcherFor {
      languageVersion.set(JavaLanguageVersion.of(version))
    }.get().metadata.installationPath.asFile.absolutePath
    systemProperty("tinyjib.javaHome.$version", javaHome)
  }
}

tasks.jar {
  manifest {
    attributes("Implementation-Version" to version.toString())
  }
}

tasks.check {
  dependsOn(tasks.detektMain, tasks.detektTest)
}

detekt {
  parallel = true
  buildUponDefaultConfig = true
  config.setFrom(files(project.rootDir.resolve("detekt.yml")))
}
